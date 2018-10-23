package com.rate.cache;

public class CacheTouch
{
  /**
   * 
   * @uml.property name="cachePointer"
   * @uml.associationEnd inverse="cacheTouch:com.scholarone.cache.CachePointer" multiplicity="(1 1)"
   * 
   */
  private CachePointer cachePointer;

  private int touchMinute;

  /**
   * 
   */
  public CacheTouch()
  {
    this(null, 0);
  }

  /**
   * Standard constructor. Stores the object internally, and adds this to the indicated list (if it is not null).
   * 
   * @param object
   *          The object to point to.
   * @param list
   *          The TimeList this is associated with (can be null).
   */
  public CacheTouch(CachePointer pointer, int touchMinute)
  {
    this.cachePointer = pointer;
    this.touchMinute = touchMinute;
  }

  /**
   * Get touch time
   * 
   * @return
   */
  public int getTouchTime()
  {
    return touchMinute;
  }

  /**
   * Get the cache pointer for this object
   * 
   * @return
   */
  public CachePointer getPointer()
  {
    return cachePointer;
  }
}
