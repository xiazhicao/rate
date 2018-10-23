package com.rate.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

public class ConnectionInfo
{
	public String		id;

	public String		threadName;

	public Date			startDate;

	public boolean	autoCommit	= false;

	/**
	 * Constructor for ConnectionInfo
	 */
	public ConnectionInfo(Connection connection)
	{
		super();

		if (connection != null)
		{
			this.id = connection.toString();
			this.startDate = new Date();
			this.threadName = Thread.currentThread().getName();
			try
			{
				this.autoCommit = connection.getAutoCommit();
			}
			catch (SQLException se)
			{
				throw new Error(se.getMessage());
			}
		}
	}

	/**
	 * @return Returns the autoCommit.
	 */
	public boolean isAutoCommit()
	{
		return autoCommit;
	}

	/**
	 * @return Returns the id.
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * @return Returns the startDate.
	 */
	public Date getStartDate()
	{
		return startDate;
	}

	/**
	 * @return Returns the threadName.
	 */
	public String getThreadName()
	{
		return threadName;
	}

}
