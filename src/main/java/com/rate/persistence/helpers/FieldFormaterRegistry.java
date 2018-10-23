package com.rate.persistence.helpers;

public class FieldFormaterRegistry
{
  private static FieldFormaterRegistry fieldFormaterRegistry = null;

  private FieldFormater fieldFormater = null;

  /**
   * 
   */
  private FieldFormaterRegistry()
  {
    fieldFormater = new StringFormaterDefault();
  }

  public static FieldFormaterRegistry getInstance()
  {
    if (fieldFormaterRegistry == null) fieldFormaterRegistry = new FieldFormaterRegistry();
    return fieldFormaterRegistry;
  }

  /**
   * This method will any anyone to register an instance of FieldFormater
   * 
   * @param fieldFormater
   */
  public void registerStringFormatter(FieldFormater fieldFormater)
  {
    this.fieldFormater = fieldFormater;
  }

  /**
   * This method returns the registered FieldFormater. If not found it returns the default FieldFormater
   * 
   * @return
   */
  public FieldFormater getFieldFormater()
  {
    if (fieldFormater == null)
      return new StringFormaterDefault();
    else
      return fieldFormater;
  }
}
