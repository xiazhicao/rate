package com.rate.persistence.valueobject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import com.rate.cache.CacheManager;
import com.rate.persistence.ValueObjectFactory;
import com.rate.persistence.helpers.Attribute;
import com.rate.persistence.helpers.FinderMethod;
import com.rate.persistence.helpers.SaveMethod;
import com.rate.persistence.helpers.StoredProcedure;
import com.rate.persistence.helpers.XMLContent;

public class ValueObjectConfiguration implements Serializable
{
  private static final long serialVersionUID = 1L;

  private ArrayList attributeNameList;

  private HashMap attributeMap;

  private HashMap attributeColMap;

  private HashMap finderMethodMap;

  private HashMap saveMethodMap;
  
  private ArrayList distributionList;
  
  private ArrayList<String> validatorList;
  
  private transient FileStorageMap currentFsMap;

  /** The current stored-procedure. Used when reading config. Only non-null while parsing a <storedProcedure>element. */
  private transient StoredProcedure currentSP;

  /** The current finder method. Used when reading config. Only non-null while parsing a <finderMethod>element. */
  private transient FinderMethod currentFinder;

  /** The current save method. Used when reading config. Only non-null while parsing a <saveMethod>element. */
  private transient SaveMethod currentSave;

  private Class voType;

  private String voTypeName;

  private boolean useCache;

  private byte cacheScope;

  private Boolean cacheRefreshOnUse = Boolean.TRUE;
  
  private boolean useHybridStrategy = false;

  public ValueObjectConfiguration(Class objClass)
  {
    attributeNameList = new ArrayList();
    attributeMap = new HashMap();
    attributeColMap = new HashMap();
    finderMethodMap = new HashMap();
    saveMethodMap = new HashMap();
    setDistributionList(new ArrayList());
    setValidatorList(new ArrayList<String>());
    
    // initialize defaults
    useCache = false;
    cacheScope = 0;
    useHybridStrategy = false;

    setType(objClass);
  }

  public void setType(Class type)
  {
    voType = type;
  }

  public Class getType()
  {
    return voType;
  }

  public void setTypeName(String name)
  {
    voTypeName = name;
  }

  public String getTypeName()
  {
    return voTypeName;
  }

  public boolean useCache()
  {
    return useCache;
  }

  public void setUseCache(Boolean use)
  {
    useCache = use.booleanValue();
  }

  public byte getCacheScope()
  {
    return cacheScope;
  }

  private void setCacheScope(Integer scope)
  {
    if (scope == null)
      cacheScope = 0;
    else
      cacheScope = scope.byteValue();
  }

  public void setCacheTime(String time)
  {
    int timeValue = CacheManager.convertLongevity(time);

    if (timeValue >= 0)
    {
      setCacheScope(Integer.valueOf(timeValue));
    }
  }

  public void setCacheRefreshOnUse(Boolean refresh)
  {
    cacheRefreshOnUse = refresh;
  }

  public Boolean getCacheRefreshOnUse()
  {
    return cacheRefreshOnUse;
  }

  public Attribute getAttribute(String name)
  {
    return (Attribute) attributeMap.get(name);
  }

  public Attribute getAttributeByColumn(String columnName)
  {
    return (Attribute) attributeColMap.get(columnName);
  }

  public Object[] getAttributeNames()
  {
    return attributeNameList.toArray();
  }

  /**
   * sets the attribute with realAttribute, format and name values
   * @param attribute
   * @param format
   * @param attributeName
   */
  public Attribute setAttribute(Attribute attribute, String format)
  {
    String name = format+"_"+attribute.getName();
    Attribute attr = new Attribute(attribute, format, name);
    attributeMap.put(name, attr);

    if (attributeNameList.indexOf(name) < 0)
    {
      attributeNameList.add(name);
    }
    return attr;
  }
      
  public void setAttribute(String name, String type, String columnName, boolean collection, String persistence)
  {
    setAttribute(name, type, columnName, collection, persistence, null, false, 0);
  }
  
  public void setAttribute(String name, String type, String columnName, boolean collection, String persistence,
      String parentKey)
  {
    setAttribute(name, type, columnName, collection, persistence, parentKey, false, 0);
  }

  public void setAttribute(String name, String type, String columnName, boolean collection, String persistence,
      String parentKey, boolean resource, int size)
  {
    Attribute attr = new Attribute(voType, name, type, columnName, collection, persistence, parentKey, resource, size);

    attributeMap.put(name, attr);
    attributeColMap.put(columnName, attr);

    if (attributeNameList.indexOf(name) < 0)
    {
      attributeNameList.add(name);
    }
  }  

  public FinderMethod getFinderMethod(String name) throws Exception
  {
    if (!finderMethodMap.containsKey(name))
    {
      throw new Exception("Cannot find method named: " + name);
    }

    return (FinderMethod) finderMethodMap.get(name);
  }

  public SaveMethod getSaveMethod(String name) throws Exception
  {
    if (!saveMethodMap.containsKey(name))
    {
      throw new Exception("Cannot find method named: " + name + " for type " + getType());
    }

    return (SaveMethod) saveMethodMap.get(name);
  }

  public StoredProcedure getCurrentStoredProcedure()
  {
    return currentSP;
  }
  
  /**
   * Sets the current stored procedure to a new stored-procedure with the given name. If (name == null), it sets the
   * current stored procedure to null.
   * 
   */
  public void setCurrentStoredProcedure(String name)
  {
    if (name != null)
    {
      currentSP = new StoredProcedure(name);
    }
    else
    {
      currentSP = null;
    }
  }

  public Attribute addStoredProcedureArg(String name)
  {
    Attribute arg = getAttribute(name);

    currentSP.addArgument(arg);

    return arg;
  }

  public void addStoredProcedureResult(String name)
  {
    Attribute arg = getAttribute(name);

    currentSP.addResult(arg);

    // now we need to find the configuration for the child object
    // we'll be returning.
    // **note** This means we have cascading XMLConfigurations being
    // read. Could be dangerous if files are mal-formed or circularly dependent.
    // But they shouldn't be anyway. Also need to get Config's into a cache.
    try
    {
      ValueObjectConfiguration conf;

      conf = ValueObjectFactory.getInstance().getObjectConfiguration(arg.getTypeName());

      currentSP.addResultConfig(conf);
    }
    catch (Exception e)
    {
    }

  }

  public void setCurrentFinderMethod(String name)
  {
    if (name != null)
    {
      currentFinder = new FinderMethod();

      currentFinder.setMethodName(name);
      finderMethodMap.put(name, currentFinder);
    }
    else
    {
      currentFinder = null;
    }
  }

  public void setCurrentSaveMethod(String name)
  {
    if (name != null)
    {
      currentSave = new SaveMethod();

      currentSave.setMethodName(name);
      saveMethodMap.put(name, currentSave);
    }
    else
    {
      currentSave = null;
    }
  }

  public void setCurrentFsMap(boolean beginFsMap)
  {
    if (beginFsMap)
    {
      this.currentFsMap = new FileStorageMap();
      this.currentFsMap.fieldMappings = new ArrayList();
      this.getDistributionList().add(currentFsMap);
    }
    else
    {
      this.currentFsMap = null;
    }
     
  }
  
  public void setObjectType(String objectType)
  {
    this.currentFsMap.objectType = objectType;
  }
  
  public void setFileLocation(String fileLocation)
  {
    this.currentFsMap.fileLocation = fileLocation;
  }
  
  
  public void setFileExtension(String fileExtension)
  {
    this.currentFsMap.fileExtension = fileExtension;
  }
  
  public void setPrimaryKeyType(String primaryKeyType)
  {
    this.currentFsMap.primaryKeyType = primaryKeyType;
  }
  
  public void setGetPrimaryKeyMethodName(String getPrimaryKeyMethodName)
  {
    this.currentFsMap.getPrimaryKeyMethodName = getPrimaryKeyMethodName;
  }
  
  public void setSetPrimaryKeyMethodName(String setPrimaryKeyMethodName)
  {
    this.currentFsMap.setPrimaryKeyMethodName = setPrimaryKeyMethodName;
  }
  
  public void setDatetimeAddedType(String datetimeAddedType)
  {
    this.currentFsMap.datetimeAddedType = datetimeAddedType;
  }
  
  public void setGetDatetimeAddedMethodName(String getDatetimeAddedMethodName)
  {
    this.currentFsMap.getDatetimeAddedMethodName = getDatetimeAddedMethodName;
  }
  
  public void setSetDatetimeAddedMethodName(String setDatetimeAddedMethodName)
  {
    this.currentFsMap.setDatetimeAddedMethodName = setDatetimeAddedMethodName;
  }
  
  public void setFieldMapping(FieldMappingMap fieldMapping)
  {
    this.currentFsMap.fieldMappings.add(fieldMapping);
  }
  
  public void setFinderProcedure()
  {
    currentFinder.setProcedure(currentSP);
  }

  public void setFinderXMLContent(XMLContent xml)
  {
    currentFinder.setXMLContent(xml);
  }

  public void setSaveProcedure()
  {
    currentSave.setProcedure(currentSP);
  }

  public void setSaveXMLContent(XMLContent xml)
  {
    currentSave.setXMLContent(xml);
  }

  public void setSaveSingleLine(String singleLine)
  {
    currentSave.setSingleLine(singleLine);
  }

  public void setSaveDoResourcing(String doResourcing)
  {
    currentSave.setDoResourcing(doResourcing);
  }
  
  /**
   * @param useHybridStrategy the useHybridStrategy to set
   */
  public void setUseHybridStrategy(Boolean useHybridStrategy)
  {
    this.useHybridStrategy = useHybridStrategy.booleanValue();
  }

  /**
   * @return the useHybridStrategy
   */
  public Boolean isUseHybridStrategy()
  {
    return Boolean.valueOf(useHybridStrategy);
  }

  /**
   * @param distributionList the distributionList to set
   */
  public void setDistributionList(ArrayList distributionList)
  {
    this.distributionList = distributionList;
  }

  /**
   * @return the distributionList
   */
  public ArrayList getDistributionList()
  {
    return distributionList;
  }


  /**
   * @param distributionList the distributionList to set
   */
  public void setValidatorList(ArrayList<String> validatorList)
  {
    this.validatorList = validatorList;
  }

  /**
   * @return the distributionList
   */
  public ArrayList<String> getValidatorList()
  {
    if ( validatorList == null ) validatorList = new ArrayList<String>();
    return validatorList;
  }
}

class FileStorageMap
{
  
  public String objectType;
  
  public String fileLocation;
  
  public String fileExtension;
  
  public String primaryKeyType;
  
  public String getPrimaryKeyMethodName;
  
  public String setPrimaryKeyMethodName;
  
  public String datetimeAddedType;
  
  public String getDatetimeAddedMethodName;
  
  public String setDatetimeAddedMethodName;

  public ArrayList fieldMappings;
}

class FieldMappingMap
{
  
  public String elementName;
  
  public String type;
  
  public String getMethodName;
  
  public String setMethodName;
  
  public String store_in_xml;
  
  public String store_in_db;
  
  public String read_from;
  
  
  
}
