package com.rate.cache;

import java.util.Timer;
import java.util.TimerTask;

class CacheExpirationTimerTask extends TimerTask
{
  /** The Timer internally associated with this. */
  protected Timer timer;

  /**
   * 
   * @uml.property name="timeManager"
   * @uml.associationEnd inverse="cacheExpirationTimerTask:com.scholarone.cache.CacheTimeManager" multiplicity="(1 1)"
   * 
   */
  private CacheTimeManager timeManager;

  /**
   * Default constructor.
   */
  public CacheExpirationTimerTask()
  {
  }

  public CacheTimeManager getTimeManager()
  {
    return timeManager;
  }

  public void setTimeManager(CacheTimeManager timeManager)
  {
    this.timeManager = timeManager;
  }

  /**
   * Starts the timer with the indicated interval.
   * 
   * @param interval
   *          The time between calls.
   */
  public void start(int interval)
  {
    timer = new Timer(true);

    timer.schedule(this, interval, interval);
  }

  /**
   * Calls checkLists(now) on the time-manager.
   */
  public void run()
  {
    timeManager.checkLists(System.currentTimeMillis());
  }
}