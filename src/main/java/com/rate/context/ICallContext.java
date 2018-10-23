package com.rate.context;

public interface ICallContext
{
  public String getGuid();

  public void setGuid(String guid);

  public String getApiKey();

  public void setApiKey(String apiKey);

  public Integer getUserId();

  public void setUserId(Integer userId);

  public String getVersionNo();

  public void setVersionNo(String versionNo);

  public String getExternalId();

  public void setExternalId(String externalId);

  public String getAuditText();

  public void setAuditText(String auditText);

  public Integer getStackId();

  public void setStackId(Integer stackId);

  public Exception getException();

  public void setException(Exception e);

  public Integer getConfigId();

  public void setConfigId(Integer configId);

  public Integer getLocaleId();

  public void setLocaleId(Integer localeId);

  public String getSiteName();

  public void setSiteName(String siteName);

  public Integer getPersonId();

  public void setPersonId(Integer personId);

  public String getTxDataSourceName();

  public void setTxDataSourceName(String txName);

  public Object getContextAttribute(String key);

  public void setContextAttribute(String key, Object value);
  
  public void removeContextAttribute(String key);

}