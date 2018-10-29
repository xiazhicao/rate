package com.rate.callApi;

import com.rate.tool.ReadUrlUtil;

public class callApi {

	public static void main(String[] args) throws Exception {
		String url = "https://stock.finance.sina.com.cn/usstock/api/jsonp_v2.php/var%20t1csbr=/US_MinlineNService.getMinline?symbol=csbr&day=1";
		ReadUrlUtil ru = new ReadUrlUtil();
		ru.analysis(url);

	}

}
