package com.rate.persistence.valueobject;

import java.util.Vector;
import java.util.logging.Logger;

import com.rate.cache.CacheCriteria;
import com.rate.cache.CacheableObject;
import com.rate.persistence.ValueObjectFactory;

public class ValueObjectListGenerator implements CacheableObject.ObjectGenerator
{

  private static ValueObjectListGenerator instance;

  protected Logger log;

  protected ValueObjectListGenerator()
  {
  }

  public static ValueObjectListGenerator getInstance()
  {
    if (instance == null)
    {
      instance = new ValueObjectListGenerator();
    }

    return instance;
  }

  public CacheableObject regenerateObject(Class objectClass, CacheCriteria criteria)
  {
    ValueObjectList list = null;

    if (objectClass == ValueObjectList.class)
    {
      Object[] inputArray = criteria.getCriteria();

      if ((inputArray.length >= 3) && (inputArray[0] instanceof String) && (inputArray[1] instanceof String)
          && (inputArray[2] instanceof Vector))
      {
        String typeName = (String) inputArray[0];
        String method = (String) inputArray[1];
        Vector inputs = (Vector) inputArray[2];

        try
        {
          ValueObjectFactory factory = ValueObjectFactory
              .getInstance();

          list = factory.findListByCriteria(typeName, inputs, method, false);
        }
        catch (Exception e)
        {
          // report the error
        }
      }
    }

    return (CacheableObject) list;
  }
}
