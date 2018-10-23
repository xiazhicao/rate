package com.rate.persistence.helpers;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;

public class Attribute implements Serializable, Cloneable
{
  private static final long serialVersionUID = 1L;

  /** Static to indicate attribute should always be written */
  public static final int ALWAYS_WRITE = 0;

  /** Static to indicate attribute should never be written */
  public static final int NEVER_WRITE = 1;

  /** Static to indicate attribute should only be written when not null */
  public static final int NOT_NULL = 2;

  /** Static to indicate attribute should only be written when not null or blank */
  public static final int NOT_BLANK = 3;

  public static final int NO = 0;

  public static final int YES = 1;

  private String name;

  private String type;

  private String columnName;

  private boolean collection = false;

  private int persistenceMode;

  private boolean resourceMode = false;
  
  private int size = 0;

  private boolean inOut = false;

  private PropertyDescriptor pd = null;

  private String parentKey = null;

  private Class parentType = null;

  /**
   * a hint for the persistence layer that this value will come out fine as is and so no further value checks will be
   * done on it
   */
  private boolean needsConversionHint = false;

  /**
   * This is the attribute containing the special character
   */
  private Attribute realAttribute;

  /**
   * This is a string the contains the format of the attribute
   */
  private String format;

  public Attribute(Class parentObjType, String name, String type, String columnName, boolean collection,
      String persistence, String parentKey, boolean resource, int size)
  {
    if (parentObjType == null)
    {
      throw new IllegalArgumentException("Attribute parent object type cannot be null");
    }

    this.name = name;
    this.type = type;
    this.columnName = columnName;
    this.collection = collection;
    this.persistenceMode = getPersistenceMode(persistence);
    this.parentKey = parentKey;
    this.parentType = parentObjType;
    this.resourceMode = resource;
    this.size = size;
    this.inOut = false;

    setConversionHint();

    if (name.startsWith("dummy_"))
    {
      return;
    }

    try
    {
      pd = new PropertyDescriptor(name, parentObjType);
    }
    catch (IntrospectionException ie)
    {
    }

  }
    
  public Attribute(Class parentObjType, String name, String type, String columnName, boolean collection)
  {
    this(parentObjType, name, type, columnName, collection, null, null, false, 0);
  }

  public Attribute(Class parentObjType, String name, String type, String columnName)
  {
    this(parentObjType, name, type, columnName, false, null, null, false, 0);
  }

  /**
   * this methods add the realAttribute witht eh format and name to the attribute
   * 
   * @param attribute
   * @param format
   * @param attributeName
   */
  public Attribute(Attribute attribute, String format, String attributeName)
  {
    this.realAttribute = attribute;
    this.format = format;
    this.name = attributeName;
  }

  public Object clone()
  {
    try
    {
      return super.clone();
    }
    catch (Exception e)
    {
      return null;
    }
  }

  public String getActualName()
  {
    return name;
  }

  public String getName()
  {
    return realAttribute == null ? name : realAttribute.getName();
  }

  public void setName(String str)
  {
    if (realAttribute == null)
      name = str;
    else
      realAttribute.setName(str);
  }

  public String getTypeName()
  {
    return realAttribute == null ? type : realAttribute.getTypeName();
  }

  public String getColumnName()
  {
    return realAttribute == null ? columnName : realAttribute.getColumnName();
  }

  public boolean getCollection()
  {
    return realAttribute == null ? collection : realAttribute.getCollection();
  }

  public String getParentKey()
  {
    return realAttribute == null ? parentKey : realAttribute.getParentKey();
  }

  public int getPersistenceMode()
  {
    return realAttribute == null ? persistenceMode : realAttribute.getPersistenceMode();
  }

  public boolean getResourceMode()
  {
    return realAttribute == null ? resourceMode : realAttribute.getResourceMode();
  }

  public int getSize()
  {  
    return realAttribute == null ? size : realAttribute.getSize();
  }
  
  public boolean isInOut()
  {
    return realAttribute == null ? inOut : realAttribute.isInOut();
  }

  public void setInOut(boolean val)
  {
    if (realAttribute == null)
      inOut = val;
    else
      realAttribute.setInOut(val);
  }

  public boolean needsConversion()
  {
    return needsConversionHint;
  }

  public void setNeedsConversion(boolean convert)
  {
    if (realAttribute == null)
      needsConversionHint = convert;
    else
      realAttribute.setNeedsConversion(convert);
  }

  public PropertyDescriptor getPropertyDescriptor()
  {
    if (pd == null)
    {
      try
      {
        if (realAttribute == null)
          pd = new PropertyDescriptor(name, parentType);
        else
          pd = new PropertyDescriptor(realAttribute.getName(), realAttribute.parentType);
      }
      catch (IntrospectionException ie)
      {
      }
    }

    return pd;
  }

  private void setConversionHint()
  {
    // these are the types that require conversion
    if (type.equals("java.lang.Boolean") || type.equals("java.sql.Clob") || type.equals("java.sql.Blob") || type.equals("java.lang.Double"))
    {
      if (realAttribute == null)
        needsConversionHint = true;
      else
        realAttribute.needsConversionHint = true;
    }
  }

  public boolean isWritable(Object value)
  {
    return ((getPersistenceMode() == ALWAYS_WRITE) || ((getPersistenceMode() == NOT_NULL) && (value != null))
        | ((getPersistenceMode() == NOT_BLANK) && (value != null) && (!"".equals(String.valueOf(value)))));
  }

  /**
   * Returns the integer value for persistence mode corresponding to the indicated value. Expects one of "yes", "no",
   * "notnull", or "notblank". Returns ALWAYS_WRITE for unexpected values. Ignores case.
   * 
   * @param persistence
   *          The String value of the mode.
   * @return The integer value for the mode (defaults to ALWAYS_WRITE)
   */
  public static int getPersistenceMode(String persistence)
  {
    int returnValue = ALWAYS_WRITE;

    persistence = (persistence == null) ? ("yes") : (persistence.toLowerCase());

    if (persistence.equals("no"))
    {
      returnValue = NEVER_WRITE;
    }
    else if (persistence.equals("notnull"))
    {
      returnValue = NOT_NULL;
    }
    else if (persistence.equals("notblank"))
    {
      returnValue = NOT_BLANK;
    }

    // else go with default: ALWAYS_WRITE.

    return returnValue;
  }

  /**
   * this is a getter method that returns true if realAttribute is not null added this method for Special Characters FDD
   * 
   * @return
   */
  public boolean isWrapper()
  {
    return realAttribute != null;
  }

  /**
   * getter method for Format
   * 
   * @return
   */
  public String getFormat()
  {
    return format;
  }

  /**
   * setter method for Format
   * 
   * @param format
   */
  public void setFormat(String format)
  {
    if (realAttribute == null)
      this.format = format;
    else
      this.format = realAttribute.getFormat();
  }

  /**
   * gettter method of Real Attribute
   * 
   * @return
   */
  public Attribute getRealAttribute()
  {
    return realAttribute;
  }

  /**
   * setter method for Real Attribute
   * 
   * @param realAttribute
   */
  public void setRealAttribute(Attribute realAttribute)
  {
    this.realAttribute = realAttribute;
  }
}
