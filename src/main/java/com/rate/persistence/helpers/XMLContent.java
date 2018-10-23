package com.rate.persistence.helpers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class XMLContent implements Serializable
{

	/** A map of the children elements, with the string name as the key. */
	private HashMap		childElements;

	/** A list of the names of the children. Used to get the names in the order specified. */
	private ArrayList	childrenNames;

	/**
	 * Constructor. Nothing special.
	 */
	public XMLContent()
	{
		childElements = new HashMap();
		childrenNames = new ArrayList();
	}

	/**
	 * Adds the indicated ChildElement. If an alement with the same name has already been added, then nothing happens.
	 * 
	 * @param ce
	 *          New ChildElement to add.
	 */
	public void addChildElement(ChildElement ce)
	{
		String name = ce.getElementName();

		if (childElements.get(name) == null)
		{
			childElements.put(name, ce);
			childrenNames.add(name);
		}
	}

	/**
	 * Returns a List of the names of the children elements. Array is in the same order children were added. Array is a
	 * copy of the internal array.
	 * 
	 * @return a List of the names of the children elements, in the order added.
	 */
	public List getChildrenNames()
	{
		return new ArrayList(childrenNames);
	}

	/**
	 * Returns the ChildElement that has the indicated name. Comparison is case-sensitive. Returns null if no element is
	 * found.
	 */
	public ChildElement getChildElement(String name)
	{
		return (ChildElement) childElements.get(name);
	}
}