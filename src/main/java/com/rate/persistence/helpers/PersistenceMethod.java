package com.rate.persistence.helpers;

import java.io.Serializable;

public class PersistenceMethod implements Serializable
{

	/** The name of this method. */
	private String					methodName;

	/** The stored-procedure associated with this (if any) */
	private StoredProcedure	procedure;

	/** The XMLContent associated with this (if any) */
	private XMLContent			xmlContent;

	/**
	 * Constructor. All members are set through accessors.
	 */
	public PersistenceMethod()
	{
	}

	/**
	 * Sets the name of this method.
	 */
	public void setMethodName(String name)
	{
		methodName = name;
	}

	/**
	 * Returns the name of this method.
	 */
	public String getMethodName()
	{
		return methodName;
	}

	/**
	 * Sets the stored-procedure associated with this.
	 */
	public void setProcedure(StoredProcedure proc)
	{
		procedure = proc;
	}

	/**
	 * Retuns the StoredProcedure associated with this (if any).
	 */
	public StoredProcedure getStoredProcedure()
	{
		return procedure;
	}

	/**
	 * Sets the XMLContent associated with this.
	 */
	public void setXMLContent(XMLContent content)
	{
		xmlContent = content;
	}

	/**
	 * Returns the XMLContent associated with this (if any).
	 */
	public XMLContent getXMLContent()
	{
		return xmlContent;
	}

}
