/**
 * 
 */
package com.springMVC.brokeResumable.client;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.text.NumberFormat;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.springMVC.brokeResumable.helper.BrokeResumableFile;
import com.springMVC.brokeResumable.utils.HtttpUrlConnectionUtils;

/**
 * @author sunlw 
 * 
 * 1.读取要上传的文件 
 * 2.请求token 
 * 3.获取当前上传文件的位置 
 * 4.post上传文件
 *
 */
public class BrokeResumableUpload implements BrokeResumableInterface,Runnable {
 
	Logger log = Logger.getLogger(getClass());

	private BrokeResumableFile data = new BrokeResumableFile("http://localhost:8080/tk", "http://localhost:8080/upload", "D:\\ftp\\download\\CentOS-7-x86_64-DVD-1511.iso");
	
	public void setData(BrokeResumableFile data){
		this.data = data;
	}

	/* (non-Javadoc)
	 * @see test.client.BrokeResumableInterface#upload(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void upload() throws IOException {
		try {
			/*
			 * 获取token
			 */
			getToken(data);
			/*
			 * 获取上传文件的位置 , token size fileName
			 */
			getUploadFilePosition(data);
			/*
			 * post上传文件
			 */
			uploadFile(data);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
	}

	/**
	 * post上传文件
	 * 
	 * @param file
	 * @param fileUploadMap
	 * @throws IOException
	 * @throws ProtocolException
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	private void uploadFile(BrokeResumableFile data)
			throws IOException, ProtocolException, FileNotFoundException, UnsupportedEncodingException {
		final byte[] bytes = new byte[10485760];

		do {
			HttpURLConnection connection = HtttpUrlConnectionUtils.getConnect(HtttpUrlConnectionUtils.setUrl(data.getMap(), data.getUploadUrl()));
			/*
			 * post请求
			 */
			connection.setRequestMethod("POST");
			/*
			 * 允许往outputstream写内容 inputstream默认为true，故不必设置
			 */
			connection.setDoOutput(true);

			// pos上回读取传送的位置 blob每次读取多少字节 10485760 超过文件，则读取文件的大小
			Long blob = 10485760 > data.getFile().length() ? data.getFile().length() : 10485760;
			String range = "bytes " + data.getStart() + "-" + ((data.getStart() + blob)*10) + "/" + data.getFile().length();
			connection.setRequestProperty("content-range", range);
			log.debug("******************************************range******************************************");
			log.debug(range);
			// 获取输出流对象，预备上传文件
			OutputStream os = connection.getOutputStream();

			FileInputStream fis = new FileInputStream(data.getFile());
			int count = 0;
			/*
			 * 每次读取10次到内存中，防止读取过多导致内存溢出，可以根据内存情况具体修改
			 */
			int times = 0;
			// 跳过已经上传的部分
			if (data.getStart() > 0)
				fis.skip(data.getStart());
			while ((count = fis.read(bytes)) != -1 && times < 10) {
				os.write(bytes, 0, count);
				times++;
			}
			os.flush();

			/*
			 * 获取响应
			 */
			JSONObject json = HtttpUrlConnectionUtils.parseInputStream(connection.getInputStream());
			/*
			 * 关闭资源
			 */
			fis.close();
			os.close();

			data.setStart(json.getLong("start"));
			log.error("upload result:" + json.toString());
			/*
			 * 百分比格式化
			 */
			NumberFormat nf = NumberFormat.getPercentInstance();
			/*
			 * 保留2位小数
			 */
			nf.setMinimumFractionDigits(2);

			double process = (double) data.getStart() / data.getFile().length();
			log.info("已经上传:" + nf.format(process));
			log.info(HtttpUrlConnectionUtils.convertFileSize(data.getStart()) + "/" + HtttpUrlConnectionUtils.convertFileSize(data.getFile().length()));
		} while (data.getStart() < data.getFile().length()); // 循环上传直到，开始位置到文件尾部
	}

	/**
	 * 获取文件继续上传的位置
	 * 
	 * @param file
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	private void getUploadFilePosition(BrokeResumableFile data)
			throws IOException, UnsupportedEncodingException {
		InputStream is = HtttpUrlConnectionUtils.getInputStream(data.getMap(), data.getUploadUrl());
		JSONObject json = HtttpUrlConnectionUtils.parseInputStream(is);
		log.debug("getFilePosition result success:" + json.get("success"));
		data.setStart(json.getLong("start"));
		log.debug("getFilePosition result start:" + json.get("start"));
	}

	/**
	 * 获取token ,并放到map参数集合中
	 * 
	 * @param map
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	@SuppressWarnings("deprecation")
	private void getToken(BrokeResumableFile data) throws IOException, UnsupportedEncodingException {
		InputStream is = HtttpUrlConnectionUtils.getInputStream(data.getMap(), data.getTokenUrl());
		JSONObject json = HtttpUrlConnectionUtils.parseInputStream(is);
		Boolean success = json.getBoolean("success");
		if(success){
			 data.setToken((String) json.get("token"));
		}else{
			log.error(data.getFile().getName() + " " + json.getString("message"));
//			System.exit(0);
			Thread.currentThread().stop();
		}
		log.debug("parseInputStream token:" + data.getToken());
	}
	
	@Override
	public void run() {
		try {
			log.info("线程:" + Thread.currentThread().getName() + " 开始上传:"+this.data.getFileName());
			upload();
		} catch (IOException e) {
			log.error(e);
		}
	}

	@Override
	public void download() throws IOException {
		
	}
}
