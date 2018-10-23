package com.rate.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

class TimeList 
{
  /**
   * ArrayList used for holding CachePointers objects.
   */
  public static int SCALE = 60 * 1000;

  private LinkedList list = new LinkedList();
  private Object listLock = new Object();
  
  /**
   * "Shrinks" the input value to something that'll fint into an int. Current implementation returns time rounded to
   * nearest minute. This code will hiccup unpleasantly sometime in late 6052, so if that happens, the server should
   * probably be restarted.
   * 
   * @param time
   *          The time to convert (in milliseconds)
   * @return An int representing the time in minutes.
   */
  protected static int shrinkTime(long time)
  {
    return (int) (time / SCALE);
  }

  public boolean add(Object o)
  {
    synchronized(listLock)
    {
      return list.add(o);
    }
  }
  
  public boolean addAll(Collection c)
  {
    synchronized(listLock)
    {
      return list.addAll(c);
    }
  }
  
  public boolean remove(Object o)
  {
    synchronized(listLock)
    {
      return list.remove(o);
    }
  }
  
  public int size()
  {
    synchronized(listLock)
    {
      return list.size();
    }
  }
  
  public LinkedList getContents()
  {
    synchronized(listLock)
    {
      return (LinkedList)list.clone();
    }
  }
  
  /**
   * Generates a list of all objects stored in this with access-times before the indicated time, and removes those
   * entries from this list.
   */
  public ArrayList clearBefore(long time)
  {
    ArrayList oldies = new ArrayList();

    synchronized(listLock)
    {   
      boolean done = (list.size() == 0);
  
      while (!done)
      {      
        CacheTouch object = (CacheTouch) list.getFirst();
        if ( object == null )
        {
          list.removeFirst();
          done = (list.size() == 0);
          continue;
        }
        
        done = object.getTouchTime() >= shrinkTime(time);
  
        if (!done)
        {
          if (object.getTouchTime() == (object.getPointer().getLastAccessMinute()))
          {
            oldies.add(object.getPointer());
          }
  
          list.removeFirst();
          done = (list.size() == 0);
        }
      }
    }
    
    return oldies;
  }

  /**
   * Generates a list of all objects stored in this with tied to the request identified by the requestHashCode, and
   * removes those entries from this list.
   */
  public ArrayList clearFor(int requestHash)
  {
    ArrayList oldies = new ArrayList();

    synchronized(listLock)
    {
      for (int i = list.size() - 1; i >= 0; i--)
      {
        CacheTouch object = (CacheTouch) list.get(i);
  
        if (object.getPointer().getRequestHash() == requestHash)
        {
          oldies.add(object.getPointer());
          list.remove(i);
        }
      }
    }
    
    return oldies;
  }

//public ArrayList getLeastRecentlyUsed()
  public CachePointer getLeastRecentlyUsed()
  {
    synchronized(listLock)
    {   
      if (list.size()>0)
      {
        CacheTouch object = (CacheTouch) list.getFirst();
        if (object.getTouchTime() == (object.getPointer().getLastAccessMinute()))
        {
          CachePointer lru = ((CacheTouch)list.getFirst()).getPointer();
          list.removeFirst();
          return lru;
        }
        list.removeFirst();
        return null;
      }
      else
      {
        return null;
      }
    }
  }

  
  /**
   * Removes everything from the list and re-initializes it to empty.
   */
  public void flush()
  {
    synchronized(listLock)
    {
      list.clear();
    }
  }
}
