package com.rate.persistence.valueobject;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Vector;

import com.rate.persistence.ValueObject;
import com.rate.persistence.helpers.Attribute;

public abstract class GenericPersistenceStrategy implements Serializable
{
  protected boolean storedInCacheFl = true;
  /**
   * This returns a single Object for when we know the criteria being passed in will identify the object uniquely
   */
  public abstract ValueObject loadObject(ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception;

  /**
   * loadList returns a multi-row ResultSet as a ValueObjectList. Right now it is a shallow load: will not read any
   * sub-objects of the objects in the list.
   */
  public abstract ValueObjectList loadList(ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception;
  public abstract ValueObjectList loadList(Connection con, ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception;

  public abstract void save(ValueObject object) throws Exception;

  public abstract void save(ValueObject object, String methodName) throws Exception;

  public abstract Integer copy(ValueObjectConfiguration config, Vector criteria) throws Exception;

  protected abstract Object convertReadObject(Object value, Attribute attr) throws Exception;

  protected abstract Object convertWriteObject(Object value, Attribute attr) throws Exception;

  /**
   * @param isStoredInCacheFl the isStoredInCacheFl to set
   */
  public void setStoredInCacheFl(boolean isStoredInCacheFl)
  {
    this.storedInCacheFl = isStoredInCacheFl;
  }

  /**
   * @return the isStoredInCacheFl
   */
  public boolean isStoredInCacheFl()
  {
    return storedInCacheFl;
  }
}

