package com.rate.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.rate.entity.Stock;

import net.sf.json.JSONException;

public class ReadUrlUtil {

	public static void main(String[] args) throws Exception{
		String url = "https://stock.finance.sina.com.cn/usstock/api/jsonp_v2.php/var%20t1csbr=/US_MinlineNService.getMinline?symbol=csbr&day=1";
		ReadUrlUtil ru = new ReadUrlUtil();
		ru.analysis(url);
	}
	
	public void analysis(String url) throws Exception {
		String json = readJsonFromUrl(url);
		String name = json.substring(6, json.indexOf("="));
		String body = json.substring(json.indexOf("\"") + 1, json.lastIndexOf("\""));
		getData(body, name);
	}
	
	public static String readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			StringBuilder sb = new StringBuilder();
			int cp;
			while ((cp = rd.read()) != -1) {
				sb.append((char) cp);
			}
			String jsonText = sb.toString();
			return jsonText;
		} finally {
			is.close();
		}
	}

	public void getData(String json, String name) throws Exception {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String[] dataList = json.split(";");
		Stock stock = null;
		Date parsedDate = null;
		Timestamp timestamp = null;

		for (String data : dataList) {
			String[] elements = data.split(",");
			stock = new Stock();
			stock.setName(name);
			stock.setPrice(Double.valueOf(elements[3]));
			stock.setVolume(Double.valueOf(elements[1]));
			stock.setStockId(1);
			parsedDate = dateFormat.parse(elements[0]);
			timestamp = new java.sql.Timestamp(parsedDate.getTime());
			stock.setAddedDate(timestamp);
			stock.save(1);
			System.out.println(elements[0]+","+elements[1]+","+elements[3]);
		}
	}
}
