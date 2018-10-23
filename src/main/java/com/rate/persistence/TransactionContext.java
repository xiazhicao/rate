package com.rate.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;

public class TransactionContext
{
  private int timeout = 0;

  private static Hashtable txMap;

  public TransactionContext()
  {
    this(300);
  }

  /**
   * Constructor takes a Transaction timeout value in seconds
   */
  public TransactionContext(int timeoutInSeconds)
  {
    timeout = timeoutInSeconds;

    synchronized (TransactionContext.class)
    {
      if (txMap == null) txMap = new Hashtable();

    }
  }

  public void clear()
  {
    Stack txStack = null;
    synchronized (txMap)
    {
      txStack = (Stack) txMap.get(Thread.currentThread());
    }
    if (txStack != null && !txStack.isEmpty())
      handleException(new Exception("Thread " + Thread.currentThread().getName() + " had open connections"));
  }

  public static void setConnection(Connection connection) throws SQLException
  {
    TransactionHandle txHandle = null;

    if (connection == null) return;

    connection.setAutoCommit(false);

    if (txMap != null)
    {
      Stack txStack = null;
      synchronized (txMap)
      {
        txStack = (Stack) txMap.get(Thread.currentThread());
      }
      if (txStack != null && !txStack.isEmpty())
      {
        Iterator it = txStack.iterator();
        while (it.hasNext())
        {
          txHandle = (TransactionHandle) it.next();
          txHandle.setConnection(connection);
        }
      }
    }
  }

  public void beginContext()
  {
    Stack txStack = null;
    synchronized (txMap)
    {
      txStack = (Stack) txMap.get(Thread.currentThread());
    }

    if (txStack == null)
    {
      txStack = new Stack();
      synchronized (txMap)
      {
        txMap.put(Thread.currentThread(), txStack);
      }
    }

    TransactionHandle txHandle = new TransactionHandle(txStack.size());

    if (!txStack.isEmpty())
    {
      TransactionHandle txPreviousHandle = (TransactionHandle) txStack.peek();
      txHandle.setConnection(txPreviousHandle.getConnection());
    }

    txStack.push(txHandle);
  }

  public void endContext()
  {
    Stack txStack = null;
    synchronized (txMap)
    {
      txStack = (Stack) txMap.get(Thread.currentThread());
    }

    if (txStack != null && !txStack.isEmpty())
    {
      TransactionHandle txHandle = (TransactionHandle) txStack.pop();

      if (txHandle == null) return;

      txHandle.commit();
    }
  }

  /**
   * Rolls back the transaction
   */
  public void handleException(Exception e)
  {
    Stack txStack = null;
    synchronized (txMap)
    {
      txStack = (Stack) txMap.get(Thread.currentThread());
    }

    if ( txStack != null )
    {
      while (!txStack.isEmpty())
      {
        TransactionHandle txHandle = (TransactionHandle) txStack.pop();
        txHandle.rollback();
      }
    }

  }
  /**
   * Get the transaction handle for the current thread
   */
  public static TransactionHandle getCurrentHandle()
  {
    TransactionHandle txHandle = null;

    if (txMap != null)
    {
      Stack txStack = null;
      synchronized (txMap)
      {
        txStack = (Stack) txMap.get(Thread.currentThread());
      }
      if (txStack != null && !txStack.isEmpty()) txHandle = (TransactionHandle) txStack.peek();
    }

    return txHandle;
  }
}
