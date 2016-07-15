/**
 * 
 */
package com.springMVC.brokeResumable.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.NumberFormat;

import org.apache.log4j.Logger;

import com.springMVC.brokeResumable.utils.HtttpUrlConnectionUtils;
import com.springMVC.brokeResumable.utils.IoUtil;

/**
 * @author sunlw 
 */
public class BrokeResumableDownload implements BrokeResumableInterface,Runnable {
 
	Logger log = Logger.getLogger(getClass());
	private String downloadFilePath = "D:\\";
	private String downloadUrl = "http://localhost:8080/download";
	private String filenName = "CentOS-7-x86_64-DVD-1511.iso";
	
	public BrokeResumableDownload(String filenName) {
		super();
		this.filenName = filenName;
	}


	/* (non-Javadoc)
	 * @see test.client.BrokeResumableInterface#upload(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void upload() throws IOException {}
	
	
	@Override
	public void run() {
		try {
			log.info("线程:" + Thread.currentThread().getName() + " 开始下载:"+downloadFilePath + filenName);
			download();
		} catch (IOException e) {
			log.error(e);
		}
	}

	@Override
	public void download() throws IOException {
		
		/*
		 * 百分比格式化
		 */
		NumberFormat nf = NumberFormat.getPercentInstance();
		/*
		 * 保留2位小数
		 */
		nf.setMinimumFractionDigits(2);
		
		File file = new File(downloadFilePath + filenName);
		InputStream is = null;
		FileOutputStream os = null;
		Long contentLength = 0l;
		@SuppressWarnings("deprecation")
		String url = downloadUrl + "/" + (URLEncoder.encode(filenName)).replaceAll("\\+",  "%20");
		try {
			HttpURLConnection connection = HtttpUrlConnectionUtils.getConnect(url);
			Long start = 0l;
			if(file.exists()){
				start = file.length();
			}
			connection.addRequestProperty("content-range", "bytes "+start+"-"+ 0 +"/"+0);
			is = connection.getInputStream();
			contentLength = connection.getContentLengthLong();

			if(contentLength != file.length()){
				os = new FileOutputStream(file,true);
				int count  = 0;
				int readbyte = 10485760;
				byte[] bytes = new byte[(int) (readbyte > contentLength ? contentLength : readbyte)];
				while ((count = is.read(bytes)) != -1) {
					os.write(bytes, 0, count);
				}
				
				getPercent(file.length(), contentLength, nf);
			}else{
				log.info("file " + file.getPath() + " already exits!");
			}
		}catch(Exception e){
			getPercent(file.length(), contentLength, nf);
		} finally {
			IoUtil.close(is);
			IoUtil.close(os);
		}
	}
	
	private void getPercent(long start,long end,NumberFormat nf){
		double process = (double) start / end;
		log.info("已经下载:" + nf.format(process));
		log.info("已下载:"+HtttpUrlConnectionUtils.convertFileSize(start)+"/"+HtttpUrlConnectionUtils.convertFileSize(end));
	}
}
