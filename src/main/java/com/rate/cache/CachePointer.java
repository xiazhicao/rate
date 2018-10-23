package com.rate.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CachePointer
{
  /** The minute during which this was last accessed. */
  private int lastAccessMinute;

  /** Whether or not the object has ever been touched. */
  private boolean untouched = true;

  /** The last access time (in milliseconds) */
  private long lastAccessTime;

  /**
   * 
   * @uml.property name="object"
   * @uml.associationEnd inverse="cachePointer:com.scholarone.cache.CacheableObject" multiplicity="(1 1)"
   * 
   */
  private CacheableObject object;

  /**
   * 
   * @uml.property name="cacheKeys"
   * @uml.associationEnd inverse="cachePointer:com.scholarone.cache.CacheKey" multiplicity="(0 -1)"
   * 
   */
  private HashSet cacheKeys;

  /**
   * 
   * @uml.property name="timeList"
   * @uml.associationEnd inverse="cachePointer:com.scholarone.cache.TimeList" multiplicity="(0 1)"
   * 
   */
  private TimeList timeList;

  /**
   * 
   * @uml.property name="cacheTouch"
   * @uml.associationEnd inverse="cachePointer:com.scholarone.cache.CacheTouch" multiplicity="(0 -1)" ordering="ordered"
   * 
   */
  private ArrayList touchList;

  /** The hash code of the request this object is tied too */
  private int requestHash = 0;

  /**
   * Default constructor (used only for serialization).
   */
  protected CachePointer()
  {
    this(null, null);
  }

  /**
   * Stores the object and requestHash internally, and adds this to the indicated list (if it is not null).
   * 
   * @param object
   *          The object to point to.
   * @param list
   *          The TimeList this is associated with (can be null).
   * @param requestHash
   *          The current request hash
   */
  public CachePointer(CacheableObject object, TimeList list, int requestHash)
  {
    this(object, list);
    this.requestHash = requestHash;
  }

  /**
   * Standard constructor. Stores the object internally, and adds this to the indicated list (if it is not null).
   * 
   * @param object
   *          The object to point to.
   * @param list
   *          The TimeList this is associated with (can be null).
   */
  public CachePointer(CacheableObject object, TimeList list)
  {
    this.timeList = list;
    this.object = object;
    this.touchList = new ArrayList();
    this.cacheKeys = new HashSet();

    touch();
  }

  /**
   * Updates the access time. If the minute has changed, then adds this to the list again. If the
   * object.isRefreshCacheOneUse() returns false, then it only updates the first time this method is called.
   */
  public void touch()
  {
    if (untouched)
    {
      lastAccessTime = System.currentTimeMillis();
      lastAccessMinute = TimeList.shrinkTime(lastAccessTime);

      if (timeList != null)
      {
        CacheTouch touch = new CacheTouch(this, lastAccessMinute);
        touchList.add(touch);
        timeList.add(touch);
      }

      untouched = false;
    }
    else if ((object == null) || (object.isRefreshCacheOnUse()))
    {
      lastAccessTime = System.currentTimeMillis();

      int now = TimeList.shrinkTime(lastAccessTime);

      if (lastAccessMinute < now)
      {
        lastAccessMinute = now;

        if (timeList != null)
        {
          CacheTouch touch = new CacheTouch(this, lastAccessMinute);
          touchList.add(touch);
          timeList.add(touch);
        }
      }
    }
  }

  public long getLastAccessTime()
  {
    return lastAccessTime;
  }

  public long getLastAccessMinute()
  {
    return lastAccessMinute;
  }

  /**
   * Returns the object.
   * 
   * @return CacheableObject
   */
  public CacheableObject getObject()
  {
    touch();

    return object;
  }

  /**
   * Sets the object.
   * 
   * @param CacheableObject
   */
  public void setObject(CacheableObject object)
  {
    this.object = object;
  }

  /**
   * Causes this object to expire. Drops all keys and the object. Should be called after removing the keys from the
   * Cache.
   */
  public void expire()
  {
    cacheKeys.clear();

    if (timeList != null)
    {
      for (int i = 0; i < touchList.size(); i++)
      {
        CacheTouch touch = (CacheTouch) touchList.get(i);
        timeList.remove(touch);
      }
    }

    touchList.clear();
    untouched = true;

    object = null;
  }

  /**
   * This returns the object, without touching anything. Used for unit-testing.
   */
  public CacheableObject fetchObject()
  {
    return object;
  }

  /**
   * Returns the time-list. Used for unit-testing.
   */
  protected TimeList fetchTimeList()
  {
    return timeList;
  }

  /**
   * Returns the last-access-minute. Used for unit-testing.
   */
  protected int fetchLastAccessMinute()
  {
    return lastAccessMinute;
  }

  protected boolean fetchUntouched()
  {
    return untouched;
  }

  /**
   * Adds the key to the set of keys pointing to this.
   */
  public void addKey(CacheKey key)
  {
    cacheKeys.add(key);
  }

  /**
   * Returns a Set containing all the keys pointing to this. Set is live, so changes to it will be reflected in this
   * (though not in the Cache).
   */
  public Set getCacheKeys()
  {
    return cacheKeys;
  }

  /**
   * @return Returns the requestHash.
   */
  public int getRequestHash()
  {
    return requestHash;
  }

  /**
   * @param requestHash
   *          The requestHash to set.
   */
  public void setRequestHash(int requestHash)
  {
    this.requestHash = requestHash;
  }
}