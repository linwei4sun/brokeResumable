package com.springMVC.brokeResumable.client;

import java.io.IOException;

/**
 * 
 * @author sunlw
 */
public interface BrokeResumableInterface {
	/**
	 * 1.读取要上传的文件 
	 * 2.请求token 
	 * 3.获取当前上传文件的位置 
	 * 5.post上传文件
	 * 
	 * @throws IOException
	 */
	public void upload() throws IOException;

	public void download() throws IOException;
}