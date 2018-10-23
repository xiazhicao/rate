package com.rate.persistence.valueobject;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.rate.cache.CacheList;
import com.rate.constants.Constants;
import com.rate.persistence.ValueObject;

public class ValueObjectList extends CacheList implements Serializable
{
  private static final long serialVersionUID = 1L;
  
  public final static Integer MOVE_UP = Constants.INTEGER_1;

  public final static Integer MOVE_DOWN = Constants.INTEGER_0;

  // this is for lists that have been sorted for conflicts
  // etc, and passed on through the system. Its a convenience
  // member to hold the number of results that have been supressed/removed.
  private int numSupressed;

  private Map mapByName = null;
  
  private Map mapByID = null;
  
//  private Map mapByUserKey = null;

  /**
   * Creates an empty ValueObjectList object.
   */
  public ValueObjectList()
  {
    super();
  }

  /**
   * Creates a ValueObjectList using a given initial capacity.
   * 
   * @param initialCapacity -
   *          initial size of the list
   */
  public ValueObjectList(int initialCapacity)
  {
    super(initialCapacity);
  }

  /**
   * Creates a ValueObjectList initially populated with a given collection.
   * 
   * @param collection -
   *          Collection of objects used to initial the list.
   */
  public ValueObjectList(Collection collection)
  {
    super(collection);
  }

  /**
   * Creates a ValueObjectList initially populated with a given Map.
   * 
   * @param map -
   *          Map of objects used to initial the list where the key is the id
   */
  public ValueObjectList(Map map)
  {
    super();

    if (map != null)
    {
      Collection col = map.values();
      Object[] objects = col.toArray();

      for (int i = 0; i < objects.length; i++)
      {
        add((ValueObject) objects[i]);
      }
    }
  }

  /**
   * Indicates whether or not the list has changed. Also checks for modification of elements if they are of type
   * ValueObject.
   * 
   * @return boolean - Modification indicator
   */
  public synchronized boolean isAnyModified()
  {
    // Initialize return value
    int index = 0;

    // Check for changes to ValueObject elements
    boolean anyModified = false;

    while (!anyModified && (index < size()))
    {
      if (get(index) instanceof ValueObject)
      {
        anyModified = ((ValueObject) get(index)).isModified();
      }

      index++;
    }

    return anyModified;
  }

  /**
   * Replaces the element at the specified position in this list with the specified element. Overridden to handle
   * modification indicator.
   * 
   * @param index -
   *          Index of element to replace.
   * @param element -
   *          Element to be stored at the specified position.
   * @return Object - Element previously at the specified position.
   */
  public synchronized Object set(int index, Object element)
  {
    mapByName = null;    
    mapByID = null;    
//    mapByUserKey = null;

    Object original = super.set(index, element);

    return original;
  }

  /**
   * Appends the specified element to the end of this list. Overridden to handle modification indicator.
   * 
   * @param element -
   *          Element to be appended to this list.
   * @return boolean - Success indicator.
   */
  public synchronized boolean add(Object element)
  {
    mapByName = null;    
    mapByID = null;    
//    mapByUserKey = null;

    boolean success = super.add(element);

    return success;
  }

  /**
   * Inserts the specified element at the specified position in this list. Overridden to handle modification indicator.
   * 
   * @param index -
   *          Index at which the specified element is to be inserted.
   * @param element -
   *          Element to be inserted.
   */
  public synchronized void add(int index, Object element)
  {
    mapByName = null;    
    mapByID = null;    
//    mapByUserKey = null;

    super.add(index, element);
  }

  /**
   * Removes the element at the specified position in this list. Overridden to handle modification indicator.
   * 
   * @param index -
   *          Index of the element to removed.
   * @return Object - Element that was removed from the list.
   */
  public synchronized Object remove(int index)
  {
    mapByName = null;    
    mapByID = null;    
//    mapByUserKey = null;

    Object element = super.remove(index);

    return element;
  }

  /**
   * Removes a single instance of the specified element from this collection, if it is present (optional operation).
   * More formally, removes an element e such that (o==null ? e==null : o.equals(e)), if the collection contains one or
   * more such elements. Returns true if the collection contained the specified element (or equivalently, if the
   * collection changed as a result of the call).
   * 
   * @param o -
   *          element to removed.
   * @return boolean - Success indicator
   */
  public synchronized boolean remove(Object o)
  {
    mapByName = null;    
    mapByID = null;    
//    mapByUserKey = null;

    return (contains(o) ? super.remove(o) : false);
  }

  /**
   * Appends all of the elements in the specified Collection to the end of this list. Overridden to handle modification
   * indicator.
   * 
   * @param collection -
   *          Elements to be inserted into this list.
   * @return boolean - indicating result of addAll
   */
  public synchronized boolean addAll(Collection collection)
  {
    mapByName = null;    
    mapByID = null;    
//    mapByUserKey = null;

    boolean success = super.addAll(collection);

    return success;
  }

  /**
   * Inserts all of the elements in the specified Collection into this list, starting at the specified position.
   * Overridden to handle modification indicator.
   * 
   * @param index -
   *          Index at which to insert first element from the specified collection.
   * @param collection -
   *          Elements to be inserted into this list.
   * @return boolean - indicating result of addAll
   */
  public synchronized boolean addAll(int index, Collection collection)
  {
    mapByName = null;    
    mapByID = null;    
//    mapByUserKey = null;

    boolean success = super.addAll(index, collection);

    return success;
  }

  /**
   * @return java.util.Map
   */
  public synchronized Map getMapById()
  {
    if ( mapByID == null )
    {
      // start off with correct initial size to speed up
      mapByID = Collections.synchronizedMap(new HashMap(size()));
        
      for (int i = 0; i < size(); i++)
      {
        Object element = get(i);
  
        if (element instanceof ValueObject)
        {
          ValueObject value = (ValueObject) element;
  
          if (value.getId() != null)
          {
            try {
            mapByID.put(value.getId(), value);
            } catch (NullPointerException npe) {
              if ( mapByID == null )
                System.out.println("Map is null");
            }
          }
        }
      }
    }
    
    return mapByID;
  }

  /**
   * @return java.util.Map
   */
  public synchronized Map getMapByName()
  {
    if ( mapByName == null ) 
    {
      // start off with correct initial size to speed up
      mapByName = Collections.synchronizedMap(new HashMap(size()));
  
      for (int i = 0; i < size(); i++)
      {
        Object element = get(i);
  
        if (element instanceof ValueObject)
        {
          ValueObject value = (ValueObject) element;
  
          if ((value.getName() != null) && (value.getName().trim().length() > 0))
          {
            mapByName.put(value.getName(), value);
          }
        }
      }
    }
    
    return mapByName;
  }

  /**
   * @return java.util.Map
   */
  /*
  public synchronized Map getMapByUserKey()
  {
    if ( mapByUserKey == null ) 
    {
      // start off with correct initial size to speed up
      mapByUserKey = Collections.synchronizedMap(new HashMap(size()));
  
      for (int i = 0; i < size(); i++)
      {
        Object element = get(i);
  
        if (element instanceof ValueObject)
        {
          ValueObject value = (ValueObject) element;
  
          String key = value.getMapKey();
          if ((key != null) && (key.trim().length() > 0))
          {
            mapByUserKey.put(key, value);
          }
        }
      }
    }
    
    return mapByUserKey;
  }
  */
  
  public synchronized void sort()
  {
    Collections.sort(this);
  }

  public synchronized void move(ValueObject object, Integer moveDirection, int moves)
  {
    int newPos = 0;
    int pos = -1;

    for (int i = 0; i < size(); i++)
    {
      if (get(i).equals(object))
      {
        pos = i;

        break;
      }
    }

    if (pos >= 0)
    {
      if (moveDirection.equals(MOVE_UP))
      {
        newPos = pos - moves;

        if (newPos < 0)
        {
          newPos = 0;
        }

        remove(pos);
        add(newPos, object);
      }
      else if (moveDirection.equals(MOVE_DOWN))
      {
        newPos = pos + moves + 1;

        if (newPos > size())
        {
          newPos = size();
        }

        add(newPos, object);
        remove(pos);
      }
    }

    resetOrder();
  }

  public synchronized void resetOrder()
  {
    int order = 10;
    ValueObject object = null;

    for (int i = 0; i < size(); i++)
    {
      Object element = get(i);

      if (element instanceof ValueObject)
      {
        object = (ValueObject) element;

        object.setOrder(Integer.valueOf(order));

        order += 10;
      }
    }
  }

  public synchronized Object findById(Integer id)
  {
    Object o = null;
    
    getMapById();
    
    if ( mapByID != null )
    {
      o = mapByID.get(id);
    }
    
    return o;
  }
  
  public synchronized Object findByName(String name)
  {
    Object o = null;
    
    getMapByName();
    
    if ( mapByName != null )
    {
      o = mapByName.get(name);
    }
    
    return o;
  }

//  public synchronized Object findByUserKey(String key)
//  {
//    Object o = null;
    
//    getMapByUserKey();
    
//    if ( mapByUserKey != null )
//    {
//      o = mapByUserKey.get(key);
//    }
    
//    return o;
//  }
  
  public synchronized Object clone()
  {
    ValueObjectList clone = new ValueObjectList();

    try
    {
      for (int i = 0; i < this.size(); i++)
      {
        Object element = get(i);

        if (element instanceof ValueObject)
        {
          clone.add(((ValueObject) element).clone());
        }
      }
    }
    catch (CloneNotSupportedException cnse)
    {
      System.out.println(cnse.getMessage());
    }

    return clone;
  }

  /**
   * Getter for property numSupressed.
   * 
   * @return Value of property numSupressed.
   */

  public int getNumSupressed()
  {
    return numSupressed;
  }

  /**
   * Setter for property numSupressed.
   * 
   * @param numSupressed
   *          New value of property numSupressed.
   */
  public void setNumSupressed(int numSupressed)
  {
    this.numSupressed = numSupressed;
  }

  public boolean isCacheable()
  {
    return true;
  }

  /**
   * Takes the items in the list in their current order and resets their order property 1,2,...,N
   */
  public synchronized void reOrderInPlace()
  {
    for (int i = 0; i < size(); i++)
    {
      ValueObject obj = (ValueObject) get(i);

      obj.setOrder(Integer.valueOf(i + 1));
    }
  }
  
  /**
   * Extracts a sublist of ValueObjects where the name begins with the supplied letter.
   * 
   * @param letter      The letter used for comparison.  The name of the ValueObject must start with this letter.
   * @return            ValueObjectList containing the matching ValueObjects
   */
  public synchronized ValueObjectList extractObjectsByFirstLetter(char letter)
  {
    ValueObjectList newList = new ValueObjectList();
    boolean started = false;
    for (int i = 0; i < size(); i++)
    {
      ValueObject object = (ValueObject) get(i);
      if (object.getName() != null && object.getName().length() > 0
          && object.getName().charAt(0) == letter)
      {
        started = true;
        newList.add(object);
      }
      else if (started)
      {
        // means we hit the last one so quit looping
        break;
      }
    }

    return newList;
  }
  
  public String toString()
  {
    if ( this.size() == 0 ) return "Empty list";
    
    Object o = (Object)this.get(0);
    return "List of " + o.getClass().getName() + "[" + this.size() + "]";
  }
}