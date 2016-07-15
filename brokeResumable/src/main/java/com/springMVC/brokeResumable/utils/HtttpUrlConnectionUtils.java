package com.springMVC.brokeResumable.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * httpurlconnection工具类
 * 
 * @author sunlw
 *
 */
public class HtttpUrlConnectionUtils {
	private final static Logger log = Logger.getLogger(HtttpUrlConnectionUtils.class);

	/**
	 * 解析请求的响应流读取
	 * 
	 * @param is
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static JSONObject parseInputStream(InputStream is) throws UnsupportedEncodingException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF8"));
		StringBuffer sb = new StringBuffer();
		String curLine = "";
		while ((curLine = reader.readLine()) != null) {
			sb.append(curLine);
		}
		is.close();
		JSONObject json = JSON.parseObject(sb.toString());
		return json;
	}

	/**
	 * 获取连接
	 * 
	 * @param urlStr
	 * @return
	 * @throws IOException
	 */
	public static HttpURLConnection getConnect(String urlStr) throws IOException {
		URL url = new URL(urlStr);
		HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
		return httpConnection;
	}

	/**
	 * 设置请求头
	 * 
	 * @param httpConnection
	 * @param requestMethod
	 * @throws ProtocolException
	 */
	public static void setConnectionHeader(HttpURLConnection httpConnection, String requestMethod)
			throws ProtocolException {
		httpConnection.setConnectTimeout(3000);
	}

	/**
	 * 发起get请求返回响应流
	 * 
	 * @param params
	 * @param baseUrl
	 * @return
	 * @throws IOException
	 */
	public static InputStream getInputStream(HashMap<String, String> params, String baseUrl) throws IOException {
		HttpURLConnection connection = getConnect(setUrl(params, baseUrl));
		InputStream is = connection.getInputStream();
		return is;
	}

	/**
	 * 处理参数
	 * 
	 * @param params
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static String setUrl(HashMap<String, String> params, String baseUrl) {
		StringBuffer param = new StringBuffer();
		if (!CollectionUtils.isEmpty(params)) {
			for (Entry<String, String> entry : params.entrySet()) {
				if (StringUtils.isBlank(entry.getValue())) {
					param.append(entry.getKey()).append("=").append("&");
				} else {
					/*
					 * URLEncoder.encode(String) 防止中文乱码，web服务器会自动decode
					 */
					param.append(entry.getKey()).append("=").append(URLEncoder.encode((String) entry.getValue()));
					param.append("&");
				}
			}
			param.deleteCharAt(param.length() - 1);
		}
		
		String paramStr = param.toString();
		if (StringUtils.isNotEmpty(paramStr)) {
			if (baseUrl.contains("?")) {
				baseUrl = baseUrl + "&" + paramStr;
			} else {
				baseUrl = baseUrl + "?" + paramStr;
			}
		}
		log.debug("请求的url为:" + baseUrl);
		return baseUrl;
	}

	/**
	 * 字节转换kb，mb，gb
	 * 
	 * @param size
	 * @return
	 */
	public static String convertFileSize(long size) {
		long kb = 1024;
		long mb = kb * 1024;
		long gb = mb * 1024;

		if (size >= gb) {
			return String.format("%.1f GB", (float) size / gb);
		} else if (size >= mb) {
			float f = (float) size / mb;
			return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
		} else if (size >= kb) {
			float f = (float) size / kb;
			return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
		} else
			return String.format("%d B", size);
	}
}