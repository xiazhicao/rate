package com.rate.context;

import java.util.Hashtable;

import com.rate.configuration.CFactory;

public class DataSource implements IDataSource
{
  private Hashtable<String, String> dataSourceMap = new Hashtable<String, String>();

  private static DataSource instance = null;

  /**
   * DataSource is a Singleton
   */
  public synchronized static DataSource getInstance()
  {
    if (instance == null)
    {
      instance = new DataSource();      
    }

    return instance;
  }

  public String getDataSource()
  {
    String rv = null;
    Integer stackId = 0;
    String txName="";
    
    ICallContext callContext = ThreadLocalContext.get();
    if ( callContext != null )
    {
      // if we pass in a value in the getTxDataSourceName field,
      // use it and get out, else, parse as normal
      txName=callContext.getTxDataSourceName();
      if(txName != null && txName.length() > 0)
      {
        return txName;
      }
     
      if ( callContext.getStackId() != null )
      {
        stackId = callContext.getStackId();
      }
    }

    // Check map before checking the properties file
    if ( dataSourceMap.containsKey(stackId.toString()) )
    {
      rv = dataSourceMap.get(stackId.toString());
    }
    else 
    {
      if ( stackId.intValue() == 0 )
        rv =  CFactory.instance().getProperty("persistence.datasource.name");
     
      else
        rv =  CFactory.instance().getProperty("persistence.datasource.name." + stackId.toString());
      
      dataSourceMap.put(stackId.toString(), rv);
    }
    
    return rv;
  }

  public void setDataSource(String dataSource)
  {
    Integer stackId = 0;
    
    ICallContext callContext = ThreadLocalContext.get();
    if ( callContext != null )
    {
      if ( callContext.getStackId() != null )
        stackId = callContext.getStackId();
    }
    
    dataSourceMap.put(stackId.toString(), dataSource);
  }

  public void clear()
  {
    dataSourceMap.clear(); 
  }
}
