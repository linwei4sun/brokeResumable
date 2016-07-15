package com.springMVC.brokeResumable.utils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;

import com.springMVC.brokeResumable.config.Configurations;

/**
 * Key Util: 1> according file name|size ..., generate a key;
 * 			 2> the key should be unique.
 */
public class TokenUtil {

	/**
	 * 生成Token， A(hashcode>0)|B + |name的Hash值| +_+size的值
	 * <span style="color:red">这里应该使用file的hash值,客户端上传，服务器获取文件之后再进行验证</span>
	 * @param name
	 * @param size
	 * @return
	 * @throws Exception
	 */
	public static String generateToken(String name, String size)
			throws IOException {
		if (name == null || size == null)
			return "";
		
		String fileName = name.replaceAll("/", Matcher.quoteReplacement(File.separator));
		File file = new File(Configurations.getFileRepository() + File.separator + fileName);
		if(file.exists() && file.length() == Long.valueOf(size) ){
			return CommonUtils.FILE_FIELD;
		}
		
		int code = name.hashCode();
		try {
			String token = (code > 0 ? "A" : "B") + Math.abs(code) + "_" + size.trim();
			/** TODO: store your token, here just create a file */
			IoUtil.storeToken(token);
			
			return token;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}
}
