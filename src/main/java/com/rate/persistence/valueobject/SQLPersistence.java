package com.rate.persistence.valueobject;

import java.beans.PropertyDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.rate.cache.CacheManager;
import com.rate.context.DataSource;
import com.rate.persistence.ConnectionManager;
import com.rate.persistence.ValueObject;
import com.rate.persistence.ValueObjectFactory;
import com.rate.persistence.helpers.Attribute;
import com.rate.persistence.helpers.FieldFormaterRegistry;

public abstract class SQLPersistence extends GenericPersistenceStrategy
{
  protected final static String BOOLEAN_CHAR_REP_TRUE = "1";
  
  protected final static String BOOLEAN_CHAR_REP_FALSE = "0";

  public final static String NULL_STRING = "NULL_STRING";
  
  public final static String NULL_INTEGER = "NULL_INTEGER";
  
  public final static String NULL_BOOLEAN = "NULL_BOOLEAN";
  
  private final static int TYPE_MAP_SIZE = 101;

  private final static String upper = "upper";
  
  private final static String upperASCII = "upperASCII";
  
  private final static String ASCII = "ASCII";
  
  protected final static int RETRY = 1;
  
  // always want a prime number, sufficiently large to avoid collisions

  // I am implementing my own hash table here for performance reasons
  private final transient static int[] class2SQLTypeMap = new int[TYPE_MAP_SIZE];

  private static transient boolean hashInitialized = false;

  private static Hashtable tx2connTable = null;

  private CacheManager cacheManager;
  
  // Would be nice to have a way to determine this max programatically, but haven't found one yet.
  private static final int AUTO_BOX_CACHE_MAX = 10000;

  private static long intsAllocated = 0;

  private static long intsAllocatedZeroTo10K = 0;

  private static long intsAllocated10KTo20K = 0;

  private static long intsAllocated20KTo100K = 0;

  private static long intsAllocatedOver100K = 0;
  
  private static long intsAllocatedCached = 0;
  

  public SQLPersistence()
  {
    if (!hashInitialized)
    {
      initHash();
    }

    cacheManager = CacheManager.getInstance();

    if (tx2connTable == null)
    {
      tx2connTable = new Hashtable();
    }
  }

  private String getDataSourceName()
  {
    return DataSource.getInstance().getDataSource();
  }
  
  private void initHash()
  {
    // initialize with default values
    for (int i = 0; i < TYPE_MAP_SIZE; i++)
    {
      class2SQLTypeMap[i] = Types.NULL;
    }

    // then fill in with the real stuff
    class2SQLTypeMap[Math.abs(new String("java.lang.Integer").hashCode()) % TYPE_MAP_SIZE] = Types.INTEGER;
    class2SQLTypeMap[Math.abs(new String("java.lang.Long").hashCode()) % TYPE_MAP_SIZE] = Types.BIGINT;
    class2SQLTypeMap[Math.abs(new String("java.lang.Boolean").hashCode()) % TYPE_MAP_SIZE] = Types.CHAR;
    class2SQLTypeMap[Math.abs(new String("java.lang.String").hashCode()) % TYPE_MAP_SIZE] = Types.VARCHAR;
    class2SQLTypeMap[Math.abs(new String("java.sql.Timestamp").hashCode()) % TYPE_MAP_SIZE] = Types.TIMESTAMP;
    class2SQLTypeMap[Math.abs(new String("java.sql.Clob").hashCode()) % TYPE_MAP_SIZE] = Types.CLOB;
    class2SQLTypeMap[Math.abs(new String("java.sql.Blob").hashCode()) % TYPE_MAP_SIZE] = Types.BLOB;
    class2SQLTypeMap[Math.abs(new String("java.lang.Double").hashCode()) % TYPE_MAP_SIZE] = Types.DOUBLE;
    class2SQLTypeMap[Math.abs(new String("java.lang.Float").hashCode()) % TYPE_MAP_SIZE] = Types.FLOAT;
    class2SQLTypeMap[Math.abs(new String("java.lang.Short").hashCode()) % TYPE_MAP_SIZE] = Types.SMALLINT;
    class2SQLTypeMap[Math.abs(new String("java.math.BigDecimal").hashCode()) % TYPE_MAP_SIZE] = Types.DECIMAL;

    hashInitialized = true;
  }

  /**
   * Lookup table to convert Java class names into java.sql.Types values. Useful during save operaions when setting NULL
   * parameters.
   */
  public static int javaClass2SQLType(String className)
  {
    return class2SQLTypeMap[Math.abs(className.hashCode()) % TYPE_MAP_SIZE];
  }
  
  public abstract void batchUpdate(ValueObjectList list) throws Exception;
  
  public abstract void batchUpdate(ValueObjectList list, String methodName) throws Exception;
  
  protected void setObjectValue(ValueObject object, String columnName, Object value) throws Exception
  {
    ValueObjectConfiguration config = object.getConfiguration();
    Attribute attr = config.getAttributeByColumn(columnName);

    // if I don't have an attribute that matches this column name
    // then don't try to set it
    if (attr == null)
    {
      String err = "SQLPersistence.setObjectValue- no value found for column name " + columnName;

      err += " Attempting to set for object of type " + object.getClass().getName();

      throw new Exception(err);
    }

    setObjectValue(object, attr, value);
  }

  /**
   * Convert types read from database to types expected by code
   */
  protected Object convertReadObject(Object value, Attribute attr) throws Exception
  {
    // sanity check...sometimes in config, values may be null ie. blobs especially!! GAM 9/13/02
    if (value == null)
    {
      return null;
    }

    // convert CLOBs
    try
    {
      if (value instanceof Clob)
      {
        if (((Clob) value).length() > 0)
        {
          return ((Clob) value).getSubString((long) 1, (int) ((Clob) value).length());
        }
        else
        {
          return new String();
        }
      }
    }
    catch (SQLException se)
    {
      throw new Exception(se.toString());
    }

    // convert Boolean flags
    if ((value instanceof String) && attr.getTypeName().equals("java.lang.Boolean"))
    {
      if (value.equals(BOOLEAN_CHAR_REP_TRUE) || value.equals("Y"))
      {
        return Boolean.TRUE;
      }

      return Boolean.FALSE;
    }

    if ((value instanceof java.math.BigDecimal) && attr.getTypeName().equals("java.lang.Double"))
    {
      return new Double(((java.math.BigDecimal) value).doubleValue());
    }

    // convert Blob
    if (javaClass2SQLType(attr.getTypeName()) == Types.BLOB)
    {
      try
      {
        InputStream is = ((Blob) value).getBinaryStream();
        ObjectInputStream ois = new ObjectInputStream(is);

        return ois.readObject();
      }
      catch (Exception e)
      {
        throw new Exception("Can't read Blob: " + e.toString());
      }
    }

    return value;

  }

  /**
   * override in derived classes to perform type-specific conversions Need the attribute here so we know what type to
   * convert it to.
   * 
   * Converts data in java objects to types expected by database.
   */
  protected Object convertWriteObject(Object value, Attribute attr) throws Exception
  {
    return value;
  }

  // JK: Changed Return list from ValueObjectList to List<ValueObject>.  This is to avoid object being added to the Cache by ValueObjectList.add() method.
  // Adding to cache is managed by caller when entire find/load process is complete (this avoids adding partially loaded objects into the Cache).
  protected List<ValueObject> readResultSet(ResultSet rs, ValueObjectConfiguration config) throws Exception
  {
    return readResultSet(rs, config, null);
  }

  /**
   * Reads the result set into a list of ValueObjects, based on the given configuration. Takes an optional Map which
   * will reference the found objects by their ID in addition to the returned List.
   */
  // JK: Changed Return list from ValueObjectList to List<ValueObject>.  This is to avoid object being added to the Cache by ValueObjectList.add() method.
  // Adding to cache is managed by caller when entire find/load process is complete (this avoids adding partially loaded objects into the Cache).
  protected List<ValueObject> readResultSet(ResultSet rs, ValueObjectConfiguration config, Map objectsByPkMap)
      throws Exception
  {
//    log.debug("Entering SQLPersistence.readResultSet");

    List<ValueObject> results = new ArrayList<ValueObject>();
    ValueObject newOb = null;

    // define these out here, useful debugging when exception thrown
    String colName = "";
    int i = 0;
    String[] colNames = null;
    Object value = null;
    
    try
    {
      ResultSetMetaData rsmd = rs.getMetaData();
      int colCount = rsmd.getColumnCount();
      colNames = new String[colCount + 1]; // over allocate by one to account for the 1-based index in resultset
      Attribute[] attributes = new Attribute[colCount + 1];

      for (i = 1; i <= colCount; i++)
      {
        colNames[i] = rsmd.getColumnName(i);
        attributes[i] = config.getAttributeByColumn(colNames[i]);

        // if I don't have an attribute that matches this column name
        // then don't try to set it
        if (attributes[i] == null)
        {
          String err = "SQLPersistence.readResultSet - no value found for column name " + colNames[i];

          throw new Exception(err);
        }
      }

      ValueObjectFactory vof = ValueObjectFactory.getInstance();
      int err_count = 0;
      
      while (rs.next())
      {
        newOb = vof.createObjectFromConfig(config);

        try 
        {
          // column numbers are 1-based
          for (i = 1; i <= colCount; i++)
          {
//          value = rs.getObject(i);
          value = getObjectFromResultSetSmartMemory(rs, i);
            setObjectValue(newOb, attributes[i], value);
          }
          
          // object is now in sync with database
          if (newOb != null)
          {
            newOb.setIsModified(false);
          }

          results.add(newOb);

          if (objectsByPkMap != null) objectsByPkMap.put(newOb.getId(), newOb);          
        }
        catch(SQLException se)
        {
          err_count++;
          
          if (colNames != null && i >= 1 && i < colNames.length) colName = colNames[i];

          String err = "Error loading object type " + config.getType().getName();

          if ( value != null )
            err += "Column index = " + i + ", Column name = " + colName + ", Last loaded value = " + value + ", " + se.toString();
          else
            err += "Column index = " + i + ", Column name = " + colName + ", " + se.toString();
          
          if ( err_count > 25 )
            throw new Exception("Error reading result set: " + err);
        }
      }
    }
    catch (SQLException se)
    {
      if (colNames != null && i >= 1 && i < colNames.length) colName = colNames[i];

      String err = "Error loading object type " + config.getType().getName();

      if ( value != null )
        err += "Column index = " + i + ", Column name = " + colName + ", Value = " + value + ", " + se.toString();
      else
        err += "Column index = " + i + ", Column name = " + colName + ", " + se.toString();

      throw new Exception("Error reading result set: " + err);
    }
    catch (Exception oce)
    {
      throw new Exception("Error reading result set: " + oce.toString());
    }

    return results;
  }

  /**
   * Gets a connection object from the app server connection pool
   * 
   * @return connection object
   */
  protected Connection openConnection()
  {
    ConnectionManager connMgr = ConnectionManager.getInstance();
    return connMgr.openConnection(getDataSourceName());
  }

  /**
   * Releases a connection back to the connection pool.
   * 
   * @param conn
   *          connection to be returned to the pool.
   */
  public boolean closeConnection(Connection connection)
  {
    ConnectionManager connMgr = ConnectionManager.getInstance();
    return connMgr.closeConnection(connection);
  }

  protected void setStatementInputParameters(PreparedStatement statement, Vector argumentDefs, Vector argumentVals)
      throws SQLException
  {
    for (int i = 0; i < argumentVals.size(); i++)
    {
      Object val = argumentVals.get(i);
      Attribute attr = (Attribute) argumentDefs.get(i);
      int sqlType = javaClass2SQLType(attr.getTypeName());

      try
      {
        if (val == null)
        {
          // set null by type defined in argument list
          statement.setNull(i + 1, sqlType);
        }
        else
        {
          if (sqlType == Types.BLOB)
          {
            // hack because crappy JDBC won't let us create and store a BLOB
            try
            {
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              ObjectOutputStream os = new ObjectOutputStream(baos);

              os.writeObject(val);
              statement.setBytes(i + 1, baos.toByteArray());
            }
            catch (IOException e)
            {
              throw new SQLException("Error initializing BLOB data" + e.toString());
            }
          }
          else
          {
            // if val is a string escape single quotes
            if (val instanceof String)
            {
              ((String) val).replaceAll("'", "''");
            }
            else if (val instanceof Boolean)
            {
              if ( ((Boolean)val).booleanValue() )
              {
                val = BOOLEAN_CHAR_REP_TRUE;
              }
              else
              {
                val = BOOLEAN_CHAR_REP_FALSE;
              }
            }            
            statement.setObject(i + 1, val);
          }
        }
      }
      catch (SQLException se)
      {
        String err = "Error initializing SQL stored procedure call ";

        err += "Last arg index = " + (i + 1) + ", Attribute name = " + attr.getName();

        throw new SQLException(err + se.toString());
      }
    }
  }

  /**
   * Ensures all result sets that belong to this statement are closed, then closes the statement. ALso closes the
   * connection that this was using
   * 
   * @param statement
   *          Statement to be closed.
   */
  public void closeStatement(boolean closeConn, Statement statement)
  {
    // this should never fail and if it does, we're in trouble
    try
    {
      if (statement != null)
      {
        Connection con = statement.getConnection();
        statement.close();
        if (closeConn)
        {
          closeConnection(con);
        }
        statement = null;
      }
    }
    catch (SQLException se)
    {
      System.out.println("**** " + se.getMessage());
    }
  }

  public void cache(ValueObject object)
  {
    // if cacheable
    if (object.getConfiguration().useCache())
    {
      if (object.getDeletedBy() != null)
        cacheManager.flush(object);
      else
        cacheManager.put(object);
    }
  }

  public void doCache(ValueObject object)
  {
    if (object.getDeletedBy() != null)
      cacheManager.flush(object);
    else
      cacheManager.put(object);
  }

  /**
   * This version of the function is more generic as it takes an Attribute parameter as opposed to a column name
   */
  protected void setObjectValue(Object object, Attribute attr, Object value) throws Exception
  {
    try
    {
      PropertyDescriptor pd = attr.getPropertyDescriptor();

      if (pd == null)
      {
        throw new Exception("Unable to get PropertyDescriptor for attribute " + attr.getName()
            + " of object " + object.getClass().getName());
      }

      Method method = pd.getWriteMethod();

      if (attr.needsConversion())
      {
        value = convertReadObject(value, attr);
      }

      Object[] args = { value };

      method.invoke(object, args);
    }
    catch (InvocationTargetException ite)
    {
      String err = "Cannot set value " + attr.getName() + " on object " + object.getClass().getName() + " Reason = ";

      throw new Exception(err + ite.getTargetException().toString(), ite);
    }
    catch (IllegalAccessException iae)
    {
      String err = "Cannot set value " + attr.getName() + " on object " + object.getClass().getName() + " Reason = ";

      throw new Exception(err + iae.toString(), iae);
    }
    catch (IllegalArgumentException iarge)
    {
      String err = "Cannot set value " + attr.getName() + " on object " + object.getClass().getName();

      if (value != null)
      {
        err += " Value type = " + value.getClass().getName() + " Reason = ";
      }
      else
      {
        err += " Value is null. Reason = ";
      }

      System.out.println("SQLPersistence.setObjectValue(): Error - IllegalArgumentException - " + err);

      throw new Exception(err + iarge.toString(), iarge);
    }
  }

  /**
   * This method will call the FieldFormaterRegistry and call the appropreate method to convert the string
   * @param format
   * @return
   */
  private Object getFormatedValue(String format,Object value)
  {
    String stringValue = "";
    try
    {
      stringValue = (String) value;
      if (ASCII.equalsIgnoreCase(format))
        stringValue = FieldFormaterRegistry.getInstance().getFieldFormater().getASCIIValue(stringValue);
      else if (upperASCII.equalsIgnoreCase(format))
        stringValue = FieldFormaterRegistry.getInstance().getFieldFormater().getUpperASCIIValue(stringValue);
      else if (upper.equalsIgnoreCase(format))
        stringValue = FieldFormaterRegistry.getInstance().getFieldFormater().getUpperValue(stringValue);
      value = stringValue;
    }
    catch (ClassCastException cce)
    {
      // We only want to change the Object value if it is a string.
      // If it is not a string we do not make any changes to the object.
    }
    return value;
  }
  
  /**
   * @return Object
   */
  public Object getObjectValue(Object object, Attribute attr) throws Exception
  {
    Object value = null;
    
    if ( object == null || attr == null )
      return value;
    
    try
    {
      PropertyDescriptor pd = attr.getPropertyDescriptor();
      if (pd == null)
      {        
        throw new Exception("Unable to get PropertyDescriptor for attribute " + attr.getName()
            + " of object " + object.getClass().getName());
      }

      Method method = pd.getReadMethod();

      if (!method.getDeclaringClass().isInstance(object))
      {
      }

      Object[] args = {};

      value = method.invoke(object, args);

      value = convertWriteObject(value, attr);
      if(attr.isWrapper())value = getFormatedValue(attr.getFormat(), value);
    }
    catch (InvocationTargetException ite)
    {
      throw new Exception(
          "Cannot get value: " + attr.getName() + ": " + ite.getTargetException().toString(), ite);
    }
    catch (IllegalAccessException iae)
    {
      throw new Exception("Cannot get value: " + attr.getName() + ": " + iae.toString(), iae);
    }

    return value;
  }

  /**
   * Given a list of the expected attributes, read their corresponding values from the ValueObject and add them to the
   * Vector
   */
  protected Vector initializeArguments(Object obj, Vector params) throws Exception
  {
    Vector args = new Vector();

    for (int i = 0; i < params.size(); i++)
    {
      Attribute attr = (Attribute) params.get(i);

      args.add(getObjectValue(obj, attr));
    }

    return args;
  }  
  
  protected Object getObjectFromResultSetSmartMemory(ResultSet rs, int index) throws SQLException
  {
    Object value = rs.getObject(index);
    if (value != null && value instanceof Integer)
    {
      intsAllocated++;
      int val = ((Integer)value).intValue();
      if (val >= 0 && val <= 10000)
      {
        intsAllocatedZeroTo10K++;
      }
      else if (val > 10000 && val <= 20000)
      {
        intsAllocated10KTo20K++;
      }
      else if (val > 20000 && val <= 100000)
      {
        intsAllocated20KTo100K++;
      }
      else if (val > 100000)
      {
        intsAllocatedOver100K++;
      }
      if (val >= -128 && val <= AUTO_BOX_CACHE_MAX)
      {
        intsAllocatedCached++;
        return Integer.valueOf(val);
      }
    }
    return value;
  }
}
