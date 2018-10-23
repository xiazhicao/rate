package com.rate.cache;

import javax.management.PersistentMBean;

public interface JmxCacheTimeManagerMBean extends PersistentMBean
{
  public static String SET_LRU_COMMAND = "SET_LRU";
  
  public static String STOP_LRU_COMMAND = "STOP_LRU";
  
  public static String[] commandNames = { SET_LRU_COMMAND, STOP_LRU_COMMAND };

  public void setManagedResource(CacheTimeManager tm);

  public void load();
  
  public void store();
  
  /**
   * @param cacheUseLRUFlushing
   * @param cacheMaxSize
   * @param cacheLruRunFrequency
   * @param serverName
   * @param serversList
   */
  public void createAndConfigureLRUFlushThread(Boolean cacheUseLRUFlushing, Integer cacheMaxSize,
      Long cacheLruRunFrequency, String serverName, String serversList, Boolean stopOnError);

  public void stopLRUFlushing();
  
  /**
   * @return the lastCacheSize
   */
  public Integer getLastCacheSize();

  public String getThreadStatus();
  
  /**
   * @param cacheMaxSize the cacheMaxSize to set
   */
  public void setCacheMaxSize(Integer cacheMaxSize);

  /**
   * @return the cacheMaxSize
   */
  public Integer getCacheMaxSize();

  /**
   * @param cacheUseLRUFlushing the cacheUseLRUFlushing to set
   */
  public void setCacheUseLRUFlushing(Boolean cacheUseLRUFlushing);

  /**
   * @return the cacheUseLRUFlushing
   */
  public Boolean getCacheUseLRUFlushing();

  /**
   * @param cacheLruRunFrequency the cacheLruRunFrequency to set
   */
  public void setCacheLruRunFrequency(Long cacheLruRunFrequency);

  /**
   * @return the cacheLruRunFrequency
   */
  public Long getCacheLruRunFrequency();

  /**
   * @param stopOnError the stopOnError to set
   */
  public void setStopOnError(Boolean stopOnError);

  /**
   * @return the stopOnError
   */
  public Boolean getStopOnError();


}
