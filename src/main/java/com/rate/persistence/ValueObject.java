package com.rate.persistence;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import com.rate.cache.CacheCriteria;
import com.rate.cache.CacheKey;
import com.rate.cache.CacheableObject;
import com.rate.constants.Constants;
import com.rate.persistence.helpers.Attribute;
import com.rate.persistence.validator.BasePersistenceValidator;
import com.rate.persistence.valueobject.GenericPersistenceStrategy;
import com.rate.persistence.valueobject.PrimaryKeyGenerator;
import com.rate.persistence.valueobject.ValueObjectConfiguration;
import com.rate.persistence.valueobject.ValueObjectList;

public class ValueObject extends Object implements Cloneable, Comparable, Serializable, CacheableObject
{
  public static final byte CLIENT = 0;

  public static final byte SERVER = 1;
  
  private static int MODIFIED_BIT = 0;

  private static int DELETED_BIT = 2;

  private static byte MODIFIED_STATUS_ON_MASK = (byte) (1 << MODIFIED_BIT);

  private static byte DELETED_STATUS_ON_MASK = (byte) (1 << DELETED_BIT);

  private static byte MODIFIED_STATUS_OFF_MASK = (byte) ~(1 << MODIFIED_BIT);

  private static byte DELETED_STATUS_OFF_MASK = (byte) ~(1 << DELETED_BIT);

  private Integer id;

  private String name;

  private String displayName;

  private String description;

  private Integer order;
  
  private byte modifiedDeletedStatus = 0x0;

  private Timestamp modifiedDate;

  private Timestamp addedDate;

  private Integer addedBy = null;

  private Integer modifiedBy = null;

  private Integer deletedBy = null;

  private static transient GenericPersistenceStrategy defaultStrategy = null;

  public static final String DEFAULT_STRATEGY_CLASS_NAME = "StoredProcedurePersistence";

  protected static transient Logger log;

  public static final String FIND_BY_PRIMARY_KEY = "findByPrimaryKey";

  public static final String DEFAULT_SAVE = "save";

  static final long serialVersionUID = -6222883795609458276L;

  protected static final String XML_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

  private static SimpleDateFormat xmlDateFormatter;

  private byte scope = 0;
  
  private byte isCacheable = -1;
  
  protected byte mode = SERVER;

  private final static Boolean xmlDateLock = new Boolean(true); // SF_15052
  
  private final static Boolean defaultStrategyLock = new Boolean(true);
  
  public ValueObject()
  {
    order = Constants.INTEGER_0;
    setMode(SERVER);
  }

  public ValueObject(byte mode)
  {
    order = Constants.INTEGER_0;
    setMode(mode);
  }

  public void setMode(byte mode)
  {
    this.mode = mode;

  }

  /**
   * Clears all data from the value object.
   */
  public void clear()
  {
    id = null;
    name = null;
    displayName = null;
    description = null;
    order = 0;
    modifiedDeletedStatus = 0x0;
    modifiedDate = null;
  }

  /**
   * Initializes this children list to empty lists. This method is called when deep loading this object.
   */
  public void initChildren()
  {
  }

  public void save(Integer userId) throws Exception
  {
    this.save(userId, getDefaultStrategy());
  }

  public void save(Integer userId, GenericPersistenceStrategy strategy) throws Exception
  {
    this.save(userId, DEFAULT_SAVE, strategy);
  }

  public void save(Integer userId, String methodName) throws Exception
  {
    this.save(userId, methodName, getDefaultStrategy());
  }

  /**
   * This method causes the object to save itself, according to its Configuration and the specified PersistenceStrategy.
   * Always calls strategt.save, whether this is modified or not. If (strategy == null), uses the strategy for this.
   * 
   * @param userId
   *          The user id to be used to indicate by whom this object was added/modified.
   * @param methodName
   *          The name of the method to use for saving.
   * @param strategy
   *          The persistence strategy to use for saving.
   */
  public final void save(Integer userId, String methodName, GenericPersistenceStrategy strategy)
      throws Exception
  {
    if (mode == CLIENT) return;

    if (!isModified())
    {
      return;
    }

    if (strategy == null)
    {
      strategy = getDefaultStrategy();

      if (strategy == null)
      {
        throw new Exception("Cannot save without a persistence strategy");
      }
    }

    if ( this.getConfiguration() != null )
    {
      ArrayList<String> validators = this.getConfiguration().getValidatorList();
      for (String validatorName : validators)
      {
        BasePersistenceValidator validator = BasePersistenceValidator.getValidator(validatorName);
        if (validator != null && !validator.validateSave(this))
        {
          Exception pve = new Exception("Persistence Security Violation: " + this.getClass().getName() + "["
              + (getId() != null ? getId() : "") + "] using " + methodName);
          pve.printStackTrace();
          
          throw pve;
        }
      }
    }
    
    // find out if this is the first time we've been saved
    // set the addedBy flag if we are, else set the moddedBy
    // no ID means object created from scratch
    boolean add = ((getId() == null) || (getId().intValue() == 0));

    if (add)
    {
      setAddedBy(userId);
    }
    else
    {
      setModifiedBy(userId);
      // have to set to null so sproc will work!
      setAddedBy(null);
    }

    strategy.save(this, methodName);
  }

  public Object clone() throws CloneNotSupportedException
  {
    ValueObject theClone = (ValueObject) super.clone();
    theClone.initClone();
    return theClone;
  }
  
  public void initClone()
  {
  }

  public Integer copy(Vector criteria)
  {
    if (mode == CLIENT) return null;

    ValueObjectFactory factory = ValueObjectFactory.getInstance();
    Integer object = null;

    try
    {
      object = factory.copy(this.getClass().getName(), criteria);
    }
    catch (Exception e)
    {
      // report the error
    }

    return object;
  }

  public void delete(Integer userId) throws Exception
  {
    this.delete(userId, getDefaultStrategy());
  }

  public void delete(Integer userId, GenericPersistenceStrategy strategy) throws Exception
  {
    this.delete(userId, DEFAULT_SAVE, strategy);
  }

  public void delete(Integer userId, String methodName) throws Exception
  {
    this.delete(userId, methodName, getDefaultStrategy());
  }

  /**
   * Delete is really just a call to save() with the deletedBy field set, as we don't ever really delete anything from
   * the DB
   */
  public void delete(Integer userId, String methodName, GenericPersistenceStrategy strategy)
      throws Exception
  {
    if (mode == CLIENT) return;

    if (strategy == null)
    {
      strategy = getDefaultStrategy();

      if (strategy == null)
      {
        throw new Exception("Cannot save without a persistence strategy");
      }
    }


    if ( this.getConfiguration() != null )
    {
      ArrayList<String> validators = this.getConfiguration().getValidatorList();
      for (String validatorName : validators)
      {
        BasePersistenceValidator validator = BasePersistenceValidator.getValidator(validatorName);
        if (validator != null && !validator.validateDelete(this))
        {
          Exception pve = new Exception("Persistence Security Violation: " + this.getClass().getName() + "["
              + (getId() != null ? getId() : "") + "] using " + methodName);
          pve.printStackTrace();
          
          throw pve;
        }
      }
    }
    
    setAddedBy(null);
    // not a problem most of the time, but best be sure.
    setModifiedBy(null);
    setDeletedBy(userId);
    // it won't save if it hasn't changed
    setIsModified(true);

    strategy.save(this, methodName);
    setIsDeleted(true);
  }

  // this is to "lazy-load" child ValueObject(List)s of other ValueObjects
  // As such, the search criteria should always be the primary key of the parent object.
  public ValueObject lazyLoadObject(String className, String method)
  {
    return lazyLoadObject(className, getId(), method);
  }

  public ValueObject lazyLoadObject(String className, Integer id, String method)
  {
    Vector criteria = new Vector();

    criteria.add(id);

    return lazyLoadObject(className, criteria, method);
  }

  public ValueObject lazyLoadObject(String className, Integer id1, Integer id2, String method)
  {
    Vector criteria = new Vector();

    criteria.add(id1);
    criteria.add(id2);

    return lazyLoadObject(className, criteria, method);
  }

  public ValueObject lazyLoadObject(String className, Vector criteria, String method)
  {
    if (mode == CLIENT) return null;

    ValueObjectFactory factory = ValueObjectFactory.getInstance();
    ValueObject object = null;

    try
    {
      object = factory.findObjectByCriteria(className, criteria, method);
    }
    catch (Exception e)
    {
      // report the error
    }

    return object;
  }

  public void load()
  {
    if (mode == CLIENT) return;

    Vector criteria = new Vector();

    criteria.add(id);

    ValueObject object = lazyLoadObject(this.getClass().getName(), getId(), FIND_BY_PRIMARY_KEY);

    if (object == null)
    {
      return;
    }

    ValueObjectConfiguration config = object.getConfiguration();

    Object[] names = config.getAttributeNames();

    for (int i = 0; i < names.length; i++)
    {
      Attribute attr = config.getAttribute((String) names[i]);

      Object value = null;

      try
      {
        PropertyDescriptor pd = attr.getPropertyDescriptor();

        if (pd == null)
        {
          continue;
        }

        Method readMethod = pd.getReadMethod();
        Method writeMethod = pd.getWriteMethod();

        Object[] readArgs = {};

        value = readMethod.invoke(object, readArgs);

        Object[] writeArgs = { value };

        writeMethod.invoke(this, writeArgs);
      }
      catch (InvocationTargetException ite)
      {
        String err = "ValueObject.Load() - Cannot set value " + attr.getName() + " on object "
            + this.getClass().getName() + " Reason = ";

      }
      catch (IllegalAccessException iae)
      {
        String err = "ValueObject.Load() - Cannot set value " + attr.getName() + " on object "
            + this.getClass().getName() + " Reason = ";

      }
      catch (IllegalArgumentException iarge)
      {
        String err = "ValueObject.Load() - Cannot set value " + attr.getName() + " on object "
            + this.getClass().getName();

        if (value != null)
        {
          err += "Value type = " + value.getClass().getName() + " Reason = ";
        }
        else
        {
          err += "Value is null. Reason = ";
        }

      }
    }
  }

  // this is to "lazy-load" child ValueObject(List)s of other ValueObjects
  // As such, the search criteria should always be the primary key of the parent object.
  protected ValueObjectList lazyLoadList(String className, String method)
  {
    return lazyLoadList(className, getId(), method);
  }

  protected ValueObjectList lazyLoadList(String className, Integer id, String method)
  {
    if (id == null)
    {
      return new ValueObjectList();
    }

    Vector criteria = new Vector();

    criteria.add(id);

    return lazyLoadList(className, criteria, method);
  }

  protected ValueObjectList lazyLoadList(String className, Vector criteria, String method)
  {
    if (mode == CLIENT) return new ValueObjectList();

    ValueObjectFactory factory = ValueObjectFactory.getInstance();
    ValueObjectList list = null;

    try
    {
      list = factory.findListByCriteria(className, criteria, method);
    }
    catch (Exception e)
    {

      // report the error

      // could not build child object for some reason...
      // I don't want to return null here as the object will keep trying
      // to load. So return an empty list instead...

      list = new ValueObjectList();
    }

    return list;
  }

  public static GenericPersistenceStrategy getDefaultStrategy()
  {
    synchronized(defaultStrategyLock)
    {
      if (defaultStrategy == null)
      {
        try
        {
          defaultStrategy = ValueObjectFactory.getInstance().createStrategy(DEFAULT_STRATEGY_CLASS_NAME);
          System.out.println("ValueObject.getDefaultStrategy : successfully created " + DEFAULT_STRATEGY_CLASS_NAME);
        }
        catch (Exception oce)
        {
          // report the error
          System.out.println("ValueObject.getDefaultStrategy : could not create " + DEFAULT_STRATEGY_CLASS_NAME);
        }
      }
      return defaultStrategy;
    }
  }
  
  public void setConfiguration(ValueObjectConfiguration config)
  {
    // configuration = config;
  }

  public ValueObjectConfiguration getConfiguration()
  {
    if (mode == CLIENT) return null;

    ValueObjectConfiguration configuration = null;
    try
    {
      configuration = ValueObjectFactory.getInstance().getObjectConfiguration(
          this.getClass());
    }
    catch (Exception oce)
    {
    }

    return configuration;
  }

  /**
   * Access method for the id property.
   * 
   * @return the current value of the id property
   */
  public Integer getId()
  {
    return id;
  }

  /**
   * Sets the value of the id property.
   * 
   * @param id
   *          the new value of the id property
   */
  public void setId(Integer id)
  {
    updateModifiedStatus(this.id, id);

    this.id = id;
  }

  public CacheableObject.ObjectGenerator getRegenerator()
  {
    return PrimaryKeyGenerator.getInstance();
  }

  public byte getCacheLongevity()
  {
    if ( scope == 0 )
    {
      ValueObjectConfiguration configuration = getConfiguration();
      if (configuration != null) 
        scope = configuration.getCacheScope();
    }
    
    return scope;
  }

  public void setCacheLongevity(byte scope)
  {
    this.scope = scope;
  }

  public boolean isCacheable()
  {
    if ( isCacheable == -1 ) 
    {
      ValueObjectConfiguration configuration = getConfiguration();
      if ( configuration != null && configuration.useCache())
        isCacheable = 1;
      else
        isCacheable = 0;
    }
    
    return (isCacheable == 1);
  }

  public void setIsCacheable(boolean cache)
  {
    if (cache)
      this.isCacheable = 1;
    else
      this.isCacheable = 0;
  }
  
  public CacheKey getPrimaryKey()
  {
    if (mode == CLIENT) return null;

    if (getId() != null) return new CacheKey(getClass(), new CacheCriteria(getId()), getRegenerator());

    return null;
  }

  public CacheKey getSecondaryKey()
  {
    return null;
  }

  public boolean isRefreshCacheOnUse()
  {
    ValueObjectConfiguration configuration = getConfiguration();
    return (configuration != null) && (Boolean.TRUE.equals(configuration.getCacheRefreshOnUse()));
  }

  /**
   * When a ValueObject is in a ValueObjectList, the list maintains three internal maps of its objects.
   * The maps are keyed by id, name and the results of getMapKey.  This allows a custom keys to be generated
   * by object type.
   *   
   * @return String     The key for the map
   */
  public String getMapKey()
  {
    return getName();
  }
  
  /**
   * Access method for the name property.
   * 
   * @return the current value of the name property
   */
  public String getName()
  {
    return name;
  }

  /**
   * Sets the value of the name property.
   * 
   * @param name
   *          the new value of the name property
   */
  public void setName(String name)
  {
    updateModifiedStatus(this.name, name);

    this.name = name;

    if (displayName == null)
    {
      displayName = name;
    }
  }

  /**
   * Access method for the displayName property.
   * 
   * @return the current value of the displayName property
   */
  public String getDisplayName()
  {
    return displayName;
  }

  /**
   * Sets the value of the displayName property.
   * 
   * @param displayName
   *          the new value of the displayName property
   */
  public void setDisplayName(String displayName)
  {
    updateModifiedStatus(this.displayName, displayName);

    this.displayName = displayName;
  }

  /**
   * Access method for the description property.
   * 
   * @return the current value of the description property
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * Sets the value of the description property.
   * 
   * @param description
   *          the new value of the description property
   */
  public void setDescription(String description)
  {
    updateModifiedStatus(this.description, description);

    this.description = description;
  }

  /**
   * Access method for the order property.
   * 
   * @return the current value of the order property
   */
  public Integer getOrder()
  {
    return order;
  }

  /**
   * Sets the value of the order property.
   * 
   * @param order
   *          the new value of the order property
   */
  public void setOrder(Integer order)
  {
    updateModifiedStatus(this.order, order);

    this.order = order;
  }

  /**
   * Determines if the isModified property is true.
   * 
   * @return <code>true<code> if the isModified property is true
   */
  public boolean isModified()
  {
    return (modifiedDeletedStatus & MODIFIED_STATUS_ON_MASK) > 0;
  }

  /**
   * Sets the value of the isModified property.
   * 
   * @param isModified
   *          the new value of the isModified property
   */
  public void setIsModified(boolean isModified)
  {
    if (isModified)
      this.modifiedDeletedStatus |= MODIFIED_STATUS_ON_MASK;
    else
      this.modifiedDeletedStatus &= MODIFIED_STATUS_OFF_MASK;
  }

  /**
   * Access method for the modifiedDate property.
   * 
   * @return the current value of the modifiedDate property
   */
  public Timestamp getModifiedDate()
  {
    return modifiedDate;
  }

  public Timestamp getAddedDate()
  {
    return addedDate;
  }

  /**
   * Sets the value of the modifiedDate property.
   * 
   * @param aModifiedDate
   *          the new value of the modifiedDate property
   */
  public void setModifiedDate(Timestamp modifiedDate)
  {
    updateModifiedStatus(this.modifiedDate, modifiedDate);

    this.modifiedDate = modifiedDate;
  }

  public void setAddedDate(Timestamp addedDate)
  {
    updateModifiedStatus(this.addedDate, addedDate);

    this.addedDate = addedDate;
  }

  public Integer getAddedBy()
  {
    return addedBy;
  }

  public void setAddedBy(Integer userId)
  {
    addedBy = userId;
  }

  public Integer getModifiedBy()
  {
    return modifiedBy;
  }

  public void setModifiedBy(Integer userId)
  {
    modifiedBy = userId;
  }

  public Integer getDeletedBy()
  {
    return deletedBy;
  }

  public void setDeletedBy(Integer userId)
  {
    deletedBy = userId;
  }

  /**
   * Getter for property isDeleted.
   * 
   * @return Value of property isDeleted.
   */
  public boolean isDeleted()
  {
    return (modifiedDeletedStatus & DELETED_STATUS_ON_MASK) > 0;
  }

  /**
   * Setter for property isDeleted.
   * 
   * @param isDeleted
   *          New value of property isDeleted.
   */
  public void setIsDeleted(boolean isDeleted)
  {
    if (isDeleted)
      this.modifiedDeletedStatus |= DELETED_STATUS_ON_MASK;
    else
      this.modifiedDeletedStatus &= DELETED_STATUS_OFF_MASK;
  }

  /**
   * Convenience method to easily determine last modified date Gets the most recent of the added and modified date No
   * setter for this.
   */
  public Timestamp getLastUpdateDate()
  {
    return ((getModifiedDate() != null) ? getModifiedDate() : getAddedDate());
  }

  /**
   * Updates the modified status of the value object by checking for property changes given two property values.&nbsp
   * Used by property setters in descendant classes.
   * 
   * @param oldValue -
   *          Original property value.
   * @param newValue -
   *          New property value.
   */
  public void updateModifiedStatus(Object oldValue, Object newValue)
  {
    // Only compare values if the value object is not already modified
    if (!isModified())
    {
      // Compare old and new values for equality
      if ((oldValue == null) && (newValue == null))
      {
        setIsModified(false);
      }
      else if (((oldValue == null) && (newValue != null)) || ((oldValue != null) && (newValue == null)))
      {
        setIsModified(true);
      }
      else if (oldValue != null && oldValue.equals(newValue))
      {
        if (newValue instanceof ValueObject)
        {
          setIsModified(((ValueObject) newValue).isModified());
        }
        else if (newValue instanceof ValueObjectList)
        {
          setIsModified(((ValueObjectList) newValue).isAnyModified());
        }
      }
      else
      {
        setIsModified(true);
      }
    }
  }

  public String getCacheKey()
  {
    if (id != null)
    {
      return getClass().getName() + ":" + id.toString();
    }
    else
    {
      return null;
    }
  }

  /**
   * Returns the string representation of the value object.
   * 
   * @return String - Value object's string representation.
   */
  public String toString()
  {
    if (getDisplayName() != null)
    {
      return getDisplayName();
    }
    else 
      if (getName() != null)
    {
      return getName();
    }
    else
    {
      return super.toString();
    }
  }

  /**
   * @param obj
   * @return int
   */
  public int compareTo(Object obj)
  {
    ValueObject v = (ValueObject) obj;

    if (this.getOrder() == null)
    {
      this.setOrder(Constants.INTEGER_0);
    }

    if (v.getOrder() == null)
    {
      v.setOrder(Constants.INTEGER_0);
    }

    return this.getOrder().compareTo(v.getOrder());
  }

  /**
   * @param object
   * @return boolean
   */
  public boolean equals(Object object)
  {
    if (object == null)
    {
      return false;
    }
    
    ValueObject vo = null;
    if ( object instanceof ValueObject)
    {
      vo = (ValueObject)object;
    }
      
    if ((this.getId() != null) && (vo != null && vo.getId() != null))
    {
      return this.getId().intValue() == vo.getId().intValue();
    }
    else
    {
      return super.equals(object);
    }

  }

  //SF_15052 SimpleDateFormat isn't thread-safe, needs synchronization
  public static String getXmlDateFormatter(Object value)
  {
    synchronized (xmlDateLock)
    {
      if (xmlDateFormatter == null)
      {
        xmlDateFormatter = new SimpleDateFormat(XML_DATE_FORMAT);
      }

      return xmlDateFormatter.format(value);
    }
  }    
  
  public static SimpleDateFormat getXmlDateFormatter()
  {
    synchronized (xmlDateLock)
    {
      if (xmlDateFormatter == null)
      {
        xmlDateFormatter = new SimpleDateFormat(XML_DATE_FORMAT);
      }

      return xmlDateFormatter;
    }
  }

  protected Timestamp xmlString2Timestamp(String str) throws Exception
  {
    try
    {
      Date newDate = getXmlDateFormatter().parse(str);

      return new Timestamp(newDate.getTime());
    }
    catch (Exception e)
    {
      throw new Exception(e);
    }
  }

  /**
   * Converts the indicated time to an XML-format date.
   */
  protected String timestamp2XmlString(Timestamp time) throws Exception
  {
    try
    {
      return getXmlDateFormatter().format(time);
    }
    catch (Exception e)
    {
      throw new Exception(e);
    }
  }

  /**
   * Binds the new object to the containers of the old object, since containers are transient
   */
  public void regenerate(CacheableObject obj)
  {
  }

  /**
   * Called when actively flushed from the cache. Remove from all containers.
   */
  public void flush()
  {
  }

  public void lazyLoadAll()
  {
  }
}
