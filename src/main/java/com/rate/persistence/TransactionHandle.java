package com.rate.persistence;

import java.sql.Connection;
import java.sql.SQLException;

public class TransactionHandle
{
  private int handle = 0;

  private Connection connection = null;

  public TransactionHandle(int handle)
  {
    this.handle = handle;
  }

  /**
   * @return Returns the connection.
   */
  public synchronized Connection getConnection()
  {
    return connection;
  }

  /**
   * @param connection
   *          The connection to set.
   */
  public synchronized void setConnection(Connection connection)
  {
    this.connection = connection;
  }

  public void rollback()
  {
    // only rollback if it is the main transaction
    if (handle > 0 || connection == null) return;

    try
    {
      connection.rollback();
    }
    catch (SQLException se)
    {
      throw new Error(se.toString());
    }
    finally
    {
      ConnectionManager connMgr = ConnectionManager.getInstance();
      connMgr.closeConnection(connection);
    }
  }

  public void commit()
  {
    // only commit if it is the main transaction
    if (handle > 0 || connection == null) return;

    try
    {
      connection.commit();
    }
    catch (SQLException se)
    {
      throw new Error(se.toString());
    }
    finally
    {
      ConnectionManager connMgr = ConnectionManager.getInstance();
      connMgr.closeConnection(connection);
    }
  }
}
