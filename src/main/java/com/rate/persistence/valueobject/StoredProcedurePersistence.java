package com.rate.persistence.valueobject;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.rate.configuration.CFactory;
import com.rate.context.DataSource;
import com.rate.persistence.ConnectionManager;
import com.rate.persistence.ValueObject;
import com.rate.persistence.helpers.Attribute;
import com.rate.persistence.helpers.FinderMethod;
import com.rate.persistence.helpers.SaveMethod;
import com.rate.persistence.helpers.StoredProcedure;

public class StoredProcedurePersistence extends SQLPersistence
{
  private static ValueObject lastFailedObject = null;

  private static String DRIVER_FLAVOR_DB2_LEGACY_8_2_DRIVER_VERSION_STRING_PREFIX = "08.02";
  private static String DRIVER_FLAVOR_DB2_LEGACY_8_2_DRIVER_NAME = "IBM DB2 JDBC 2.0 Type 2";

  public StoredProcedurePersistence()
  {
  }

  /**
   * This returns a single Object for when we know the criteria being passed in will identify the object uniquely
   */
  public ValueObject loadObject(ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception
  {
    return (loadObject(null, config, keys, methodName));
  }

  public ValueObject loadObject(Connection con, ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception
  {      
    String msg = "StoredProcedurePersistence.loadObject(";
    msg += config.getTypeName() + ", " + methodName + ")";
    FinderMethod finder = config.getFinderMethod(methodName);
    StoredProcedure sp = finder.getStoredProcedure();
    ValueObject newOb = null;
    ValueObject parentOb = null;
    CallableStatement statement = null;

    
    long startTime = System.currentTimeMillis();

    ValueObjectConfiguration childConf = config;

    try
    {
      String sqlStatement = createStatement(sp);

      statement = initializeStatement(con, sp, sqlStatement, keys);

      boolean hasResultSet = statement.execute();
      displaySQLWarnings(statement);
           
      boolean moreResults = true;
      int i = 0;

      List<ValueObject> cacheList = new ArrayList<ValueObject>();
      
      while (hasResultSet && moreResults)
      {
        ResultSet rs = statement.getResultSet();

        // we're iterating through the ResultSets
        // each subsequent Set will be a child object of the parent object
        if (i > 0)
        {
          Vector<ValueObjectConfiguration> resultConfigs = sp.getResultConfigs();
          // In case the database has returned more resultsets then the config is aware
          if ( resultConfigs.size() > i )
          {
            childConf = resultConfigs.get(i);
          }
          else
          {
            moreResults = statement.getMoreResults();
            continue;
          }
        }

        List<ValueObject> list = readResultSet(rs, childConf);

        // if we get more than a single row here throw up an exception
        // because we were expecting a unique value
        // if (list.size() > 1)
        if (list.size() > 0)
        {
          newOb = list.get(0);
        }
        else
        {
          newOb = null;
        }

        // save the parent object
        if (i == 0)
        {
          parentOb = newOb;
          
          if (parentOb == null)
          {
            // no base object matching our criteria was returned:
            // get out of the processing loop without worrying about any other resultSets
            break;
          }
          
          if ( childConf.useCache() )
          {
            cacheList.add(parentOb);
          }
        }
        else
        {
          // assign the child object to where it belongs in the parent
          Attribute attr = (Attribute) sp.getResults().get(i);
          String colName = attr.getColumnName();
          boolean isCollection = attr.getCollection();

          if (parentOb != null)
          {
            if (isCollection)
            {
              // convert List<ValueObject> to ValueObjectList
              ValueObjectList voList = new ValueObjectList();
              voList.addAll(list);
              setObjectValue(parentOb, colName, voList);
              if ( childConf.useCache() )
              {
                cacheList.addAll(list);
              }
            }
            else if (newOb != null)
            {
              setObjectValue(parentOb, colName, newOb);
              if ( childConf.useCache() )
              {
                cacheList.add(newOb);
              }
            }
          }
        }

        moreResults = statement.getMoreResults();

        i++;
      }

      // Place appropriate objects into cache
      for ( ValueObject nextObject : cacheList )
      {
        doCache(nextObject);
      }
    }
    catch (SQLException se)
    {
      StringBuffer spText = new StringBuffer();
      spText.append(sp.getName() + "(");

      for (int i = 0; i < keys.size(); i++)
      {
        Object obj = keys.get(i);

        spText.append(obj);

        if (i + 1 < keys.size())
        {
          spText.append(",");
        }
      }

      spText.append(")");
      
      StringBuffer s = new StringBuffer();
      if ( se.getSQLState() != null && se.getSQLState().equalsIgnoreCase("42815") && se.getErrorCode() == -4461 )
        s.append("TYPE 4 driver issue - ");
      s.append("Error in StoredProcedurePersistence.loadObject - " + spText.toString());
      
    }
    finally
    {
      closeStatement((con == null), statement);
    }

    long duration = System.currentTimeMillis() - startTime;
    displayProcedureTiming(sp.getName(), duration);
    
    // object is in sync with DB
    if (parentOb != null)
    {
      parentOb.setIsModified(false);
    }

    return parentOb;
  }

  /**
   * version of the function which is optimized for a single result set. less memory and cpu-intensive if we don't need
   * to track parent-child relations
   */
  private ValueObjectList loadListShallow(ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception
  {
    return (loadListShallow(null, config, keys, methodName));
  }
  
  private ValueObjectList loadListShallow(Connection con, ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception
  {
    FinderMethod finder = config.getFinderMethod(methodName);
    StoredProcedure sp = finder.getStoredProcedure();
    CallableStatement statement = null;
    List<ValueObject> list = null;
    ValueObjectList results = null;

    try
    {
      String sqlStatement = createStatement(sp);

      statement = initializeStatement(con, sp, sqlStatement, keys);

      boolean hasResultSet = statement.execute();
      displaySQLWarnings(statement);

      if (hasResultSet)
      {
        ResultSet rs = statement.getResultSet();
        list = readResultSet(rs, config);
      }
    }
    catch (SQLException se)
    {
      String errString = getSPErrString(se, sp, keys);
      
      StringBuffer s = new StringBuffer();
      if ( se.getSQLState() != null && se.getSQLState().equalsIgnoreCase("42815") && se.getErrorCode() == -4461 )
        s.append("TYPE 4 driver issue - ");
      s.append("Error in StoredProcedurePersistence.loadList - " + errString);
      
    }
    finally
    {
      closeStatement((con == null), statement);
    }

    results = new ValueObjectList();
    if (list != null)
    {
      // convert List<ValueObject> to ValueObjectList (we do this last to avoid caching of parent objects before they are fully assembled).
      results.addAll(list);
    }

    return results;
  }

  /**
   * implementation for returning nested lists of valueobjects each child list much contain the primary key of its
   * parent object in its result set. Can be n levels deep, as long as stored procedure returns the result sets.
   */
  private ValueObjectList loadListDeep(ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception
  {
    return (loadListDeep(null, config, keys, methodName));
  }
  
  private ValueObjectList loadListDeep(Connection con, ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception
  {
    FinderMethod finder = config.getFinderMethod(methodName);
    StoredProcedure sp = finder.getStoredProcedure();
    ValueObjectConfiguration configChild = null;
    ValueObjectConfiguration childConf = config;
    CallableStatement statement = null;
    ValueObjectList results = null;
    ValueObject parentOb = null;
    Attribute attrChild = null;
    String parentKey = null;
    String parentMapKey = null;
    Object children = null;
    Integer parentId = null;
    Attribute attr = null;
    List<ValueObject> parentList = null;

    int j = 0;
    int i = 0;

    try
    {
      String sqlStatement = createStatement(sp);

      statement = initializeStatement(con, sp, sqlStatement, keys);

      boolean hasResultSet = statement.execute();
      displaySQLWarnings(statement);
      
      boolean moreResults = true;

      // this holds all the idMaps from all the result sets
      // so child result sets can find their parents
      HashMap parentMapMap = new HashMap();
      HashMap<ValueObject, Map> childrenMap = new HashMap<ValueObject, Map>();
      List<ValueObject> cacheList = new ArrayList<ValueObject>();
      
      while (hasResultSet && moreResults)
      {
        ResultSet rs = statement.getResultSet();

        // we're iterating through the ResultSets
        // each subsequent Set will be a child object of the parent object
        if (i > 0)
        {
          Vector<ValueObjectConfiguration> resultConfigs = sp.getResultConfigs();
          if ( resultConfigs.size() > i )
          {
            childConf = resultConfigs.get(i);
          }
          else
          {
            moreResults = statement.getMoreResults();
            continue;
          }
        }
                
        HashMap idMap = new HashMap();
        addParentIdMap(parentMapMap, sp.getResults().get(i), idMap);

        List<ValueObject> list = readResultSet(rs, childConf, idMap);
        
        if ( childConf.useCache() )
        {
          cacheList.addAll(list);
        }
        
        if (i == 0)
        {
          parentList = list;
          // Initialize all children
          // Call initChildren then fetch each child list into a local map.
          // If child list is still null after initChildren, create a empty list
          for ( ValueObject parentObject : parentList )
            parentObject.initChildren();
        }

        if ((i > 0) && (list != null) && (parentList != null))
        {
          // assign the child object to where it belongs in the parent
          attr = (Attribute) sp.getResults().get(i);
          parentKey = attr.getParentKey();
          parentMapKey = attr.getName();

          HashMap parentIdMap = findParentIdMap(parentMapMap, parentMapKey);
          
          for (ValueObject newOb : list) // For each child...
          {
            configChild = newOb.getConfiguration();
            attrChild = configChild.getAttributeByColumn(parentKey.toUpperCase());
            parentId = (Integer) getObjectValue(newOb, attrChild);
            if ( parentId == null )
            {
            }
            else
            {
              parentOb = (ValueObject) parentIdMap.get(parentId);
  
              Map collectionMap = childrenMap.get(parentOb);
              if ( collectionMap == null )
              {
                collectionMap = new HashMap<String, Object>();
                childrenMap.put(parentOb, collectionMap);
              }
              
              if (attr.getCollection())
              {
                children = collectionMap.get(attr.getName());
                if ( children == null )
                {
                  children = getObjectValue(parentOb, attr);
                  collectionMap.put(attr.getName(), children);
                }
                        
                // JK: Warning.  This adds child object to cache (if child object class is cacheable)
                ((ValueObjectList) children).add(newOb);
              }
              else
              {
                collectionMap.put(attr.getName(), newOb);
              }
            }
          }
        }

        moreResults = statement.getMoreResults();

        i++;
      }

      // Set the child lists to the correct setter methods of the parent object. 
      if ( childrenMap != null )        
      {
        Iterator itParent = childrenMap.keySet().iterator();
        while ( itParent.hasNext() )
        {
          ValueObject parent = (ValueObject) itParent.next();
          Map collectionMap = childrenMap.get(parent);
          Iterator itAttr = collectionMap.keySet().iterator();
          while ( itAttr.hasNext() )
          {
            String attribName = (String) itAttr.next();          
            attr = parent.getConfiguration().getAttribute(attribName);
            children = collectionMap.get(attribName);
            if ( children != null && attr != null )
            {
              setObjectValue(parent, attr, children);
            }
          }
          parent.setIsModified(false);
        }
      }
    }
    catch (SQLException se)
    {
      String errString = getSPErrString(se, sp, keys);
      
      StringBuffer s = new StringBuffer();
      if ( se.getSQLState() != null && se.getSQLState().equalsIgnoreCase("42815") && se.getErrorCode() == -4461 )
        s.append("TYPE 4 driver issue - ");
      s.append("Error in StoredProcedurePersistence.loadList - " + errString);
      
    }
    finally
    {
      closeStatement((con == null), statement);
    }

    results = new ValueObjectList();
    if (parentList != null)
    {
      // convert parent list from List<ValueObject> to ValueObjectList (waiting to construct ValueObjectList avoids adding partially assembled parent objects into the cache)
      results.addAll(parentList);
    }

    return results;
  }

  public ValueObjectList loadList(ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception
  {
    return (loadList(null, config, keys, methodName));
  }
  
  public ValueObjectList loadList(Connection con, ValueObjectConfiguration config, Vector keys, String methodName)
      throws Exception
  {
    String msg = "StoredProcedurePersistence.loadList(";
    msg += config.getTypeName() + ", " + methodName + ")";

    FinderMethod finder = config.getFinderMethod(methodName);
    StoredProcedure sp = finder.getStoredProcedure();

    ValueObjectList results = null;
    long startTime = System.currentTimeMillis();

    // call appropriate implementation based on num of result sets
    if (sp.getResultConfigs().size() == 1)
      results = loadListShallow(con, config, keys, methodName);
    else
      results = loadListDeep(con, config, keys, methodName);

    long duration = System.currentTimeMillis() - startTime;
    displayProcedureTiming(sp.getName(), duration);

    return results;
  }

  public Integer copy(ValueObjectConfiguration config, Vector keys) throws Exception
  {
    return (copy(null, config, keys));
  }
  
  public Integer copy(Connection con, ValueObjectConfiguration config, Vector keys) throws Exception
  {
    FinderMethod finder = config.getFinderMethod("copy");
    StoredProcedure sp = finder.getStoredProcedure();
    Integer object = null;

    CallableStatement statement = null;

    long startTime = System.currentTimeMillis();

    try
    {
      String sqlStatement = createStatement(sp);

      statement = initializeStatement(con, sp, sqlStatement, keys);

      Vector params = sp.getInOutArguments();

      for (int i = 0; i < params.size(); i++)
      {
        Attribute attr = (Attribute) params.get(i);
        int sqlType = javaClass2SQLType(attr.getTypeName());
        statement.registerOutParameter(i + 1, sqlType);
      }

      // execute the statement
      executeUpdate(statement);
      displaySQLWarnings(statement);

      // set any values returned from an INOUT parameter
      for (int i = 0; i < params.size(); i++)
      {
        Attribute attr = (Attribute) params.get(i);
        object = (Integer) statement.getObject(i + 1);
      }
    }
    catch (SQLException se)
    {
      String errString = getSPErrString(se, sp, keys);
      
      StringBuffer s = new StringBuffer();
      if ( se.getSQLState() != null && se.getSQLState().equalsIgnoreCase("42815") && se.getErrorCode() == -4461 )
        s.append("TYPE 4 driver issue - ");
      s.append("Error in StoredProcedurePersistence.copy - " + errString);
      
    }
    finally
    {
      closeStatement((con == null), statement);
    }

    long duration = System.currentTimeMillis() - startTime;
    displayProcedureTiming(sp.getName(), duration);

    return object;
  }

  /**
   * Calls the default procedure, named "save"
   */
  public void save(ValueObject object) throws Exception
  {
    this.save(object, ValueObject.DEFAULT_SAVE);
  }

  /**
   * Saves the object using the stored procedure associated with the indicated method. If the object is not modified, it
   * does nothing.
   * 
   * @param object
   *          The object to save.
   * @param methodName
   *          The name of the method to use for saving the object.
   */
  public void save(ValueObject object, String methodName) throws Exception
  {
    save(null, object, methodName);
  }
  
  public void save(Connection con, ValueObject object, String methodName) throws Exception
  {
    lastFailedObject = null;

    if (!object.isModified())
    {
      return;
    }

    SaveMethod saver = object.getConfiguration().getSaveMethod(methodName);
    StoredProcedure sp = saver.getStoredProcedure();

    CallableStatement statement = null;

    long startTime = System.currentTimeMillis();

    try
    {
      String sqlStatement = createStatement(sp);
      Vector args = this.initializeArguments(object, sp.getArguments());

      statement = initializeStatement(con, sp, sqlStatement, args);

      // register any out parameters, declared as INOUT in XML
      Vector params = sp.getInOutArguments();

      for (int i = 0; i < params.size(); i++)
      {
        Attribute attr = (Attribute) params.get(i);
        int index = sp.getArguments().indexOf(attr);
        int sqlType = javaClass2SQLType(attr.getTypeName());
        statement.registerOutParameter(index + 1, sqlType);
      }

      // execute the statement
      executeUpdate(statement);
      displaySQLWarnings(statement);

      // set any values returned from an INOUT parameter
      for (int i = 0; i < params.size(); i++)
      {
        Attribute attr = (Attribute) params.get(i);
        int index = sp.getArguments().indexOf(attr);
        setObjectValue(object, attr.getColumnName(), statement.getObject(index + 1));
      }

      // object is now in sync with database
      object.setIsModified(false);
      object.setIsDeleted(false);
      object.setAddedBy(null);
      object.setModifiedBy(null);
      object.setDeletedBy(null);
      cache(object);
    }
    catch (SQLException se)
    {
      if ( !se.getSQLState().equalsIgnoreCase("75001") )
      {
        StringBuffer spText= new StringBuffer();
        spText.append(sp.getName() + "(");
  
        for (int i = 0; i < sp.getArguments().size(); i++)
        {
          Attribute attr = (Attribute) sp.getArguments().get(i);
  
          spText.append(getObjectValue(object, attr));
  
          if (i + 1 < sp.getArguments().size())
          {
            spText.append(",");
          }
        }
  
        spText.append(")");
        
        StringBuffer s = new StringBuffer();
        if ( se.getSQLState() != null && se.getSQLState().equalsIgnoreCase("42815") && se.getErrorCode() == -4461 )
          s.append("TYPE 4 driver issue - ");
        s.append("Error in StoredProcedurePersistence.save - " + spText.toString());
        
      }
      
    }
    finally
    {
      // close connections
      closeStatement((con == null), statement);
    }

    long duration = System.currentTimeMillis() - startTime;
    displayProcedureTiming(sp.getName(), duration);
  }

  public void batchUpdate(ValueObjectList list) throws Exception
  {
    batchUpdate(list, "save");
  }

  /**
   * Used to save a list of objects in one DB call. Uses the JDBC batchUpdate call to make repeated calls to the
   * contained object's save stored proc.
   * 
   * @param list
   * @param containedClassName
   * @param methodName
   * @throws Exception
   */
  public void batchUpdate(ValueObjectList list, String methodName) throws Exception
  {
    // first get the object configuration
    ValueObjectConfiguration voc = null;
    StoredProcedure sp = null;
    SaveMethod saver = null;
    CallableStatement statement = null;
    String sqlStatement = null;

    try
    {
      Iterator it = list.iterator();
      while (it.hasNext())
      {
        ValueObject object = (ValueObject) it.next();
        if (!object.isModified()) continue;

        // this set up only gets called the first time
        if (voc == null)
        {
          voc = object.getConfiguration();
          saver = voc.getSaveMethod(methodName);
          sp = saver.getStoredProcedure();
          sqlStatement = createStatement(sp);
          statement = initBatchStatement(sp, sqlStatement);
        }

        // add a new set of arguments for each element
        Vector args = this.initializeArguments(object, sp.getArguments());
        setStatementInputParameters(statement, sp.getArguments(), args);
        statement.addBatch();
      }

      // execute the statement
      statement.executeBatch();
      displaySQLWarnings(statement);

      // set any values returned from an INOUT parameter
      // for (int i = 0; i < params.size(); i++)
      // {
      // Attribute attr = (Attribute)params.get(i);
      //
      // if (attr.isInOut())
      // {
      // setObjectValue(object, attr.getColumnName(), statement.getObject(i + 1));
      // }
      // }

      // object is now in sync with database
      // object.setIsModified(false);
      // cache(object);

    }
    catch (SQLException se)
    {
      StringBuffer s = new StringBuffer();
      if ( se.getSQLState() != null && se.getSQLState().equalsIgnoreCase("42815") && se.getErrorCode() == -4461 )
        s.append("TYPE 4 driver issue - ");
      s.append("Error in batchUpdate");
      
    }
    finally
    {
      // close connections
      closeStatement(true, statement);
    }
  }

  protected void executeUpdate(CallableStatement statement) throws SQLException
  {
    boolean success = false;
    int retry_count = 0;

    while(!success) 
    {
      try
      {
        statement.executeUpdate();
        displaySQLWarnings(statement);
        
        success = true;
      }
      catch(SQLException se)
      {
        StringBuffer s = new StringBuffer();
        if ( se.getSQLState() != null && se.getSQLState().equalsIgnoreCase("42815") && se.getErrorCode() == -4461 )
          s.append("TYPE 4 driver issue - ");
        s.append("SQLException during an update. SQLSTATE [" + se.getSQLState() + "]");
        
        // Deadlock or Timeout
        if (se.getSQLState().equalsIgnoreCase("40001"))
        {          
          retry_count++;
          if ( retry_count > RETRY ) throw se;
          System.out.println("Retry #" + retry_count + " - " + statement.toString());
        }
        else
        {   
          throw se;
        }
      }
    }
  }
  
  /**
   * Initializes a statement from a list of query criteria
   */
  protected CallableStatement initializeStatement(Connection con, StoredProcedure sp, String statementStr, Vector arguments)
      throws SQLException
  {
    CallableStatement statement = null;
    Connection connection = con;

    try
    {
      if (connection == null)
      {
        connection = openConnection();
      }
      statement = connection.prepareCall(statementStr);
  
      setStatementInputParameters(statement, sp.getArguments(), arguments);
    }
    catch(SQLException e)
    {
      if ( statement != null ) statement.close();
      if ( con == null &&  connection != null ) connection.close();
      
      throw e;
    }

    return statement;
  }

  /**
   * Initializes a statement from a list of query criteria
   */
  protected CallableStatement initBatchStatement(StoredProcedure sp, String statementStr) throws SQLException
  {
    CallableStatement statement = null;
    Connection connection = null;

    connection = openConnection();
    statement = connection.prepareCall(statementStr);

    return statement;
  }

  private String createStatement(StoredProcedure sp)
  {
    StringBuffer buf = new StringBuffer();

    buf.append("{CALL " + sp.getName() + "(");

    int numArgs = sp.getArguments().size();
    int i;

    for (i = 0; i < numArgs - 1; i++)
    {
      buf.append("?,");
    }

    if (numArgs > 0)
    {
      buf.append('?');
    }

    buf.append(")}");

    return buf.toString();
  }

  private void addParentIdMap(HashMap mapMap, Object keyAttr, HashMap map)
  {
    String keyName = "";
    if (!(keyAttr instanceof Attribute))
      keyName = "root";
    else
      keyName = ((Attribute) keyAttr).getName();

    mapMap.put(keyName, map);
  }

  private HashMap findParentIdMap(HashMap mapMap, String key)
  {
    // to locate the parent list for this key, we need to drop
    // the last namespace off of it.
    // ex. "abstract.authors" -> "abstract"
    // "abstract" -> "root"
    String realKey = "root"; // most likely case
    int delimIndex = key.lastIndexOf('.');
    if (delimIndex > 0) realKey = key.substring(0, delimIndex);

    return (HashMap) mapMap.get(realKey);
  }

  private String getSPErrString(SQLException se, StoredProcedure sp, Vector keys)
  {
    StringBuffer spText = new StringBuffer();
    Object obj;

    spText.append(sp.getName() + "(");

    for (int l = 0; l < keys.size(); l++)
    {
      obj = keys.get(l);
      spText.append(obj);

      if (l + 1 < keys.size())
      {
        spText.append(",");
      }
    }

    spText.append(")");
    return spText.toString();
  }

  public static ValueObject getLastFailed()
  {
    return lastFailedObject;
  }

  // Defect 11882
  // #NOTE : "Pass through" functionality avoiding our persistence layer
  public static Connection getPTConnection(boolean reuseTXconn)
  {
   /*
   * vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
   * WARNING - SHOULDN'T YOU REALLY BE USING THE S1CommonUtility CLASS ?!?
   * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
   */
   // #NOTE : Caller is responsible for calling ValueObjectFactory.closeConnection(con)
    Connection retVal = null;

    String dataSourceName = DataSource.getInstance().getDataSource();
    retVal = ConnectionManager.getInstance().openConnection(reuseTXconn, dataSourceName);

    return (retVal);
  }
  
  public static boolean closePTConnection(Connection _con)
  {
  /*
   * vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
   * WARNING - SHOULDN'T YOU REALLY BE USING THE S1CommonUtility CLASS ?!?
   * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
   */
    boolean retVal = false;

    retVal = ConnectionManager.getInstance().closeConnection(_con);

    return (retVal);
  }
  
  public static ResultSet execStoredProc(Connection _con, String _proc, List<Object> _params) throws SQLException
  {
    /*
     * vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
     * WARNING - SHOULDN'T YOU REALLY BE USING THE S1CommonUtility CLASS ?!?
     * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     */
    DataBaseConnectionInfo dataBaseConnectionInfo = executeStoredProcWithoutClosingDBHandles(_con, _proc, _params);
    return dataBaseConnectionInfo.getResultSet();
  }
  
  /**
   * This method executes a sproc and return resultset and statement.
   * ResultSet and Statement objects must be handled/closed in the calling class.
   * @param _con
   * @param _proc
   * @param _params
   * @return
   * @throws SQLException
   */
  public static DataBaseConnectionInfo executeStoredProcWithoutClosingDBHandles(Connection _con, String _proc, List<Object> _params) throws SQLException
  {
    DataBaseConnectionInfo dataBaseConnectionInfo = new DataBaseConnectionInfo();
    ResultSet retVal = null;

    StringBuilder procCall = new StringBuilder("{call ");
    procCall.append(_proc);
    procCall.append("(");
    int numParams = 0;
    if (_params != null)
    {
      numParams = _params.size();
      for (int i = 0; i < numParams; i++)
      {
        if (i > 0)
        {
          procCall.append(",");
        }
        procCall.append("?");
      }
    }
    procCall.append(")}");
    CallableStatement sp = _con.prepareCall(procCall.toString());

    Object thisParam = null;
    if (_params != null)
    {
      for (int i = 0; i < numParams; i++)
      {
        thisParam = _params.get(i);
        if (NULL_STRING.equals(thisParam))
        {
          sp.setNull(i+1, Types.VARCHAR);
          sp.registerOutParameter(i+1, Types.VARCHAR);
        }
        else if (NULL_INTEGER.equals(thisParam))
        {
          sp.setNull(i+1, Types.INTEGER);
          sp.registerOutParameter(i+1, Types.INTEGER);
        }
        else if (NULL_BOOLEAN.equals(thisParam))
        {
          sp.setNull(i+1, Types.CHAR);
          sp.registerOutParameter(i+1, Types.CHAR);
        }
        else if (thisParam instanceof String)
        {
          sp.setString(i+1, (String)thisParam);
          sp.registerOutParameter(i+1, Types.VARCHAR);
        }
        else if (thisParam instanceof Integer)
        {
          sp.setInt(i+1, (Integer)thisParam);
          sp.registerOutParameter(i+1, Types.INTEGER);
        }
        else if (thisParam instanceof Boolean)
        {
          String charValue = ((Boolean) thisParam) ? BOOLEAN_CHAR_REP_TRUE : BOOLEAN_CHAR_REP_FALSE;
          sp.setString(i+1, charValue);
          sp.registerOutParameter(i+1, Types.CHAR);
        }
      }
    }

    if (sp.execute())
    {
      displaySQLWarnings(sp);
      retVal = sp.getResultSet();
    }
    
    // copy any output parameters back to the param list that the caller passed in
    if (_params != null)
    {
      // JK: Hack alert.  Different DB2 drivers behave differently - especially w.r.t. handling out parameters.
      // Legacy 8.2/Type 2 driver:
      //   -  silently allows code to attempt to retrieve an out parameter even if it really is not an out parameter in the SP (not sure what 
      //      value it sets, but no one cares since we can't be expecting a real value since it really is not an out parameter).
      //   - throws an exception when calling CallableStatement.getParameterMetaData() AND ALSO DESTROYS/CLOSES CONNECTION.  So... DO NOT
      //     call CallableStatement.getParameterMetaData() if driver is legacy 8.2 driver.
      // Universal Driver (DB2 v 9.7+)/Type 2:
      //   -  silently allows code to attempt to retrieve for an out parameter even if it really is not an out parameter in the SP (not sure what 
      //      value it sets, but no one cares since we can't be expecting a real value since it really is not an out parameter).
      //   - throws an exception (not implemented) if you call CallableStatement.getParameterMetaData() (can be used to determine which params are actually our params).
      // Universal Driver (DB2 v 9.7+)/Type 4:
      //   - throws an exception if you ask for an out parameter that is not declared in SP
      //   - supports use of CallableStatement.getParameterMetaData().
      boolean isLegacy82Driver = isDB2Legacy82Driver(_con);
      
      ParameterMetaData paramMetaData = null;

      if (!isLegacy82Driver)
      {
        try
        {
          paramMetaData = sp.getParameterMetaData();
        }
        catch (SQLException sqle)
        {
          paramMetaData = null;
        }
      }
      for (int i = 0; i < numParams; i++)
      {
        thisParam = _params.get(i);
        int mode = -1;
        if (paramMetaData != null)
        {
          mode = paramMetaData.getParameterMode(i+1);
        }
        
        if (paramMetaData == null || mode == ParameterMetaData.parameterModeInOut || mode == ParameterMetaData.parameterModeOut)
        {
          if (thisParam instanceof Integer || NULL_INTEGER.equals(thisParam))
          {
            _params.set(i, sp.getInt(i + 1));
          }
          else if (thisParam instanceof Boolean || NULL_BOOLEAN.equals(thisParam))
          {
            String charValue = sp.getString(i + 1);
            _params.set(i, BOOLEAN_CHAR_REP_TRUE.equals(charValue));
          }
          else if (thisParam instanceof String)
          {
            _params.set(i, sp.getString(i + 1));
          }
        }
      }
    }
    dataBaseConnectionInfo.setResultSet(retVal);
    dataBaseConnectionInfo.setCallableStatement(sp);
    return dataBaseConnectionInfo;
  }
  
  private static void displayProcedureTiming(String spName, long duration)
  {
    String msg = spName + "[" + DataSource.getInstance().getDataSource() + "]: " + duration + " ms";
    
    // check property to see if we should output to system out
    String printProcTimings = CFactory.instance().getProperty("persistence.logging.proctimings.tosystemout");
    if (printProcTimings != null && printProcTimings.equals("Y"))
    {
      System.out.println(msg);
    }
  }
  
  private static void displaySQLWarnings(Statement statement) throws SQLException
  {
    SQLWarning sqlwarn = statement.getWarnings();     
    while (sqlwarn != null) 
    {         
      StringBuffer s = new StringBuffer();
      if ( sqlwarn.getSQLState() != null && sqlwarn.getSQLState().equalsIgnoreCase("0100E") && sqlwarn.getErrorCode() == 464)
      {
        s.append("TYPE 4 driver issue - ");
      }
      s.append("Message: " + sqlwarn.getMessage());
      
      // SELECT INTO, UPDATE or FETCH found no rows - Suppressing the log
      if ( sqlwarn.getSQLState() != null && sqlwarn.getSQLState().equalsIgnoreCase("02000") )
        break;
      
      sqlwarn=sqlwarn.getNextWarning();
    }
  }

  private static boolean isDB2Legacy82Driver(Connection conn)
  {
    DatabaseMetaData dbmd = null;
    try
    {
      dbmd = conn.getMetaData();
    }
    catch(Throwable t)
    {
      return false;
    }

    if (dbmd != null)
    {
      String driverName = null;
      String driverVersionString = null;
      try
      {
        driverName = dbmd.getDriverName();
      }
      catch(Throwable t)
      {}
      try
      {
        driverVersionString = dbmd.getDriverVersion();
      }
      catch(Throwable t)
      {}

      // Identify Legacy/8.2/Type 2 by:
      // 1) driver name = "IBM DB2 JDBC 2.0 Type 2"
      // AND 2) driver version begins with "08.02"
      if (driverName != null && driverVersionString != null 
          && driverVersionString.trim().startsWith(DRIVER_FLAVOR_DB2_LEGACY_8_2_DRIVER_VERSION_STRING_PREFIX) 
          && driverName.trim().equals(DRIVER_FLAVOR_DB2_LEGACY_8_2_DRIVER_NAME))
        return true;
    }
    return false;
  }
}
