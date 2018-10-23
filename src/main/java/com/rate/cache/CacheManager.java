package com.rate.cache;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rate.context.ICallContext;
import com.rate.context.ThreadLocalContext;

public class CacheManager
{

  /**
   * the log4j logger object
   */
  public static final String NAME = "CACHE";

  private static CacheManager instance;

  private Hashtable<Integer, Cache> cacheMap;

  protected CacheManager()
  {
    cacheMap = new Hashtable<Integer, Cache>();
  }

  public synchronized static CacheManager getInstance()
  {
    if (instance == null)
    {
      instance = new CacheManager();
    }
    return instance;
  }

  public Cache getCache()
  {
    Integer stackId = 0;
    
    ICallContext callContext = ThreadLocalContext.get();
    if ( callContext != null )
    {
      if ( callContext.getStackId() != null )
        stackId = callContext.getStackId();
    }
    
    if ( !cacheMap.containsKey(stackId) )
      cacheMap.put(stackId, new Cache(stackId));
    
    return cacheMap.get(stackId);
  }
  
  public synchronized void reset()
  {
    getCache().clear();
  }

  public CacheTimeManager getTimeManager()
  {
    return getCache().getTimeManager();
  }

  /**
   * Sets the caching times...
   */
  public void setTimes(int shortTime, int mediumTime, int longTime)
  {
    getCache().getTimeManager().setTimes(shortTime, mediumTime, longTime);
  }

  public boolean containsObject(CacheableObject object)
  {
    return ((object != null) && containsKey(object.getPrimaryKey()));
  }

  public boolean containsKey(CacheKey key)
  {
    return getCache().containsKey(key);
  }

  /**
   * Puts the object into the cache accessed by its primary key. Simply calls put(null, object).
   *
   * @param object
   *          The object to add to the cache.
   */
  public void put(CacheableObject object)
  {
    getCache().put(null, object);
  }

  /**
   * Puts the object into the cache with the specified key. Does nothing if (object == null) or if
   * !object.isCacheable(). Otherwise, it ensures that the cache contains the object, accessed by the object's primary
   * key. If (key != null), then it also ensures that the cache contains the object, accessed by that key.
   * <P>
   * If the cache already contains an object with the same primary key as the specified object, then that object is kept
   * and the new object is not added.
   * </P>
   * <P>
   * The entry in the cache is updated with last-access time of now, whether or not new object/keys are added.
   *
   * @param key
   *          The key to access this objec by.
   * @param object
   *          The object to add to the cache.
   */
  public void put(CacheKey key, CacheableObject object)
  {
    getCache().put(key, object);
  }

  /**
   * Returns the object corresponding to the input key, if one is present in the cache.
   *
   * @param key
   *          The CacheKey to look the object up for.
   */
  public CacheableObject get(CacheKey key) throws Exception, Exception
  {
    return getCache().get(key);
  }

  /**
   * Looks in the cache for an object with a key that matches the key for the input object. Returns one if found.
   * Returns null if not. Does not add the object to the cache.
   *
   * @param The
   *          object to base the search on.
   */
  public CacheableObject get(CacheableObject object) throws Exception, Exception
  {    
    return ((object == null) ? (null) : (get(object.getPrimaryKey())));
  }

  /**
   * Returns an object corresponding to the indicated class and id, if there is one in the cache. If either parameter is
   * null or no class with the indicated name is found, then the ensuing NullPointerException or ClassNotFoundException
   * is ignored and null is returned.
   *
   * @param className
   *          The name of the class to find an object for.
   */
  public CacheableObject get(String className, Integer id) throws Exception, Exception
  {
    CacheableObject result = null;

    try
    {
      result = get(new CacheKey(Class.forName(className), id));
    }
    catch (ClassNotFoundException ignore)
    {
    }
    catch (NullPointerException ignore)
    {
    }

    return result;
  }

  /**
   * If the object is contained in the cache, it is removed from the cache.
   */
  public void flush(CacheableObject object)
  {
    if (object != null)
    {
      getCache().flush(object.getPrimaryKey(), false);
    }
  }

  /**
   * Flushes the object corresponding to the indicated key from the cache. If recursive is set, it calls object.flush()
   * to process recursively. Does nothing if no object corresponding to the key is found.
   *
   * @param key
   *          The key to flush from the cache.
   * @param recursive
   *          Whether or not to flush recursively.
   */
  public void flush(CacheKey key, boolean recursive)
  {
    getCache().flush(key, recursive);
  }

  /**
   * Flushes the object corresponding to the request hash code from the cache.
   *
   * @param requestHash
   *          The request hash code
   */
  public void flush(int requestHash)
  {
    getCache().flush(requestHash);
  }

  /**
   * Regenerates the object from the database and updates the cache
   */
  public void regenerate(CacheKey key)
  {
    getCache().regenerate(key);
  }

  /**
   * Converts the input string to an int, corresponding to the internally-used longevity value for that string..
   */
  public static int convertLongevity(String longevity)
  {
    return CacheTimeManager.convertLongevity(longevity);
  }

  /**
   * Returns the size of the cache (number of key-value) pairs.
   *
   * @author kenk
   *
   * @return the size of the cache
   */
  public int size()
  {
    return getCache().size();
  }

  /**
   * Returns a list of string representations of the cachemap
   *
   * @author jeffs
   *
   * @return the List of strings
   */
  public List outputContents()
  {
    Map cacheMap = getCache().getCacheMap();
    List outputList = new ArrayList();
    synchronized (getCache())
    {
      Set keys = cacheMap.keySet();
      Iterator itr = keys.iterator();
      String value = "";

      while (itr.hasNext())
      {
        Object key = (Object) itr.next();
        CachePointer pointer = (CachePointer) cacheMap.get(key);

        value = DateFormat.getTimeInstance(DateFormat.LONG).format(new Date(pointer.getLastAccessTime())) + " "
            + key.toString();
        outputList.add(value);

        Collections.sort(outputList);
      }
    }
    return outputList;
  }

  /**
   * Returns a list of items in the cachemap
   *
   * @author jeffs
   *
   * @return the List of strings
   */
  public Map getContents()
  {
    Map outputMap = new HashMap();
    Map cacheMap = getCache().getCacheMap();
    synchronized (getCache())
    {
      Iterator itr = cacheMap.entrySet().iterator();

      while (itr.hasNext())
      {
        Map.Entry myEntry = (Map.Entry) itr.next();
        CachePointer obj = (CachePointer) myEntry.getValue();

        try
        {
          outputMap.put(obj.toString(), obj.fetchObject());
        }
        catch (Exception e)
        {
          // TODO: Handle exception. Since this is a developer only option don't know how to handle.
        }
      }
    }

    return outputMap;
  }
  
  public List<CacheableObject> getAllCacheableObjectsForClass(Class cl)
  {
    return getCache().getAllCacheableObjectsForClass(cl);

  }

//  public Map getCacheMap()
//  {
//    return this.getCache().getCacheMap();
//  }
}
