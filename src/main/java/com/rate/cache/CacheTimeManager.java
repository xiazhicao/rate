package com.rate.cache;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.management.ObjectName;

import com.rate.uitility.process.ProcessHelpers;

public class CacheTimeManager
{

  /**
   * The JMX object name for the CacheTimeManager MBean used in the Cache LRU eviction methods. 
   */
  public final static java.lang.String CACHE_TIME_MANAGER_MBEAN_NAME = "com.scholarone.mc:name=CacheTimeManagerMBean";
 
  private static final String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SS";

  public static final byte CALL = 0;

  public static final byte SHORT = 1;

  public static final byte MEDIUM = 2;

  public static final byte LONG = 3;

  public static final byte VERY_LONG = 4;

  public static final byte PERMANENT = 5;

  public static final String REQUEST_STRING = "REQUEST";

  public static final String SHORT_STRING = "SHORT";

  public static final String MEDIUM_STRING = "MEDIUM";

  public static final String LONG_STRING = "LONG";

  public static final String VERY_LONG_STRING = "VERY_LONG";

  public static final String PERMANENT_STRING = "PERMANENT";

  public static final byte MIN_SCOPE_VALUE = 0;

  public static final byte MAX_SCOPE_VALUE = PERMANENT;

  /** A list of strings, enabling simple lookup. */
  private static final ArrayList strings;

  private Logger log;

  private static ObjectName cacheObjectName;

  private static JmxCacheTimeManager cacheMBean;

  private DateFormat df;
  static
  {
    strings = new ArrayList();

    strings.add(REQUEST_STRING);
    strings.add(SHORT_STRING);
    strings.add(MEDIUM_STRING);
    strings.add(LONG_STRING);
    strings.add(VERY_LONG_STRING);
    strings.add(PERMANENT_STRING);
  }

  /** The number of states that are kept in lists. (PERMANENT has no list) */
  private static final int NUM_LISTS = 5;

  /** Times to live for various cache levels, in minutes. */
  private int[] times = new int[] { 0, 5, 15, 30, 120 };

  private Cache cache;

  private static CacheObjectFlushLRUThread flusher;

  private TimeList[] lists;

  /**
   * Default constructor, only to be used for serialization.
   */
  protected CacheTimeManager()
  {
    this(null);
  }

  /**
   * Sets the caching times...
   */
  public void setTimes(int shortTime, int mediumTime, int longTime)
  {
    if ( shortTime > 0 )
      times[1] = shortTime;
    if ( mediumTime > 0 )
      times[2] = mediumTime;
    if ( longTime > 0 )
      times[3] = longTime;
  }

  /**
   * Standard constructor. Sets the internal link to the specified Cache.
   * Also hooks up the JMX management bean
   */
  public CacheTimeManager(Cache cache)
  {
    try
    {
/*      cacheObjectName = new javax.management.ObjectName("com.scholarone.mc:name=CacheTimeManagerMBean");
      if (!JmxUtility.getInstance().getJmxMBeanServer().isRegistered(cacheObjectName))
      {
        cacheMBean = new JmxCacheTimeManager();
        JmxUtility.getInstance().getJmxMBeanServer().registerMBean(cacheMBean,cacheObjectName);
      }*/
      df = new SimpleDateFormat(dateFormat);
    } catch (Throwable t)
    {
    }
    if (cacheMBean != null)
    {
      cacheMBean.setManagedResource(this);
    }

    this.cache = cache;
    lists = new TimeList[NUM_LISTS];

    for (int i = 0; i < NUM_LISTS; ++i)
    {
      lists[i] = new TimeList();
    }

    initTimer();
  }

  /**
   * @param cacheUseLRUFlushing
   * @param cacheMaxSize
   * @param cacheLruRunFrequency
   * @param serverName
   * @param serversList
   */
  public static void createAndConfigureLRUFlushThread(Boolean cacheUseLRUFlushing, Integer cacheMaxSize,
      Long cacheLruRunFrequency, String serverName, String serversList, Boolean stopOnError)
  {
    if (cacheUseLRUFlushing!=null &&cacheMaxSize != null && cacheLruRunFrequency != null && serverName != null
        && serversList != null && stopOnError != null)
    {
      if (cacheUseLRUFlushing.booleanValue() && ProcessHelpers.isThisServerUsedByProcess(serverName, serversList))
      {
        if (getFlusher() != null) // also add - get status
        {
          getFlusher().stopLRUFlushing();
          setFlusher(null);
        }
        setFlusher(new CacheObjectFlushLRUThread(cacheMBean,cacheMaxSize,cacheLruRunFrequency,stopOnError));
        getFlusher().setName("Cache LRU Config Flusher");
        getFlusher().setDaemon(true);
        getFlusher().setPriority(Thread.MIN_PRIORITY);
        getFlusher().start();
      }
      else
      {
        if (getFlusher() != null) // also add - get status
          getFlusher().stopLRUFlushing();
        setFlusher(null);
      }
    }
  }

  /**
   * @param serverName
   * @param serversList text string list of servers to run LRU daemon on
   * @return
   */
  public static boolean isThisServerUsedByLruDaemon(String serverName, String serversList)
  {
    // Machine run list can be either:
    // "NONE" - daemon will not run on any machine
    // <empty> - daemon will run on all machines
    // -xxx,yyy - daemon will run on all machines EXCEPT xxx or yyy
    //  xxx,yyy - daemon will run only on machines xxx,yyy

    // Should we run on this machine?
    boolean runThisTimerFl = false;
    if (serversList != null && "NONE".equalsIgnoreCase(serversList))
      return runThisTimerFl; // is false
    if (serversList == null || serversList.trim().length() == 0)
      runThisTimerFl = true; // runs on any and all servers
    else
    {
      if (serversList.startsWith("-"))
      {
        runThisTimerFl = true;

        String machineList = serversList.substring(1, serversList.length());
        StringTokenizer st = new StringTokenizer(machineList, ","); // split based on ,
        while (st.hasMoreTokens())
        {
          String value = st.nextToken().toUpperCase().trim();
          if (value.length() > 0)
          {
            if (value.equalsIgnoreCase(serverName))
            {
              runThisTimerFl = false;
              break;
            }
          }
        }
      }
      else
      {
        runThisTimerFl = false;

        String machineList = serversList.substring(0, serversList.length());
        StringTokenizer st = new StringTokenizer(machineList, ","); // split based on ,
        while (st.hasMoreTokens())
        {
          String value = st.nextToken().toUpperCase().trim();
          if (value.length() > 0)
          {
            if (value.equalsIgnoreCase(serverName))
            {
              runThisTimerFl = true;
              break;
            }
          }
        }
      }
    }
    return runThisTimerFl;
  }


  /**
   * Initiates the timer for checking the cache.
   */
  protected void initTimer()
  {
    CacheExpirationTimerTask timer = new CacheExpirationTimerTask();

    timer.setTimeManager(this);
    timer.start(60000); // 1 minute.
  }

  /**
   * Determines which TimeList the CacheableObject should be placed into.
   */
  public TimeList getList(CacheableObject object)
  {
    int which = object.getCacheLongevity();

    which = Math.max(which, CALL); // Negative values get moved to "SHORT".

    if (which < NUM_LISTS)
    {
      return lists[which];
    }
    else
    {
      return null;
    }
  }

  /**
   * Checks all the lists,clearing entries entered before the indicated time. Handles synchronization internally.
   *
   * @param time
   *          The time before which objects should be discarded.
   */
  public void checkLists(long time)
  {
    System.out.println("Running CacheTimeManger.checkList()");
    
    if (cache != null)
    {
      synchronized (cache)
      {
        // exclude objects cached at request scope
        for (int ind = 1; ind < NUM_LISTS; ++ind)
        {
          long when = time - (times[ind] * TimeList.SCALE);

          checkList(ind, when);
        }
      }
    }
  }

  /**
   * Checks the indicated list, clearing entries entered before the indicated time. Should only be called from within a
   * <code>synchronized(cache)</code> block.
   *
   * @param ind
   *          Which list to check.
   * @param when
   *          The time before which objects should be discarded.
   */
  protected void checkList(int ind, long when)
  {
    synchronized(lists[ind])
    {
      ArrayList results = lists[ind].clearBefore(when);
      
      if ( results.size() > 0 )
      {
        System.out.println(strings.get(ind) + " list evicting " + results.size() + " items");
        if ( ind == 3 )
        {
          Iterator it = results.iterator();

          while (it.hasNext())
          {
            CachePointer ptr = (CachePointer) it.next();
            System.out.println(ptr.getObject());
          }
        }
        flush(results);
      }
    }
  }

  /**
   * This methods will remove enough items from the cache to lower
   * the total size of items in the cache by the overLimitAmount.
   * First all the TimeLists are combined into one, then it is sorted.
   * As the oldest items is removed, its size is calculated, the
   * item is flushed from cache, and its size is added to the total
   * number bytes removed until the goal is reached.
   *
   * Should only be called from within a <code>synchronized(cache)</code>
   * block.
   *
   * @param overLimitAmount a long giving the amount the
   * cache has exceeded the configurable limit set.
   */
  public long flushLeastRecentlyUsed(long overLimitAmount)
  {
    long totalFlushed = 0;
//    if (cache != null)
//    {
//      //Should only be called from within a <code>synchronized(cache)</code> block.
//      //synchronized (cache.getCacheMap())
//      //{
//        TimeList allTimeLists = combineTouchLists();
//
//        Date now = new Date();
//        while (totalFlushed < overLimitAmount && allTimeLists.size() > 0)
//        {
//          CachePointer lru = allTimeLists.getLeastRecentlyUsed();
//          if (lru != null)
//          {
//            Object obj = lru.fetchObject();
//            if (obj != null && obj.getClass() != null && obj.getClass().getName() != null)
//            {
//              long sizeLru = ObjectProfiler.sizeof(obj);
//              Date date = new Date(lru.getLastAccessTime());
//              long diff = now.getTime() - date.getTime();
//              String dateStr = df.format(date);
//              //String objName = obj.getClass().getName();
//              log.debug(
//                "<evicted>"+
//                  "<objectClass>"+obj.getClass().getName()+"</objectClass>"+
//                  "<objectSize>"+sizeLru+"</objectSize>"+
//                  "<lastAccessed>"+dateStr+"</lastAccessed>"+
//                  "<objectAge>"+getDateDiffString(diff)+"</objectAge>"+
//                "</evicted>"
//              );
//              cache.flush(lru, true);
//              totalFlushed += sizeLru;
//            }
//            else
//            {
//              cache.flush(lru, true);
//            }
//          }
//        }
//      //}
//    }
    return totalFlushed;
//
  }

  /**
   * A comparator for use in Collections.sort to put the cache
   * TimeLists into order by the last access time.
   *
   * @author John Black
   *
   */
  class compareCacheTimes implements Comparator
  {
    public int compare(Object o1, Object o2)
    {
      int result = 0;
      if (o1 instanceof CacheTouch && o2 instanceof CacheTouch)
      {
        if (((CacheTouch)o1).getPointer().getLastAccessTime() < ((CacheTouch)o2).getPointer().getLastAccessTime())
        {
          result = -1;
        }
        if (((CacheTouch)o1).getPointer().getLastAccessTime() > ((CacheTouch)o2).getPointer().getLastAccessTime())
        {
          result = 1;
        }
      }
      return result;
    }
  }

  /**
   * This method combines and sorts all the values in all the TimeLists
   * for the cache, thens sorts the resulting list by last access
   * time. When complete it returns a new TimeList
   *
   * @return TimeList linked list containing all the items in the cache
   * order by last access time.
   */
  public TimeList combineTouchLists()
  {
    TimeList allTimeLists = new TimeList();
    LinkedList list = new LinkedList();
    
    // exclude objects cached at request scope
    for (int ind = 1; ind < NUM_LISTS; ++ind)
    {
      list.addAll(lists[ind].getContents());
    }
    Collections.sort(list, new compareCacheTimes());
    allTimeLists.addAll(list);
    
    return allTimeLists;

  }

  /**
   * This routine is Duplicated in MC DateUtiltiy. Given a long
   * value this method will produce a String representation of the
   * duration in the following form:
   * days 'D' hours 'H' minutes 'M'
   *   123D12H34M
   * @param diff a long value representing the millisecond difference between two long time values
   * @return
   */
  public static String getDateDiffString(long diff)
  {
    String minus = (diff<0)?"-":"";
    diff = Math.abs(diff);
    String result = "";

    String miliStr = "";
    String secondsStr = "";
    String minutesStr = "";
    String hoursStr = "";
    String daysStr = "";

    long miliseconds = 0;
    long seconds = 0;
    long minutes = 0;
    long hours = 0;
    long days = 0;

    seconds = diff / 1000;
    miliseconds = diff-(seconds*1000);
    miliStr = (miliseconds >= 0)?"."+miliseconds+"S":"";
    if (seconds > 60)
    {
      minutes = seconds / 60;
      seconds = seconds-(minutes*60);
      secondsStr = ""+seconds;
      if (minutes > 60)
      {
        hours = minutes / 60;
        minutes = minutes-(hours*60);
        minutesStr = ""+minutes+"M";
        if (hours > 24)
        {
          days = hours / 24;
          hours = hours - (days*24);
          hoursStr = ""+hours+"H";
          daysStr = ""+days+"D";
        }
        else
        {
          hoursStr = ""+hours+"H";
        }
      }
      else
      {
        minutesStr = ""+minutes+"M";
      }
    }
    else
    {
      secondsStr = ""+seconds;
    }
    result = result + minus+daysStr+hoursStr+minutesStr+secondsStr+miliStr;
    return result;
  }

  private void flush(ArrayList results)
  {
    Iterator it = results.iterator();

    while (it.hasNext())
    {
      CachePointer ptr = (CachePointer) it.next();
      cache.flush(ptr, true);
    }
  }

  /**
   * Clear entries tied to this request Handles synchronization internally.
   *
   * @param requestHash
   *          The hashcode identifying the request.
   */
  public void flush(int requestHash)
  {
    if (cache != null)
    {
      synchronized (cache)
      {
        ArrayList results = lists[0].clearFor(requestHash);
        flush(results);
      }
    }
  }

  /**
   * Clears all the lists. Should only be called from within a <code>synchronized(cache)</code> block. /**
   */
  public void flushAll()
  {
    // exclude objects cached at request scope
    for (int i = 1; i < NUM_LISTS; ++i)
    {
      lists[i].flush();
    }
  }

  /**
   * Takes the indicated string and converts it to a longevity. Returns -1 for unrecognized strings (including null).
   * Comparison is case-insensitive.
   */
  public static int convertLongevity(String longevity)
  {
    return strings.indexOf(String.valueOf(longevity).toUpperCase());
  }

  /**
   * @param flusher the flusher to set
   */
  public static void setFlusher(CacheObjectFlushLRUThread _flusher)
  {
    flusher = _flusher;
  }

  /**
   * @return the flusher
   */
  public static CacheObjectFlushLRUThread getFlusher()
  {
    return flusher;
  }
}
