package com.rate.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.ConfigurationException;

public class PropertyConfiguration implements Configuration
{

	private static final String	FILE_NAME		= "scholarone.properties";

	protected static Properties		properties	= null;

	public PropertyConfiguration() throws ConfigurationException
	{
		if (properties == null)
		{
			init();
		}
	}

	private void init()
			throws ConfigurationException
	{
		properties = new Properties();

		InputStream is = this.getClass().getClassLoader().getResourceAsStream(FILE_NAME);

		if (is != null)
		{
			try
			{
				properties.load(is);
			}
			catch (IOException ioe)
			{
				throw new ConfigurationException("Problem loading " + FILE_NAME + ": " + ioe.getMessage());
			}
		}
		else
		{
			throw new ConfigurationException("Unable to locate " + FILE_NAME);
		}
	}

	public String getProperty(String key)
	{
		return properties.getProperty(key, "");
	}
}
