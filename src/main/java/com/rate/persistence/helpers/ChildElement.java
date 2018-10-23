package com.rate.persistence.helpers;

import java.io.Serializable;
import java.lang.reflect.Method;

public class ChildElement implements Serializable
{

	/** The name of this element */
	private String	elementName;

	/** The name of the class for this object type. */
	private String	objectType;

	/** The name of the method for adding elements. */
	private String	addMethodName;

	/** The name of the method for getting elements. */
	private String	getMethodName;

	/** The Method object to be invoked to add elements. (lazy-loaded) */
	private Method	addMethod;

	/** The Method object to be invoked for getting child elements. (lazy-loaded) */
	private Method	getMethod;

	/**
	 * Constructor. All memebers are set via initializers.
	 */
	public ChildElement()
	{
	}

	/**
	 * Sets the name of this element.
	 */
	public void setElementName(String name)
	{
		elementName = name;
	}

	/**
	 * Returns the name of this element.
	 */
	public String getElementName()
	{
		return elementName;
	}

	/**
	 * Sets the object-type for this child-element. No checking is done at this time that the object-type is a valid class
	 * name.
	 */
	public void setObjectType(String type)
	{
		objectType = type;
	}

	/**
	 * Returns the name of the type of this child-element.
	 */
	public String getObjectType()
	{
		return objectType;
	}

	/**
	 * Sets the name of the method to call to add a new child element.
	 * 
	 * @param name
	 *          The name of the Method.
	 */
	public void setAddMethodName(String name)
	{
		addMethodName = name;
	}

	/**
	 * Returns the name of the Method used to add children.
	 */
	public String getAddMethodName()
	{
		return addMethodName;
	}

	/**
	 * Sets the member variable "getMethodName", which is the name of the get-method for this child-element.
	 */
	public void setGetMethodName(String name)
	{
		getMethodName = name;
	}

	/**
	 * Returns the name of the get-method for this child-element.
	 */
	public String getGetMethodName()
	{
		return getMethodName;
	}

	/**
	 * Returns the Method to call to add a child using this child-element. Calculated on first call and then stored, for
	 * efficiency.
	 * 
	 * @param parentClass
	 *          The Class the Method is to be invoked on.
	 * @return A method suitable for invoking to add objects corresponding to this child element.
	 * @throws ClassNotFoundException
	 *           if the Class corresponding to the object-type does not exist.
	 * @throws NoSuchMethodException
	 *           if the requested method is not found in the parent-class.
	 */
	public Method getAddMethod(Class parentClass)
			throws ClassNotFoundException, NoSuchMethodException
	{
		if ((addMethod == null) || (parentClass != addMethod.getDeclaringClass()))
		{
			Class[] argTypes = new Class[] { Class.forName(getObjectType()) };

			addMethod = parentClass.getMethod(getAddMethodName(), argTypes);
		}

		return addMethod;
	}

	/**
	 * Returns the Method to call to getting the children corresponding to this child. Calculated on first call and then
	 * stored, for efficiency.
	 * 
	 * @param parentClass
	 *          The Class the Method is to be invoked on.
	 * @return A method suitable for invoking to get children objects corresponding to this child element.
	 * @throws NoSuchMethodException
	 *           if the requested method is not found in the parent-class.
	 */
	public Method getGetMethod(Class parentClass)
			throws NoSuchMethodException
	{
		if ((getMethod == null) || (parentClass != getMethod.getDeclaringClass()))
		{
			Class[] argTypes = new Class[0];

			getMethod = parentClass.getMethod(getGetMethodName(), argTypes);
		}

		return getMethod;
	}
}
