package com.rate.cache;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CacheObjectFlushLRUThread extends Thread
{
  private Integer cacheMaxSize;

  private long cacheLruRunFrequency = 10000;

  private Boolean stopOnError = Boolean.TRUE;

  private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SS";

  private static final String PENDING = "PENDING";

  private static final String RUNNING = "RUNNING";

  private static final String STOPPED = "STOPPED";

  private static final long ONE_THOUSAND_MILLISECONDS = 1000;

  private static final int ONE_MEGABYTE = 1048576;

  private String threadStatus = PENDING;

  private JmxCacheTimeManager cacheMBean;

  private String serverName;

  public CacheObjectFlushLRUThread(JmxCacheTimeManager cacheMBean, Integer cacheMaxSize, Long cacheLruRunFrequency,
      Boolean stopOnError) throws RuntimeException
  {
    super();
    // this.serverName = JmxUtility.getInstance().getMyServerName();
    if (cacheMaxSize == null || cacheMBean == null || cacheLruRunFrequency == null || stopOnError == null)
    {
      String error = this.serverName + ":One or more required parameters are null";
      throw new RuntimeException(error);
    }
    this.cacheMBean = cacheMBean;
    this.cacheMaxSize = cacheMaxSize;
    this.cacheLruRunFrequency = cacheLruRunFrequency.longValue();
    this.stopOnError = stopOnError;
    this.threadStatus = PENDING;

    this.cacheMBean.updateCacheMaxSize(cacheMaxSize);
    this.cacheMBean.updateCacheLruRunFrequency(cacheLruRunFrequency);
    this.cacheMBean.updateStopOnError(stopOnError);
    this.cacheMBean.updateThreadStatus(this.threadStatus);
  }

  public void stopLRUFlushing()
  {
    threadStatus = STOPPED;
    this.cacheMBean.updateThreadStatus(STOPPED);
    DateFormat df = new SimpleDateFormat(dateFormat);
    String endTime = df.format(new Date());
  }

  public void run()
  {
    if (!threadStatus.equals(RUNNING))
    {
      threadStatus = RUNNING;
      DateFormat df = new SimpleDateFormat(dateFormat);
      String startTime = df.format(new Date());
      this.cacheMBean.updateThreadStatus(this.threadStatus);
    }
    while (threadStatus.equals(RUNNING))
    {
      try
      {
        try
        {
          Thread.sleep(this.getCacheLruRunFrequency().longValue() * ONE_THOUSAND_MILLISECONDS);
        }
        catch (InterruptedException e)
        {
        }
        flushExcessConfigs();
      }
      catch (Throwable t)
      {
        if (this.getStopOnError().booleanValue())
        {
          threadStatus = STOPPED;
        }
        this.cacheMBean.updateThreadStatus(this.threadStatus);
      }
    }
  }

  public void flushExcessConfigs()
  {
//    try
//    {
//      CacheManager cacheManager = CacheManager.getInstance();
//      Cache cache = cacheManager.getCache();
//      Map cacheMap = cache.getCacheMap();
//      if (cacheMap != null && cacheMap.size() > 0)
//      {
//        boolean overThreshHoldTriggered = false;
//        DateFormat df = new SimpleDateFormat(dateFormat);
//        String startTime = df.format(new Date());
//        int cacheSize = 0;
//        int threshHold = this.getCacheMaxSize().intValue() * ONE_MEGABYTE;
//        Collection cacheCol = null;
//        synchronized (cacheMap)
//        {
//          cacheCol = cacheMap.values();
//        }
//        Object[] cacheObjs = cacheCol.toArray();
//        cacheCol = null;
//        cacheSize = ObjectProfiler.sizeof(cacheObjs);
//        cacheObjs = null;
//        this.cacheMBean.updateLastCacheSize(cacheSize);
//        while (cacheSize > threshHold)
//        {
//          overThreshHoldTriggered = true;
//          log.debug("<overload>" + "<serverName>" + this.serverName + "</serverName>"
//              + "<status>threshhold exceeded</status>" + "<cacheSize>" + cacheSize + "</cacheSize>" + "<threshHold>"
//              + threshHold + "</threshHold>" + "</overLoad>");
//          long overLimitAmount = cacheSize - threshHold;
//          // Instead of recomputing the size of the entire cacheMap, just subtract the amount flushed
//          // long totalFlushed =
//          cacheManager.getTimeManager().flushLeastRecentlyUsed(overLimitAmount);
//          cacheMap = cache.getCacheMap();
//          synchronized (cacheMap)
//          {
//            cacheCol = cacheMap.values();
//          }
//          cacheObjs = cacheCol.toArray();
//          cacheCol = null;
//          cacheSize = ObjectProfiler.sizeof(cacheObjs);
//          cacheObjs = null;
//        }
//        String endTime = df.format(new Date());
//        String status = "ok";
//        if (overThreshHoldTriggered)
//        {
//          status = "adjusted";
//        }
//        log.debug("<update>" + "<serverName>" + this.serverName + "</serverName>" + "<status>" + status + "</status>"
//            + "<cacheSize>" + cacheSize + "</cacheSize>" + "<threshHold>" + threshHold + "</threshHold>"
//            + "<startTime>" + startTime + "</startTime>" + "<endTime>" + endTime + "</endTime>" + "</update>");
//      }
//    }
//    catch (java.util.ConcurrentModificationException cme)
//    {
//      log.error(this.serverName + ":Cache LRU", cme);
//      throw cme;
//    }
//    catch (java.lang.NullPointerException npe)
//    {
//      log.error(this.serverName + ":Cache LRU", npe);
//      throw npe;
//    }
  }

  /**
   * @param cacheMaxSize
   *          the cacheMaxSize to set
   */
  public void setCacheMaxSize(Integer cacheMaxSize)
  {
    this.cacheMaxSize = cacheMaxSize;
    this.cacheMBean.updateCacheMaxSize(cacheMaxSize);
  }

  /**
   * @return the cacheMaxSize
   */
  public Integer getCacheMaxSize()
  {
    return cacheMaxSize;
  }

  /**
   * @param cacheLruRunFrequency
   *          the cacheLruRunFrequency to set
   */
  public void setCacheLruRunFrequency(Long cacheLruRunFrequency)
  {
    this.cacheLruRunFrequency = cacheLruRunFrequency.longValue();
    this.cacheMBean.updateCacheLruRunFrequency(cacheLruRunFrequency);
  }

  /**
   * @return the cacheLruRunFrequency
   */
  public Long getCacheLruRunFrequency()
  {
    return new Long(cacheLruRunFrequency);
  }

  /**
   * @param stopOnError
   *          the stopOnError to set
   */
  public void setStopOnError(Boolean stopOnError)
  {
    this.stopOnError = stopOnError;
    this.cacheMBean.updateStopOnError(stopOnError);
  }

  /**
   * @return the stopOnError
   */
  public Boolean getStopOnError()
  {
    return stopOnError;
  }
}