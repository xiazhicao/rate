package com.rate.cache;

import java.io.Serializable;

public class CacheKey implements Serializable
{

  // 070314-000085
  static final long serialVersionUID = 7040868976681048896L;

  /** The class of the object held by this. */
  private Class objectClass;

  /**
   * The criteria used to identify the object.
   * 
   * @uml.property name="criteria"
   * @uml.associationEnd inverse="cacheKey:com.scholarone.cache.CacheCriteria" multiplicity="(1 1)"
   * 
   */
  private CacheCriteria criteria;

  /** A generator for regenerating this object. */
  private transient CacheableObject.ObjectGenerator generator;

  /**
   * Value of hashcode. Lazy-loaded, with "0" indicating uncalculated. In unlikely event final hashcode actually is "0",
   * it'll need to be recalculated on every call.
   */
  private int hashValue = 0;

  private byte scope = CacheTimeManager.SHORT;

  /**
   * Default constructor. Only used for Serialization.
   */
  protected CacheKey()
  {
  }

  /**
   * Typical constructor. Stores a reference to the class and the criteria.
   * 
   * @param objectClass
   *          The class of the object.
   * @param criteria
   *          The criteria used to identfy the object.
   */
  public CacheKey(Class objectClass, CacheCriteria criteria)
  {
    this(objectClass, criteria, null);
  }

  /**
   * Full constructor. Stores a reference to the class and the criteria, and also initializes the generator.
   * 
   * @param objectClass
   *          The class of the object.
   * @param criteria
   *          The criteria used to identfy the object.
   * @param generator
   *          The gnerator to be used for reconstructing the object, if necessary.
   */
  public CacheKey(Class objectClass, CacheCriteria criteria, CacheableObject.ObjectGenerator generator)
  {
    this.objectClass = objectClass;
    this.criteria = criteria;
    this.generator = generator;
  }

  /**
   * Constructs a CacheKey using a specific class and object for criteria. If value is not a CacheCriteria, then a new
   * CacheCriteria is created with "value" as its value, and that is used for the criteria.
   * 
   * @param objectClass
   *          The class of the object.
   * @param value
   *          The values to use to identify the object.
   */
  public CacheKey(Class objectClass, Object value)
  {
    this.objectClass = objectClass;
    this.criteria = (value instanceof CacheCriteria) ? ((CacheCriteria) value) : (new CacheCriteria(value));
    this.generator = null;
  }

  /**
   * Constructs a CacheKey using a specific class and object for criteria. If value is not a CacheCriteria, then a new
   * CacheCriteria is created with "value" as its value, and that is used for the criteria.
   * 
   * @param objectClass
   *          The class of the object.
   * @param value
   *          The values to use to identify the object.
   * @param scope
   *          The caching scope
   */
  public CacheKey(Class objectClass, Object value, byte scope)
  {
    this.objectClass = objectClass;
    this.criteria = (value instanceof CacheCriteria) ? ((CacheCriteria) value) : (new CacheCriteria(value));
    this.generator = null;
    this.scope = scope;
  }

  /**
   * Returns the class of the object this key identifies.
   */
  public Class getObjectClass()
  {
    return objectClass;
  }

  /**
   * Returns the generator for this key.
   * 
   * @return The generator assigned to this key.
   */
  protected CacheableObject.ObjectGenerator getGenerator()
  {
    return generator;
  }

  /**
   * Returns the criteria used by this key to identify the object.
   * 
   * @return The criteria assigned to this key.
   */
  public CacheCriteria getCriteria()
  {
    return criteria;
  }

  /**
   * Sets the generator to the specified value.
   */
  public void setGenerator(CacheableObject.ObjectGenerator generator)
  {
    this.generator = generator;
  }

  public String toString()
  {
    StringBuffer s = new StringBuffer();
    s.append(getObjectClass() + "@" + criteria.toString());

    return s.toString();
  }

  /**
   * Returns a hashCode that is consistent with equals. Value is cached so it is only ever calcualted once.
   */
  public int hashCode()
  {
    if (hashValue == 0)
    {
      hashValue = calculateHashValue();
    }

    return hashValue;
  }

  /**
   * Calculates the hash-code. For this class, the hash-code is based on the class and the id. If the id is null, it
   * returns the hashcode
   * 
   * Should be overridden by subclasses that have different hash-code values.
   */
  protected int calculateHashValue()
  {
    if (criteria != null)
    {
      return objectClass.hashCode() + (criteria.hashCode() * 31);
    }
    else
    {
      return super.hashCode();
    }
  }

  /**
   * Returns true if the other object is a CacheKey representing the same class and same id. If the id is null, then it
   * returns true only if (other == this).
   */
  public boolean equals(Object other)
  {
    if (this == other)
    {
      return true;
    }

    if ((other != null) && (other.getClass() == this.getClass()))
    {
      CacheKey otherKey = (CacheKey) other;

      return ((this.criteria != null) && (this.objectClass.equals(otherKey.objectClass)) && (this.criteria
          .equals(otherKey.criteria)));
    }

    return false;
  }

  /**
   * @return Returns the scope.
   */
  public byte getScope()
  {
    return scope;
  }

  /**
   * @param scope
   *          The scope to set.
   */
  public void setScope(byte scope)
  {
    this.scope = scope;
  }
}
