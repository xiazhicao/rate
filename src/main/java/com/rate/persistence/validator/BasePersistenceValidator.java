package com.rate.persistence.validator;

import com.rate.persistence.ValueObject;

public abstract class BasePersistenceValidator
{
  public abstract boolean validate(ValueObject object);
    
  public abstract boolean validateSave(ValueObject object);
  
  public abstract boolean validateDelete(ValueObject object);
  
  public static BasePersistenceValidator getValidator(String validatorName)
  {
    BasePersistenceValidator validator = null;
    
    Class<?> xClass;
    try
    {
      xClass = Class.forName(validatorName);
      validator = (BasePersistenceValidator) xClass.getConstructor(new Class[] {}).newInstance();
    }
    catch (Exception e)
    {
    }
    
    return validator;
  }
}
