package com.rate.persistence.helpers;

import java.io.Serializable;

public class SaveMethod extends PersistenceMethod implements Serializable
{

	/** Whether output of the attributes is on one line, versus one per line */
	private Boolean	singleLine	= Boolean.FALSE;

    /** Whether to perform resourcing during save method.  Default is true */
    private Boolean doResourcing  = Boolean.TRUE;

	/**
	 * Constructor. All members are set through accessors.
	 */
	public SaveMethod()
	{
	}

	/**
	 * Sets whether the attributes will be writen out on one line. If "true", then all attributes will be on one line.
	 * Otherwise, attributes are writen out one per line.
	 * 
	 * @param value
	 *          String representing a boolean value.
	 */
	public void setSingleLine(String value)
	{
		singleLine = Boolean.valueOf(value);
	}

	/**
	 * Returns the Boolean indicating whether attributes are on one line.
	 */
	public Boolean getSingleLine()
	{
		return singleLine;
	}


    /**
     * Sets whether to perform resourcing during save method. 
     * 
     * @param value
     *          String representing a boolean value.
     */
    public void setDoResourcing(String value)
    {
      doResourcing = Boolean.valueOf(value);
    }

    /**
     * Returns the Boolean indicating whether to perform resourcing during save method. 
     */
    public Boolean getDoResourcing()
    {
        return doResourcing;
    }
}
