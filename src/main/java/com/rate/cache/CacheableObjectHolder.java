package com.rate.cache;

public class CacheableObjectHolder
{
  /**
   * 
   * @uml.property name="key"
   * @uml.associationEnd inverse="cacheableObjectHolder:com.scholarone.cache.CacheKey" multiplicity="(1 1)"
   * 
   */
  private CacheKey key;

  /**
   * 
   * @uml.property name="object"
   * @uml.associationEnd inverse="cacheableObjectHolder:java.lang.Object" multiplicity="(1 1)"
   * 
   */
  private Object object;

  protected CacheableObjectHolder()
  {
  }

  public CacheableObjectHolder(Object it)
  {
    if ((it instanceof CacheableObject) && (((CacheableObject) it).isCacheable()))
    {
      CacheableObject co = (CacheableObject) it;

      key = co.getPrimaryKey();

      if (key != null)
      {
        CacheManager.getInstance().put(co);
      }
    }

    object = it;
  }

  public Object getObject() throws Exception, Exception
  {
    // if no key try to get the primary key. When the primary key is based on
    // the id it is not available until the object is saved
    if (key == null)
    {
      if ((object != null) && (object instanceof CacheableObject) && (((CacheableObject) object).isCacheable()))
      {
        CacheableObject co = (CacheableObject) object;

        key = co.getPrimaryKey();

        if (key != null)
        {
          CacheManager.getInstance().put(co);
        }
      }
    }
    else
    {
      // if the key is set, always get this object from cache
      object = CacheManager.getInstance().get(key);

      if ((object == null) && (key.getGenerator() != null))
      {
        object = key.getGenerator().regenerateObject(key.getObjectClass(), key.getCriteria());
      }
    }

    return object;
  }

  public CacheKey getKey()
  {
    return key;
  }
}