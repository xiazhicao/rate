package com.rate.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import net.sf.json.JSONException;

import com.rate.entity.Stock;


public class ReadUrlUtil
{

	public static String readJsonFromUrl(String url) throws IOException,
	    JSONException
	{
		InputStream is = new URL(url).openStream();
		try
		{
			BufferedReader rd = new BufferedReader(new InputStreamReader(is,
			    Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1)
			{
				sb.append((char) cp);
			}
			String jsonText = sb.toString();
			return jsonText;
		}
		finally
		{
			is.close();
		}
	}
	
	public void analysis(String url) throws JSONException, IOException
	{
		String json = readJsonFromUrl(url);
    
	}
	
	public List<Stock> getData(String json, List<Stock> list)
	{
		if(json.length() == 0)
		{
			return list;
		}
		
		
		
	}
}