package com.rate.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.naming.InitialContext;
import javax.sql.DataSource;

public class ConnectionManager
{
  private static ConnectionManager instance;

  private Hashtable connections = null;

  private InitialContext ctx = null;

  private Hashtable<String, DataSource> dataSourceMap = null;

  /**
   * Constructor for a ConnectionManager
   */
  private ConnectionManager()
  {
    super();

    try
    {
      if (ctx == null) ctx = new InitialContext();
    }
    catch (Exception e)
    {
    }

    if (connections == null) connections = new Hashtable();
    if (dataSourceMap == null) dataSourceMap = new Hashtable<String, DataSource>();
  }

  /**
   * Provides the instance of ConnectionManager. If the instance has yet to be created, it is created now.
   * 
   * @return ConnectionManager
   */
  public static ConnectionManager getInstance()
  {    
    if (instance == null)
    {
      synchronized (ConnectionManager.class)
      {
        if (instance == null)
        {
          instance = new ConnectionManager();
        }
      }
    }

    return (instance);
  }

  /**
   * Gets a pooled database connection for the caller based on the sourceName; If there is already a connection for the
   * current transaction context, it is returned, otherwise, a new connection is retrieved from the pool with the
   * JNDI_NAME that matched sourceName.
   * 
   * @param sourceName
   * @return Connection
   */
  public Connection openConnection(String sourceName)
  {
    return (openConnection(true, sourceName));
  }

  public Connection openConnection(boolean reuseTXconn, String sourceName)
  {
    Connection connection = null;
    DataSource source = null;

    try
    {
      if (reuseTXconn)
      {
        connection = getCurrentTxConnection();
      }
      if (connection == null)
      {
        source = (DataSource) dataSourceMap.get(sourceName);
        if (source != null)
        {
          try
          {
            connection = source.getConnection();
          }
          catch (Throwable t)
          {
            dataSourceMap.remove(sourceName);
          }
        }

        if (connection == null)
        {
          synchronized (ctx)
          {
            source = (DataSource) dataSourceMap.get(sourceName);
            if (source == null)
            {
              source = (DataSource) ctx.lookup("java:" + sourceName);
              if (source != null)
              {
                connection = source.getConnection();
                dataSourceMap.put(sourceName, source);
              }
            }
            else
            {
              connection = source.getConnection();
            }
          }
        }

        if (connection == null)
        {
          return null;
        }

        try
        {
          TransactionHandle txHandle = TransactionContext.getCurrentHandle();
          // if there is a transaction handle this is the first call
          // to openConnection since the beginContext and this is
          // the connection that will be used for the transaction
          if ((txHandle != null) && (reuseTXconn))
          {
            setCurrentTxConnection(connection);
          }
          // otherwise there is no transaction so set autocommit on
          else
          {
            connection.setAutoCommit(true);
          }

          // tracks this connection with a connectionInfo object
          ConnectionInfo connectionInfo = new ConnectionInfo(connection);

          // System.out.println("*** ADD -> " + connectionInfo.id);
          connections.put(connectionInfo.id, connectionInfo);
        }
        catch (Exception e)
        {
          throw new Error(e.toString());
        }
      }
    }
    catch (Exception e)
    {
      throw new Error(e.toString());
    }

    return connection;
  }

  /**
   * Puts the connection back in the pool. If this connection is being closed under the scope of a nested transaction it
   * is not returned to the pool. The connection can only be return to the pool from the main transaction scope.
   * 
   * @param connection
   * @return boolean indicating whether the connection was put back in the pool
   */
  public boolean closeConnection(Connection connection)
  {
    boolean closed = false;

    if (connection != null)
    {
      if (connection != getCurrentTxConnection())
      {
        try
        {
          // Check auto-commit first, to avoid creating masses of useless/expensive
          // exceptions - only attempt rollback when false.
          if (!connection.getAutoCommit())
          {
            connection.rollback();
          }
        }
        catch (SQLException se)
        {
        }

        // System.out.println("*** REMOVE -> " + connection.toString());
        connections.remove(connection.toString());

        try
        {
          connection.setAutoCommit(true);
          connection.close();
        }
        catch (SQLException se)
        {
        }

        closed = true;
      }
    }

    return closed;
  }

  /**
   * Get the connection for the current transaction scope
   * 
   * @return Connection
   */
  protected Connection getCurrentTxConnection()
  {
    Connection connection = null;

    TransactionHandle txHandle = TransactionContext.getCurrentHandle();
    if (txHandle != null) connection = txHandle.getConnection();

    return connection;
  }

  /**
   * Set the connection supplied as the connection for the current transaction scope.
   * 
   * @param connection
   * @throws SQLException
   */
  protected void setCurrentTxConnection(Connection connection) throws SQLException
  {
    if (connection == null) return;

    TransactionContext.setConnection(connection);
  }

  /**
   * Return a list of open connections represented by ConnectionInfo objects
   * 
   * @return List
   */
  public List getOpenConnections()
  {
    ArrayList openConnections = new ArrayList();

    for (Enumeration e = connections.elements(); e.hasMoreElements();)
    {
      openConnections.add(e.nextElement());
    }

    return openConnections;
  }
}
