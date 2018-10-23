package com.rate.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;

public class CacheList extends AbstractList implements CacheableObject, Serializable
{
  /** Increment for use in increasing the capacity. */
  private static final int CAPACITY_INCREMENT = 16;

  /** A static count of how many lists have been created. */
  protected static int listCount = 0;

  /** The objects held by this list. */
  protected transient CacheableObjectHolder[] objects = null;

  /** The size of the list. */
  protected transient int size;

  /** The id of this list. */
  protected int whichList;

  /** The time when this list was constructed. */
  protected long creationTime;

  /** The time when this list was constructed. */
  protected byte scope = CacheTimeManager.MEDIUM;
  
  protected boolean refreshOnCacheUse = true;

  /**
   * Creates an empty CacheList with default capacity.
   */
  public CacheList()
  {
    this(CAPACITY_INCREMENT);
  }

  /**
   * Creates a CacheList using a given intial capacity.
   * 
   * @param initialCapacity -
   *          initial size of the list
   */
  public CacheList(int initialCapacity)
  {
    whichList = ++listCount;
    creationTime = System.currentTimeMillis();

    ensureCapacity(initialCapacity);
  }

  /**
   * Constructor specifying initial capacity and id. Exists specifically for subclasses that may have need to specify
   * the id. When using this constructor, it becomes the subclass-writer's responsibility to guarantee uniqueness.
   * 
   * @param initialCapacity -
   *          initial size of the list
   * @param id
   *          The id to use for this list.
   */
  protected CacheList(int initialCapacity, int id, long time)
  {
    whichList = id;
    creationTime = time;

    ensureCapacity(initialCapacity);
  }

  /**
   * Creates a CacheList initially populated with a given collection.
   * 
   * @param collection -
   *          Collection of objects used to initial the list.
   */
  public CacheList(Collection collection)
  {
    this(collection.size());

    int i = 0;
    Iterator it = collection.iterator();

    while (it.hasNext())
    {
      add(i, it.next());

      i++;
    }
  }

  // Javadocs from superclass.
  public Object get(int index)
  {
    if ((index < 0) || (index >= size))
    {
      throw new IndexOutOfBoundsException("Index " + index + " is out of range, is < 0 or >= " + size);
    }

    CacheableObjectHolder cacheHolder = null;
    Object obj = null;
    cacheHolder = objects[index];

    // Object can not be found remove it from the list
    if (cacheHolder == null)
    {
      remove(index);
    }
    else
    {
      try
      {
        obj = cacheHolder.getObject();
      }

      // in this case, it's OK to just log the exception b/c if there was a
      // persistence exception,
      // it would have been thrown on the entire list (result set) not on an
      // individual item
      catch (Exception pe)
      {
      }
    }

    return obj;
  }

  // Javadocs from superclass.
  public int size()
  {
    return size;
  }

  /**
   * Sets the object at the specified index, and returns whatever was previously at that position.
   */
  public Object set(int index, Object object)
  {
    Object result = get(index);

    objects[index] = new CacheableObjectHolder(object);

    return result;
  }

  /**
   * Adds the object at the specified position, shifting all later objects back.
   */
  public void add(int index, Object element)
  {
    if ((index < 0) || (index > size))
    {
      throw new IndexOutOfBoundsException("Index " + index + " is out of range, is < 0 or > " + size);
    }

    ensureCapacity(size + 1);

    for (int i = size; i > index; --i)
    {
      objects[i] = objects[i - 1];
    }

    objects[index] = new CacheableObjectHolder(element);

    ++size;
  }

  /**
   * Removes and returns the object at the specified position, shifting later objects forward.
   */
  public Object remove(int index)
  {
    if ((index < 0) || (index >= size))
    {
      throw new IndexOutOfBoundsException("Index " + index + " is out of range, is < 0 or >= " + size);
    }

    Object result = null;

    try
    {
      if ((objects != null) && (objects[index] != null))
      {
        result = objects[index].getObject();
      }

      for (int i = index; i < (size - 1); ++i)
      {
        objects[i] = objects[i + 1];
      }

      objects[size - 1] = null;

      --size;
    }

    // in this case, it's OK to just log the exception b/c if there was a
    // persistence exception,
    // it would have been thrown on the entire list (result set) not on an
    // individual item
    catch (Exception pe)
    {
    }

    return result;
  }

  /**
   * Ensures that the capacity of this list is at least as large as newSize. Can be used to make adding a large number
   * of new elements more efficient.
   */
  public void ensureCapacity(int newSize)
  {
    if ((objects == null) || (newSize > objects.length))
    {
      // If newSize <= 0, round up. Also round anything less than increment.
      newSize = Math.max(newSize, CAPACITY_INCREMENT);

      // If newSize is not a multiple of CAPACITY_INCREMENT, round up.
      if ((newSize % CAPACITY_INCREMENT) != 0)
      {
        newSize += (CAPACITY_INCREMENT - (newSize % CAPACITY_INCREMENT));
      }

      CacheableObjectHolder[] newObjects = new CacheableObjectHolder[newSize];

      if (objects != null)
      {
        synchronized (objects)
        {
          System.arraycopy(objects, 0, newObjects, 0, objects.length);
        }
      }

      objects = newObjects;
    }
  }

  /**
   * Removes elements from <code>fromIndex</code> (inclusive) to <code>toIndex</code> (exclusive). Rounds fromIndex
   * up to zero and toIndex down to size(). Does nothing if (rounded) <code>fromIndex <= toIndex</code>.
   * <P>
   * Takes linear time, based on the distance from fromIndex to the end of the list.
   */
  protected void removeRange(int fromIndex, int toIndex)
  {
    fromIndex = Math.max(0, fromIndex);
    toIndex = Math.min(toIndex, size);

    if (toIndex > fromIndex)
    {
      int diff = toIndex - fromIndex;

      for (int i = fromIndex; i < (size - diff); ++i)
      {
        objects[i] = objects[i + diff];
      }

      for (int i = size - diff; i < size; ++i)
      {
        objects[i] = null;
      }

      size -= diff;
    }
  }

  /**
   * Returns an integer that's unique for this VM, for this CacheList.
   */
  public Integer getId()
  {
    return Integer.valueOf(whichList);
  }

  /**
   * Sets the id
   */
  protected void setId(Integer newId)
  {
    whichList = newId.intValue();
  }

  /**
   * Returns null, as there is no generic regenerator for CacheLists.
   */
  public ObjectGenerator getRegenerator()
  {
    return null;
  }

  /**
   * Returns scope
   */
  public byte getCacheLongevity()
  {
    return scope;
  }

  /**
   * Sets scope
   */
  public void setCacheLongevity(byte scope)
  {
    this.scope = scope;
  }

  /**
   * Returns false. A subclass needs to implement other methods (like getRegenerator()) in order to be meaningfully
   * cacheable.
   */
  public boolean isCacheable()
  {
    return true;
  }

  /**
   * Returns a cache-key that uniquley identifies this list.
   */
  public CacheKey getPrimaryKey()
  {
    CacheCriteria criteria = new CacheCriteria(new Object[] { Integer.valueOf(whichList), new Long(creationTime) });

    return new CacheKey(this.getClass(), criteria, getRegenerator());
  }

  public CacheKey getSecondaryKey()
  {
    return null;
  }

  public boolean isRefreshCacheOnUse()
  {
    return refreshOnCacheUse;
  }
  
  public void setRefreshCacheOnUse(boolean refreshOnCacheUse)
  {
    this.refreshOnCacheUse = refreshOnCacheUse;
  }

  /**
   * Implements flush() by calling clear(). Subclasses may wish to have this method call fluschChildren() instead.
   */
  public void flush()
  {
    clear();
  }

  public void regenerate(CacheableObject obj)
  {
  }

  /**
   * Flushes all the children (those that are cacheable) from the cache and then clears the list. The recursive
   * parameter is passed on to the CacheManager when flushing.
   * 
   * @param recursive
   *          Whether or not that CacheManager should flush the children recursively.
   */
  protected void flushChildren(boolean recursive)
  {
    CacheManager cacheManager = CacheManager.getInstance();

    for (int i = 0; i < size; ++i)
    {
      if (objects[i].getKey() != null)
      {
        cacheManager.flush(objects[i].getKey(), recursive);
      }
    }

    clear();
  }

  /**
   * Writes the list out to the specified object stream. Any entries that are CacheKeys have the relevant objects
   * written out, instead.
   * 
   * @param out
   *          The stream to write to.
   * @throws IOException
   *           If there is an IO problem.
   */
  private void writeObject(ObjectOutputStream out) throws IOException
  {
    out.defaultWriteObject();
    out.writeInt(size);

    for (int i = 0; i < size; ++i)
    {
      out.writeObject(get(i));
    }
  }

  /**
   * Reads in the list, from the specified stream. Calls the add() method to ensure CacheableObjects are handled
   * properly.
   * 
   * @param in
   *          The stream to read from
   * @throws IOException
   *           If there is an IO problem
   * @throws ClassNotFoundException
   *           If the class of a serialized object cannot be found.
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
  {
    in.defaultReadObject();

    int newSize = in.readInt();

    ensureCapacity(newSize);

    for (int i = 0; i < newSize; ++i)
    {
      add(in.readObject());
    }
  }
}