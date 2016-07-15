package com.springMVC.brokeResumable.helper;

import java.io.File;
import java.util.HashMap;

import org.springframework.util.CollectionUtils;

public class BrokeResumableFile {
	private String token;
	private String tokenUrl;
	private String uploadUrl;
	private Long start;
	private String fileName;
	private File file;
	private HashMap<String, String> map;

	public BrokeResumableFile(String tokenUrl, String uploadUrl, String fileName) {
		this.tokenUrl = tokenUrl;
		this.uploadUrl = uploadUrl;
		this.fileName = fileName;
	}

	public BrokeResumableFile() {}

	public File getFile() {
		if (file == null || !file.exists())
			return new File(fileName);
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
		this.map.put("token", token);
	}

	public String getTokenUrl() {
		return tokenUrl;
	}

	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}

	public String getUploadUrl() {
		return uploadUrl;
	}

	public void setUploadUrl(String uploadUrl) {
		this.uploadUrl = uploadUrl;
	}

	public Long getStart() {
		return start;
	}

	public void setStart(Long start) {
		this.start = start;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public void setMap(HashMap<String, String> map) {
		this.map = map;
	}

	public HashMap<String, String> getMap() {
		if (CollectionUtils.isEmpty(map)) {
			map = new HashMap<>();
			map.put("name", getFile().getName());
			map.put("size", String.valueOf(getFile().length()));
		}
		return map;
	}

}