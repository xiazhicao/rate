package com.rate.persistence;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.rate.cache.CacheKey;
import com.rate.cache.CacheManager;
import com.rate.cache.CacheTimeManager;
import com.rate.persistence.validator.BasePersistenceValidator;
import com.rate.persistence.valueobject.GenericPersistenceStrategy;
import com.rate.persistence.valueobject.ValueObjectConfiguration;
import com.rate.persistence.valueobject.ValueObjectConfigurationReader;
import com.rate.persistence.valueobject.ValueObjectList;
import com.rate.persistence.valueobject.ValueObjectListGenerator;

public class ValueObjectFactory
{
  private static ValueObjectFactory instance = new ValueObjectFactory();

  /**
   * We store all read-in configs to eliminate unneeded disk access
   */
  private HashMap configCache = null;

  private HashMap strategyCache = null;

  
  /**
   * We will support only 1 persistence strategy in the system at a time right now, as a minor performance optimization.
   * If support for multiple is required later, we will redefine this as a Hashtable and do a lookup.
   */
  private static GenericPersistenceStrategy strategy = null;

  /**
   * ValueObjectFactory is a Singleton
   */
  public static ValueObjectFactory getInstance()
  {
    return instance;
  }

  protected ValueObjectFactory()
  {
    configCache = new HashMap(100); // start with a good-sized cache
    strategyCache = new HashMap(5);
  }

  public synchronized void flush()
  {

    System.out.println("[JBOSS] ValueObjectFactory.flush");
  }

  public ValueObject createObjectFromConfig(ValueObjectConfiguration config) throws Exception
  {
    ValueObject newOb = null;

    try
    {
      newOb = (ValueObject) config.getType().newInstance();

      // from this point on the object will have a reference to its config
      newOb.setConfiguration(config);
    }
    catch (IllegalAccessException iae)
    {
      throw new Exception("Cannot create ValueObject: " + iae.toString());
    }
    catch (InstantiationException ie)
    {
      throw new  Exception("Cannot create ValueObject: " + ie.toString());
    }

    return newOb;
  }
 /*
  * creates and returns a strategy. 
  * XML, and by extension XMLSPPlus is not thread safe. Do
  * not cache and reuse. GAM 1/27/2010
  * 
  */
  public GenericPersistenceStrategy createStrategy(String className) throws Exception
  {
    if (strategyCache.containsKey(className))
    {
      return (GenericPersistenceStrategy) strategyCache.get(className);
    }

    try
    {
      Class type = Class.forName(className);

      strategy = (GenericPersistenceStrategy) type.newInstance();
      if (strategy.isStoredInCacheFl())
      {
        strategyCache.put(className, strategy);
      }
    }
    catch (ClassNotFoundException cnfe)
    {
      throw new Exception("Cannot create strategy: " + cnfe.toString());
    }
    catch (IllegalAccessException iae)
    {
      throw new Exception("Cannot create strategy: " + iae.toString());
    }
    catch (InstantiationException ie)
    {
      throw new Exception("Cannot create strategy: " + ie.toString());
    }

    return strategy;
  }

  public ValueObjectConfiguration getObjectConfiguration(String className) throws Exception
  {
//    log.debug("Entering ValueObjectFactory.getObjectConfiguration");

    try
    {
      Class type = Class.forName(className);

      return getObjectConfiguration(type);
    }
    catch (ClassNotFoundException cnfe)
    {
      throw new Exception("Cannot find class " + className + ": " + cnfe.toString());
    }
  }

  public ValueObjectConfiguration getObjectConfiguration(Class type) throws Exception
  {
    ValueObjectConfiguration config = null;

    String typeName = type.getName();

    // find it in the cache first, return it if it is found
    if (configCache.containsKey(typeName))
    {    
      return (ValueObjectConfiguration) configCache.get(typeName);
    }

    // otherwise read it from disk then cache it
    try
    {
      ValueObjectConfigurationReader reader = new ValueObjectConfigurationReader();

      config = reader.readConfiguration(type);
    }
    catch (Exception xce)
    {
      System.out.println("Configuration problem with class " + typeName + ": " + xce.toString());
    }      

    if ( config != null )
    {
      config.setType(type);
    }
    
    configCache.put(typeName, config);
    
    return config;
  }

  /**
   * Finds a single object of type typeName matching the given criteria, using the finder method name given. Goes one
   * level deep, will initialize sub-objects from additional result sets
   */
  public ValueObject findObjectByCriteria(String typeName, Vector criteria, String finderName)
      throws Exception, Exception
  {
    return findObjectByCriteria(typeName, criteria, finderName, null);
  }

  /**
   * Overloaded method call with argument useCache
   */
  public ValueObject findObjectByCriteria(String typeName, Vector criteria, String finderName, boolean useCache)
  throws Exception, Exception
  {
    return findObjectByCriteria(typeName, criteria, finderName, null, useCache);
  }
  
  /**
   * Overloaded method call takes Class param instead of String
   */
  public ValueObject findObjectByCriteria(Class classType, Vector criteria, String finderName)
      throws Exception, Exception
  {
    return findObjectByCriteria(classType.getName(), criteria, finderName, null);
  }

  /**
   * Overloaded method call with argument useCache
   */  
  public ValueObject findObjectByCriteria(Class classType, Vector criteria, String finderName, boolean useCache)
  throws Exception, Exception
  {
    return findObjectByCriteria(classType.getName(), criteria, finderName, null, useCache);
  }
   /* Overloaded method call takes Class param instead of String
   */
  public ValueObject findObjectByCriteria(Class classType, Vector criteria, String finderName, String strategyClassName)
      throws Exception, Exception
  {
    return findObjectByCriteria(classType.getName(), criteria, finderName, strategyClassName);
  }
  
  /**
   * Overloaded method call defaults the useCache flag to 'true'
   */
  public ValueObject findObjectByCriteria(String typeName, Vector criteria, String finderName, String strategyClassName)
  throws Exception, Exception
  {
    return findObjectByCriteria(typeName, criteria, finderName, strategyClassName, true);
  }
  
  /**
   * Finds a single object of type typeName matching the given criteria, using the finder method name given. Goes one
   * level deep, will initialize sub-objects from additional result sets
   */
  public ValueObject findObjectByCriteria(String typeName, Vector criteria, String finderName, String strategyClassName, boolean useCache)
      throws Exception, Exception
  {
    ValueObject obj = null;
    CacheKey key = null;

    CacheManager cacheManager = CacheManager.getInstance();

    
    ValueObjectConfiguration config = getObjectConfiguration(typeName);
    if (strategyClassName == null)
    {
      strategyClassName = ValueObject.DEFAULT_STRATEGY_CLASS_NAME;
    }

    if ( config.useCache() && useCache )
    {
      try
      {
        // If FIND_BY_PRIMARY_KEY, assume first item in criteria is the id and look in cache
        if (ValueObject.FIND_BY_PRIMARY_KEY.equals(finderName))
        {
          key = new CacheKey(Class.forName(typeName), criteria.get(0));
        }
        else
        {
          key = new CacheKey(Class.forName(typeName), new Object[] { finderName, criteria });
        }
      }
      catch (ClassNotFoundException cnfe)
      {
      }
  
      obj = (ValueObject) cacheManager.get(key);
      
      if (obj == null)
      {
        GenericPersistenceStrategy strategy = createStrategy(strategyClassName);

        obj = strategy.loadObject(config, criteria, finderName);
        cacheManager.put(key, obj);
      }
    }
    else
    {
      GenericPersistenceStrategy strategy = createStrategy(strategyClassName);
      obj = strategy.loadObject(config, criteria, finderName);
    }

    if ( obj != null && obj.getConfiguration() != null )
    {
      ArrayList<String> validators = obj.getConfiguration().getValidatorList();
      for (String validatorName : validators)
      {
        BasePersistenceValidator validator = BasePersistenceValidator.getValidator(validatorName);
        if (validator != null && !validator.validate(obj))
        {
          Exception pve = new Exception("Persistence Security Violation: " + this.getClass().getName() + "["
              + (obj.getId() != null ? obj.getId() : "") + "] using " + finderName);
          pve.printStackTrace();
          
          throw pve;
        }
      }
    }
    
    return obj;
  }

  public Integer copy(String className, Vector criteria) throws Exception
  {
//    log.debug("Entering ValueObjectFactory.copy");

    ValueObjectConfiguration config = getObjectConfiguration(className);
    GenericPersistenceStrategy strategy = ValueObject.getDefaultStrategy();

    Integer object = strategy.copy(config, criteria);

    return object;
  }

  /**
   * overloaded method call takes Class param instead of String
   */
  public ValueObjectList findListByCriteria(Class classType, Vector criteria, String finderName)
      throws Exception, Exception
  {
    return findListByCriteria(classType.getName(), criteria, finderName, false);
  }

  /**
   * Returns a ValueObjectList of objects of type typeName matching the given criteria, using the finder method name
   * given.
   * 
   * @param typeName
   *          The name of the type of object to be found.
   * @param criteria
   *          The criteria to use to find the object.
   * @param finderName
   *          The name of the finderMethod to find the objects with.
   * @return A ValueObjectList containing the items.
   */
  public ValueObjectList findListByCriteria(String typeName, Vector criteria, String finderName)
      throws Exception, Exception
  {
    return findListByCriteria(typeName, criteria, finderName, false);
  }
  public ValueObjectList findListByCriteria(Connection con, String typeName, Vector criteria, String finderName)
      throws Exception, Exception
  {
    return findListByCriteria(con, typeName, criteria, finderName, false);
  }

  /**
   * Returns a ValueObjectList of objects of type typeName matching the given criteria, using the finder method name
   * given.
   * 
   * @param typeName
   *          The name of the type of object to be found.
   * @param criteria
   *          The criteria to use to find the object.
   * @param finderName
   *          The name of the finderMethod to find the objects with.
   * @param strategyClassName
   *          The name of the class of the persistence strategy for this method
   * @return A ValueObjectList containing the items.
   */
  public ValueObjectList findListByCriteria(String typeName, Vector criteria, String finderName, String strategyClassName)
      throws Exception, Exception
  {
    return findListByCriteria(typeName, criteria, finderName, false, CacheTimeManager.MEDIUM, strategyClassName);
  }

  /**
   * Returns a ValueObjectList of objects of type typeName matching the given criteria, using the finder method name
   * given.
   * 
   * @param typeName
   *          The name of the type of object to be found.
   * @param criteria
   *          The criteria to use to find the object.
   * @param finderName
   *          The name of the finderMethod to find the objects with.
   * @param useCache
   *          Whether or not to look for/store the list in the Cache.
   * @return A ValueObjectList containing the items.
   */
  public ValueObjectList findListByCriteria(String typeName, Vector criteria, String finderName, boolean useCache)
      throws Exception, Exception
  {
    return findListByCriteria(typeName, criteria, finderName, useCache, CacheTimeManager.MEDIUM);
  }
  public ValueObjectList findListByCriteria(Connection con, String typeName, Vector criteria, String finderName, boolean useCache)
      throws Exception, Exception
  {
    return findListByCriteria(con, typeName, criteria, finderName, useCache, CacheTimeManager.MEDIUM);
  }

  /**
   * overloaded method call takes Class param instead of String
   */
  public ValueObjectList findListByCriteria(Class classType, Vector criteria, String finderName, boolean useCache)
      throws Exception, Exception
  {
    return findListByCriteria(classType.getName(), criteria, finderName, useCache, CacheTimeManager.MEDIUM);
  }

  /**
   * Returns a ValueObjectList of objects of type typeName matching the given criteria, using the finder method name
   * given. If specified, it will check the cache first.
   * 
   * @param typeName
   *          The name of the type of object to be found.
   * @param criteria
   *          The criteria to use to find the object.
   * @param finderName
   *          The name of the finderMethod to find the objects with.
   * @param useCache
   *          Whether or not to look for/store the list in the Cache.
   * @param scope
   *          Which caching scope (REQUEST, SHORT, MEDIUM ... )
   * @return A ValueObjectList containing the items.
   */
  public ValueObjectList findListByCriteria(String typeName, Vector criteria, String finderName, boolean useCache,
      byte scope) throws Exception, Exception
  {
    return findListByCriteria(typeName, criteria, finderName, useCache, scope, null);
  }
  public ValueObjectList findListByCriteria(Connection con, String typeName, Vector criteria, String finderName, boolean useCache,
      byte scope) throws Exception, Exception
  {
    return findListByCriteria(con, typeName, criteria, finderName, useCache, scope, null);
  }
  
  
  /**
   * Returns a ValueObjectList of objects of type typeName matching the given criteria, using the finder method name
   * given. If specified, it will check the cache first.
   * 
   * @param typeName
   *          The name of the type of object to be found.
   * @param criteria
   *          The criteria to use to find the object.
   * @param finderName
   *          The name of the finderMethod to find the objects with.
   * @param useCache
   *          Whether or not to look for/store the list in the Cache.
   * @param scope
   *          Which caching scope (REQUEST, SHORT, MEDIUM ... )
   * @param strategyClassName
   *          What persistence strategy to use to retrieve the list, stored procedure, xml, hybrid, etc.           
   * @return A ValueObjectList containing the items.
   */
  public ValueObjectList findListByCriteria(String typeName, Vector criteria, String finderName, boolean useCache,
      byte scope, String strategyClassName) throws Exception, Exception
  {
    return (findListByCriteria(null, typeName, criteria, finderName, useCache, scope, strategyClassName));
  }
  public ValueObjectList findListByCriteria(Connection con, String typeName, Vector criteria, String finderName, boolean useCache,
      byte scope, String strategyClassName) throws Exception, Exception
  {
    ValueObjectList list = null;
    CacheKey key = null;

    if (useCache)
    {
      key = new CacheKey(ValueObjectList.class, new Object[] { typeName, finderName, criteria }, scope);
      list = (ValueObjectList) CacheManager.getInstance().get(key);
    }

    if (list == null)
    {
      ValueObjectConfiguration config = getObjectConfiguration(typeName);
      GenericPersistenceStrategy strategy = null;
      if (strategyClassName == null)
      {
        strategy = ValueObject.getDefaultStrategy();
      }
      else
      {
        strategy = createStrategy(strategyClassName);
      }

      list = strategy.loadList(con, config, criteria, finderName);

      if (useCache)
      {
        key.setGenerator(ValueObjectListGenerator.getInstance());
        list.setCacheLongevity(key.getScope());
        CacheManager.getInstance().put(key, list);
      }
    }

    if (list != null && !list.isEmpty())
    {
      for( Object o : list )
      {
        ValueObject vo = (ValueObject)o;
      
        if ( vo != null && vo.getConfiguration() != null )
        {
          ArrayList<String> validators = vo.getConfiguration().getValidatorList();
          for (String validatorName : validators)
          {
            BasePersistenceValidator validator = BasePersistenceValidator.getValidator(validatorName);
            if (validator != null && !validator.validate(vo))
            {
              Exception pve = new Exception("Persistence Security Violation: " + vo.getClass().getName()
                  + " using " + finderName);
              pve.printStackTrace();
              
              throw pve;
            }
          }
        }
      }
    }
    
    return list;
  }
}
