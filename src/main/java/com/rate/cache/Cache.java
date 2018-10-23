package com.rate.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rate.context.ICallContext;
import com.rate.context.ThreadLocalContext;

public class Cache
{
  private CacheTimeManager timeManager;
  private Map cacheMap;
  private Integer stackId;
  
  public Cache(Integer stackId)
  {
    cacheMap = Collections.synchronizedMap(new HashMap());
    timeManager = new CacheTimeManager(this);
    this.stackId = stackId;
  }
  
  public Map getCacheMap()
  {
    return cacheMap;
  }

  protected void clear()
  {
    System.out.println("Clearing Cache..");
    cacheMap.clear();
    timeManager.flushAll();
  }

  protected CacheTimeManager getTimeManager()
  {
    return timeManager;
  }

  /**
   * Puts the object into the cacheMap with the specified key. Does nothing if (object == null) or if
   * !object.isCacheable(). Otherwise, it ensures that the cacheMap contains the object, accessed by the object's
   * primary key. If (key != null), then it also ensures that the cacheMap contains the object, accessed by that key.
   * <P>
   * If the cacheMap already contains an object with the same primary key as the specified object, then that object is
   * kept and the new object is not added.
   * </P>
   * <P>
   * The entry in the cacheMap is updated with last-access time of now, whether or not new object/keys are added.
   * 
   * @param key
   *          The key to access this object by.
   * @param object
   *          The object to add to the cacheMap.
   */
  public void put(CacheKey key, CacheableObject object)
  {
    if ((object != null) && (object.isCacheable()))
    {
      if ((key != null) && !(key.getObjectClass().isInstance(object)))
      {
        throw new ClassCastException("Cannot store object of type '" + object.getClass().getName()
            + "' using key expecting type '" + key.getObjectClass().getName() + "'.");
      }
      CacheKey primary = object.getPrimaryKey();

      if ((key != null && key.getScope() == CacheTimeManager.CALL)  
          || object.getCacheLongevity() == CacheTimeManager.CALL)
      {
        ICallContext context = ThreadLocalContext.get();
        if ( context != null )
        {
          if (primary != null) context.setContextAttribute(primary.toString(), object);
          if (object.getSecondaryKey() != null) context.setContextAttribute(object.getSecondaryKey().toString(), object);
          if (key != null) context.setContextAttribute(key.toString(), object);
        }
          
        return;
      }

      CachePointer pointer = (CachePointer) cacheMap.get(primary.toString());
      if (pointer != null)
      {
        pointer.setObject(object);
      }
      else
      {
        TimeList list = timeManager.getList(object);
        pointer = new CachePointer(object, list);
        pointer.addKey(primary);
        
        cacheMap.put(primary.toString(), pointer);
      }
  
      CacheKey secondary = object.getSecondaryKey();
      if (secondary != null)
      {
        pointer.addKey(secondary);
        
        cacheMap.put(secondary.toString(), pointer);
      }
      // Cache by supplied key
      if ((key != null) && (!cacheMap.containsKey(key.toString())))
      {
        pointer.addKey(key);
        
        cacheMap.put(key.toString(), pointer);
      }
    }
  }

  /**
   * Looks up the object in the cacheMap and returns it if found. If found, the last-accessed time for the object is
   * updated.
   */
  public CacheableObject get(CacheKey key) throws Exception
  {
    if (key == null)
    {
      return null;
    }

    CacheableObject obj = null;
    if (key.getScope() == CacheTimeManager.CALL)
    {
      ICallContext callContext = ThreadLocalContext.get();
      if ( callContext != null )
      {
        obj = (CacheableObject)callContext.getContextAttribute(key.toString());
        if ( obj != null )
          return obj;
      }
    }
    
    CachePointer item = (CachePointer) cacheMap.get(key.toString());
  
    if ((item == null) || (item.getObject() == null))
    {
      // if not found try to generate it then add it to cacheMap
  
      CacheableObject.ObjectGenerator gen = key.getGenerator();
  
      if (gen != null)
      {
        obj = gen.regenerateObject(key.getObjectClass(), key.getCriteria());
  
        if (obj != null)
        {
          // In case we are generating a list, set the scope define in the key
          obj.setCacheLongevity(key.getScope());
          put(key, obj);
  
          return obj;
        }
      }
  
      return null;
    }
    else
    {      
      return item.getObject();
    }
  }

  /**
   * Regenerates the object from the database and updates the cacheMap
   */
  public void regenerate(CacheKey key)
  {
    if (key == null)
    {
      return;
    }

    // get original object
    CachePointer item = (CachePointer) cacheMap.get(key.toString());
    CacheableObject object = item.getObject();
  
    // flush it
    flush(key, true);
  
    // regenerate it
    try
    {
      CacheableObject newObject = get(key);
  
      // Call object regenerate so it can transfer any transient data
      if (newObject != null)
      {
        newObject.regenerate(object);
      }
    }
    catch (Exception e)
    {
    }
  }

  public boolean containsKey(CacheKey key)
  {
    if (key == null)
    {
  	  return false;
  	}
  	
    return cacheMap.containsKey(key.toString());
  }

  /**
   * Flushes the object corresponding to the indicated key from the cacheMap. If recursive is set, it calls
   * object.flush() to process recursively. Does nothing if no object corresponding to the key is found.
   * 
   * @param key
   *          The key to flush from the cacheMap.
   * @param recursive
   *          Whether or not to flush recursively.
   */
  public void flush(CacheKey key, boolean recursive)
  {
    if (key == null)
    {
      return;
    }

    
    if (key.getScope() == CacheTimeManager.CALL)
    {
      ICallContext callContext = ThreadLocalContext.get();
      if ( callContext != null )
      {
        callContext.removeContextAttribute(key.toString());
      }
    }
    else
    {
      flush((CachePointer) cacheMap.get(key.toString()), recursive);
    }
  }

  /**
   * Flushes the object corresponding to the indicated pointer from the cacheMap. If recursive is set, it calls
   * object.flush() to process recursively.
   * 
   * @param pointer
   *          The pointer to flush from the cacheMap.
   * @param recursive
   *          Whether or not to flush recursively.
   */
  public void flush(CachePointer pointer, boolean recursive)
  {
    if (pointer != null)
    {
      Iterator it = pointer.getCacheKeys().iterator();
  
      while (it.hasNext())
      {
        Object object = it.next();
        cacheMap.remove(object.toString());
      }
  
      if ((recursive) && (pointer.getObject() != null))
      {
        pointer.getObject().flush();
      }
  
      pointer.expire();
    }
  }

  public void flush(int requestHash)
  {
    timeManager.flush(requestHash);
  }

  protected int size()
  {
    return cacheMap.size();
  }
  
  public List<CacheableObject> getAllCacheableObjectsForClass(Class cl)
  {
    List<CacheableObject> list = new ArrayList<CacheableObject>();
    
    if (cl == null)
      return list;
    
    String classPrefixStr = cl.toString() + "@";
    
    Set<String> copyCacheMapKeys = new HashSet<String>(cacheMap.keySet());
    for (String nextCacheKeyStr : copyCacheMapKeys)
    {
      if (nextCacheKeyStr.startsWith(classPrefixStr))
      {
        CachePointer item = (CachePointer) this.cacheMap.get(nextCacheKeyStr);
        if (item != null)
        {
          CacheableObject obj = item.getObject();
          if (obj != null)
            list.add(obj);
        }
      }
    }
    
    return list;
  }
  
}