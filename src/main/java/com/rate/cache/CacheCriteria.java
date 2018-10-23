package com.rate.cache;

import java.io.Serializable;

public class CacheCriteria implements Serializable
{
  /**
   * Default value to use for uncalculated hash-codes. In the unusual circumstance that an objects actual hash-value is
   * this value, then it will need to be recalculated each time hashCode() is called.
   */
  private static final int UNCALCULATED = -1;

  /** The criteria to compare. */
  private Object[] criteria;

  /**
   * The pre-calculated value for hash-code. Is not serialized, but must be recalculated after serialization.
   */
  protected transient int hashValue = UNCALCULATED;

  /**
   * Default constructor. Creates a criteria with no values.
   */
  public CacheCriteria()
  {
  }

  /**
   * Creates a criteria with a single-object. If <code>it</code> is an array of objects (or <code>null</code>),
   * then it is stored as the array. Otherwise, a new array containing only <code>it</code> is constructed and stored.
   * 
   * @param it
   *          The critieria to use for this.
   */
  public CacheCriteria(Object it)
  {
    if ((it == null) || (it instanceof Object[]))
    {
      criteria = (Object[]) it;
    }
    else
    {
      criteria = new Object[] { it };
    }
  }

  /**
   * Creates a criteria with the indicated array of values. Actually stores the array, not a copy of it.
   * 
   * @param it
   *          The array of values to use.
   */
  public CacheCriteria(Object[] it)
  {
    criteria = it;
  }

  /**
   * Returns the internally-used array of objects. Actually returns the array, not a copy. Changes made to the array
   * will result in undefined behavior, so that should never be done.
   * 
   * @return The array of objects used for criteria.
   */
  public Object[] getCriteria()
  {
    return criteria;
  }

  /**
   * Returns a hashcode for this. If criteria is not null, value is based on the values in the criteria. If criteria is
   * null, then uses the system hashCode.
   * 
   * For efficiency, this is calculated only once, then cached. Subclasses that wish different behavior should override
   * calculateHashValue().
   */
  public int hashCode()
  {
    if (hashValue == UNCALCULATED)
    {
      hashValue = calculateHashValue();
    }

    return hashValue;
  }

  /**
   * Calculates a hashcode for this. If criteria is not null, value is based on the values in the criteria. If criteria
   * is null, then uses the system hashCode.
   * 
   * @return The value for the hash-code.
   */
  protected int calculateHashValue()
  {
    if (criteria == null)
    {
      return System.identityHashCode(this);
    }
    else
    {
      int value = 0;

      for (int i = 0; i < criteria.length; ++i)
      {
        value = (value * 31) + ((criteria[i] == null) ? (0) : (criteria[i].hashCode()));
      }

      return value;
    }
  }

  /**
   * Returns true if the other is this, or if it is the same class, both objects have non-null criteria, and the entries
   * in the criteria arrays match.
   */
  public boolean equals(Object other)
  {
    if (other == this)
    {
      return true;
    }
    else if ((other == null) || !(other.getClass().equals(this.getClass())) || (other.hashCode() != this.hashCode()))
    {
      return false;
    }
    else
    {
      CacheCriteria o = (CacheCriteria) other;
      boolean equal = (this.criteria != null) && (o.criteria != null) && (this.criteria.length == o.criteria.length);

      for (int i = 0; equal && (i < this.criteria.length); ++i)
      {
        equal = (this.criteria[i] == null) ? (o.criteria[i] == null) : (this.criteria[i].equals(o.criteria[i]));
      }

      return equal;
    }
  }

  public String toString()
  {
    if (criteria == null)
    {
      return "[ No criteria ]";
    }

    StringBuffer s = new StringBuffer();
    String val;

    for (int i = 0; i < criteria.length; ++i)
    {
      if (criteria[i] != null)
      {
        val = criteria[i].toString();
      }
      else
      {
        val = "NULL" + i;
      }

      s.append("[" + val + "]");
    }

    return s.toString();
  }
}

