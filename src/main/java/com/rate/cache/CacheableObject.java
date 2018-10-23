package com.rate.cache;

public interface CacheableObject
{
  /**
   * Returns an int representing the longevity of objects in the cache. The meanings of various values is determined by
   * the caching algorithm.
   */
  public byte getCacheLongevity();

  /**
   * Sets the longevity of objects in the cache. The meanings of various values is determined by the caching algorithm.
   */
  public void setCacheLongevity(byte scope) throws Exception;

  /**
   * Returns whether or not this is cacheable.
   */
  public boolean isCacheable();

  /**
   * Returns a primary key for this object. Could simply return new CacheKey(this), but it need not do so.
   */
  public CacheKey getPrimaryKey();

  /**
   * Returns a secondary key for this object. Could simply return new CacheKey(this), but it need not do so.
   */
  public CacheKey getSecondaryKey();

  /**
   * Called when an object is actively flushed from the cache. If this object has sub-objects in the cache which should
   * also be flushed at this time, this call should handle that task.
   */
  public void flush();

  /**
   * Restore the object to its persisted state
   */
  public void regenerate(CacheableObject obj);

  /**
   * Returns whether or not an object's "last used" time should be updated each time it's used, or only when it's
   * inserted into the cache. Typical behavior would be to have this return true.
   * 
   * @return Whether or not to update the last-used time on each use.
   */
  public boolean isRefreshCacheOnUse();

  /**
   * An ObjectGenerator is an object that can restore an object of the specified class from a persisted state. It uses
   * the criteria to determine which object to restore.
   */
  public static interface ObjectGenerator
  {
    /**
     * Uses the criteria to decide which object of the specified class to restore.
     * 
     * @param objectClass
     *          The class of the object to be restored.
     * @param criteria
     *          The information this object will need to retrieve the specified object.
     */
    public CacheableObject regenerateObject(Class objectClass, CacheCriteria criteria) throws Exception;
  }
}
