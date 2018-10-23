package com.rate.cache;

public class JmxCacheTimeManager implements JmxCacheTimeManagerMBean
{
  
  private int cacheMaxSize;
  
  private long cacheLruRunFrequency;
  
  private Boolean cacheUseLRUFlushing = Boolean.FALSE;
  
  private String serverName = "n/a";
  
  private String serversList = "NONE";
  
  private Boolean stopOnError = Boolean.TRUE;
  
  private int lastCacheSize = 0;
  
  private String threadStatus = "PENDING";
  
  private CacheTimeManager cacheTimeManager;
  /**
   * 
   */
  public JmxCacheTimeManager()
  {
  }

  public void setManagedResource(CacheTimeManager cm)
  {
    this.cacheTimeManager = cm;
  }

  public void load()
  {
    
  }
  
  public void store()
  {
    
  }

  /**
   * Calls the method to create and configure the LRU thread using the 
   * managed class object CacheTimeManager
   * @param cacheUseLRUFlushing
   * @param cacheMaxSize
   * @param cacheLruRunFrequency
   * @param serverName
   * @param serversList
   */
  public void createAndConfigureLRUFlushThread(Boolean cacheUseLRUFlushing, Integer cacheMaxSize,
      Long cacheLruRunFrequency, String serverName, String serversList, Boolean stopOnError)
  {
    try 
    {
      CacheTimeManager.createAndConfigureLRUFlushThread(cacheUseLRUFlushing, cacheMaxSize,
        cacheLruRunFrequency, serverName, serversList, stopOnError);
      this.cacheUseLRUFlushing = cacheUseLRUFlushing;
      this.cacheMaxSize = cacheMaxSize.intValue();
      this.cacheLruRunFrequency = cacheLruRunFrequency.longValue();
      this.serverName = serverName;
      this.serversList = serversList;
    }
    catch (Throwable t)
    {
    }
  }

  public void stopLRUFlushing()
  {
    this.cacheUseLRUFlushing = Boolean.FALSE;
    CacheTimeManager.getFlusher().stopLRUFlushing();
  }

  
  /**
   * @param lastCacheSize the lastCacheSize to set
   */
  public void updateLastCacheSize(int lastCacheSize)
  {
    this.lastCacheSize = lastCacheSize;
  }

  /**
   * @return the lastCacheSize
   */
  public Integer getLastCacheSize()
  {
    return Integer.valueOf(this.lastCacheSize);
  }

  
  /**
   * @param cacheMaxSize the cacheMaxSize to set
   */
  public void setCacheMaxSize(Integer cacheMaxSize)
  {
    this.cacheMaxSize = cacheMaxSize.intValue();
    CacheTimeManager.getFlusher().setCacheMaxSize(cacheMaxSize);
  }

  /**
   * @return the cacheMaxSize
   */
  public Integer getCacheMaxSize()
  {
    return Integer.valueOf(this.cacheMaxSize);
  }

  /**
   * @param cacheMaxSize the cacheMaxSize to set
   */
  public void updateCacheMaxSize(Integer cacheMaxSize)
  {
    this.cacheMaxSize = cacheMaxSize.intValue();
  }

  
  /**
   * @param cacheUseLRUFlushing the cacheUseLRUFlushing to set
   */
  public void setCacheUseLRUFlushing(Boolean cacheUseLRUFlushing)
  {
    this.cacheUseLRUFlushing = cacheUseLRUFlushing;
  }

  /**
   * @return the cacheUseLRUFlushing
   */
  public Boolean getCacheUseLRUFlushing()
  {
    return cacheUseLRUFlushing;
  }


  public String getThreadStatus()
  {
    return this.threadStatus;
  }

  public void updateThreadStatus(String threadStatus)
  {
    this.threadStatus = threadStatus;
  }

  
  /**
   * @param cacheLruRunFrequency the cacheLruRunFrequency to set
   */
  public void setCacheLruRunFrequency(Long cacheLruRunFrequency)
  {
    this.cacheLruRunFrequency = cacheLruRunFrequency.longValue();
    CacheTimeManager.getFlusher().setCacheLruRunFrequency(cacheLruRunFrequency);
  }

  /**
   * @return the cacheLruRunFrequency
   */
  public Long getCacheLruRunFrequency()
  {
    return new Long(cacheLruRunFrequency);
  }

  /**
   * @param cacheLruRunFrequency the cacheLruRunFrequency to set
   */
  public void updateCacheLruRunFrequency(Long cacheLruRunFrequency)
  {
    this.cacheLruRunFrequency = cacheLruRunFrequency.longValue();
  }
  
  /**
   * @param stopOnError the stopOnError to set
   */
  public void setStopOnError(Boolean stopOnError)
  {
    this.stopOnError = stopOnError;
    CacheTimeManager.getFlusher().setStopOnError(stopOnError);
  }

  /**
   * @return the stopOnError
   */
  public Boolean getStopOnError()
  {
    return stopOnError;
  }

  /**
   * @param stopOnError the stopOnError to set
   */
  public void updateStopOnError(Boolean stopOnError)
  {
    this.stopOnError = stopOnError;
  }

}
