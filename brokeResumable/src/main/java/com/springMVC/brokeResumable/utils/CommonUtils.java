/**
 * 
 */
package com.springMVC.brokeResumable.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

/**
 * @author sunlw
 *
 */
public class CommonUtils {

	// stream servlet
	/** when the has increased to 10kb, then flush it to the hard-disk. */
	public static final int BUFFER_LENGTH = 10240;
	public static final String START_FIELD = "start";
	public static final String CONTENT_RANGE_HEADER = "content-range";

	// tokenservlet
	public static final String FILE_NAME_FIELD = "name";
	public static final String FILE_SIZE_FIELD = "size";
	public static final String TOKEN_FIELD = "token";
	public static final String SERVER_FIELD = "server";
	public static final String SUCCESS = "success";
	public static final String MESSAGE = "message";

	// formaDataServlet
	public static final String FILE_FIELD = "file";
	/** when the has read to 10M, then flush it to the hard-disk. */
	// public static final int BUFFER_LENGTH = 1024 * 1024 * 10;
	public static final int MAX_FILE_SIZE = 1024 * 1024 * 100;

	/**
	 * 获取ip
	 * @param req
	 * @return
	 */
	public static String getIpAddr(HttpServletRequest req) {
		String ipAddress = null;
		// ipAddress = this.getRequest().getRemoteAddr();
		ipAddress = req.getHeader("x-forwarded-for");
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = req.getHeader("Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = req.getHeader("WL-Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = req.getRemoteAddr();
			if (ipAddress.equals("127.0.0.1")) {
				// 根据网卡取本机配置的IP
				InetAddress inet = null;
				try {
					inet = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				try {
					ipAddress = inet.getHostAddress();
				} catch (NullPointerException e) {
					ipAddress = null;
				}
			}
		}
		// 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
		if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
															// = 15
			if (ipAddress.indexOf(",") > 0) {
				ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
			}
		}
		return ipAddress;
	}
}
