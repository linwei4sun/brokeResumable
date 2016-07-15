/**
 * 
 */
package com.springMVC.brokeResumable.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.springMVC.brokeResumable.config.Configurations;
import com.springMVC.brokeResumable.exception.StreamException;
import com.springMVC.brokeResumable.helper.Range;
import com.springMVC.brokeResumable.utils.CommonUtils;
import com.springMVC.brokeResumable.utils.IoUtil;
import com.springMVC.brokeResumable.utils.TokenUtil;

/**
 * @author sunlw springmvc 断点续传测试
 */
@Controller
@RequestMapping("/")
public class UploadController {

	Logger log = LoggerFactory.getLogger(this.getClass());

	@RequestMapping("/index")
	public String index(HttpServletRequest request, Model model) {
		model.addAttribute("ip", CommonUtils.getIpAddr(request));
		return "index";
	}

	/**
	 * springmvc断点续传
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public void SteamUpload(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doOptions(req, resp);

		OutputStream out = null;
		InputStream content = null;
		content = req.getInputStream();

		final String token = req.getParameter(CommonUtils.TOKEN_FIELD);
		final String fileName = new String(req.getParameter(CommonUtils.FILE_NAME_FIELD).getBytes("ISO-8859-1"),
				"utf-8");
		Range range = IoUtil.parseRange(req);

		final PrintWriter writer = resp.getWriter();

		/** TODO: validate your token. */

		JSONObject json = new JSONObject();
		long start = 0;
		boolean success = true;
		String message = "";
		File f = IoUtil.getTokenedFile(token);
		try {
			if (f.length() != range.getFrom()) {
				/** drop this uploaded data */
				throw new StreamException(StreamException.ERROR_FILE_RANGE_START);
			}

			out = new FileOutputStream(f, true);
			int read = 0;
			final byte[] bytes = new byte[CommonUtils.BUFFER_LENGTH];
			while ((read = content.read(bytes)) != -1)
				out.write(bytes, 0, read);

			start = f.length();
		} catch (StreamException se) {
			success = StreamException.ERROR_FILE_RANGE_START == se.getCode();
			message = "Code: " + se.getCode();
		} catch (FileNotFoundException fne) {
			message = "Code: " + StreamException.ERROR_FILE_NOT_EXIST;
			success = false;
		} catch (IOException io) {
			message = "IO Error: " + io.getMessage();
			success = false;
		} finally {
			IoUtil.close(out);
			IoUtil.close(content);

			/** rename the file */
			if (range.getSize() == start) {
				/** fix the `renameTo` bug */
				// File dst = IoUtil.getFile(fileName);
				// dst.delete();
				// TODO: f.renameTo(dst);
				// 重命名在Windows平台下可能会失败，stackoverflow建议使用下面这句
				try {
					// 先删除
					IoUtil.getFile(fileName).delete();

					Files.move(f.toPath(), f.toPath().resolveSibling(fileName));
					log.info("TK: `" + token + "`, NE: `" + fileName + "`");

					/** if `STREAM_DELETE_FINISH`, then delete it. */
					if (Configurations.isDeleteFinished()) {
						IoUtil.getFile(fileName).delete();
					}
				} catch (IOException e) {
					success = false;
					message = "Rename file error: " + e.getMessage();
				}

			}
			try {
				if (success)
					json.put(CommonUtils.START_FIELD, start);
				json.put(CommonUtils.SUCCESS, success);
				json.put(CommonUtils.MESSAGE, message);
			} catch (JSONException e) {
			}

			writer.write(json.toString());
			IoUtil.close(writer);
		}
	}

	/**
	 * springmvc断点续传
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public void getFilePosition(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doOptions(req, resp);

		final String token = req.getParameter(CommonUtils.TOKEN_FIELD);
		final String size = req.getParameter(CommonUtils.FILE_SIZE_FIELD);
		final String fileName = new String(req.getParameter(CommonUtils.FILE_NAME_FIELD).getBytes("iso-8859-1"),
				"utf-8");
		final PrintWriter writer = resp.getWriter();

		/** TODO: validate your token. */

		JSONObject json = new JSONObject();
		long start = 0;
		boolean success = true;
		String message = "";
		try {
			File f = IoUtil.getTokenedFile(token);
			// 再次上传从此处开始上传
			start = f.length();
			/** file size is 0 bytes. */
			if (token.endsWith("_0") && "0".equals(size) && 0 == start)
				f.renameTo(IoUtil.getFile(fileName));
		} catch (FileNotFoundException fne) {
			message = "Error: " + fne.getMessage();
			success = false;
		} finally {
			try {
				if (success)
					json.put(CommonUtils.START_FIELD, start);
				json.put(CommonUtils.SUCCESS, success);
				json.put(CommonUtils.MESSAGE, message);
			} catch (JSONException e) {
			}
			writer.write(json.toString());
			IoUtil.close(writer);
		}
	}

	/**
	 * springmvc断点续传
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	@RequestMapping(value = "/tk", method = RequestMethod.GET)
	public void getTK(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String name = new String(req.getParameter(CommonUtils.FILE_NAME_FIELD).getBytes("iso-8859-1"), "utf8");
		String size = req.getParameter(CommonUtils.FILE_SIZE_FIELD);
		String token = "";
		PrintWriter writer = resp.getWriter();

		JSONObject json = new JSONObject();
		try {
			token = TokenUtil.generateToken(name, size);
			if (CommonUtils.FILE_FIELD.equals(token)) {
				json.put(CommonUtils.TOKEN_FIELD, "");
				json.put(CommonUtils.SUCCESS, false);
				if (CommonUtils.FILE_FIELD.equals(token)) {
					json.put(CommonUtils.MESSAGE, "File already exists!");
				} else {
					json.put(CommonUtils.MESSAGE, "Please try again later");
				}
			} else {
				json.put(CommonUtils.TOKEN_FIELD, token);
				if (Configurations.isCrossed())
					json.put(CommonUtils.SERVER_FIELD, Configurations.getCrossServer());
				json.put(CommonUtils.SUCCESS, true);
				json.put(CommonUtils.MESSAGE, "");
			}
		} catch (Exception e) {

		}
		/** TODO: save the token. */

		writer.write(json.toString());
	}

	/**
	 * springmvc断点续传
	 * 
	 * @param req
	 * @param resp
	 * @throws ServletException
	 * @throws IOException
	 */
	@RequestMapping(value = "/fd", method = RequestMethod.POST)
	public void getFd(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doOptions(req, resp);

		/** flash @ windows bug */
		req.setCharacterEncoding("utf8");

		final PrintWriter writer = resp.getWriter();
		// Check that we have a file upload request
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if (!isMultipart) {
			writer.println("ERROR: It's not Multipart form.");
			return;
		}
		JSONObject json = new JSONObject();
		long start = 0;
		boolean success = true;
		String message = "";

		ServletFileUpload upload = new ServletFileUpload();
		InputStream in = null;
		String token = null;
		try {
			FileItemIterator iter = upload.getItemIterator(req);
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				String name = item.getFieldName();
				in = item.openStream();
				if (item.isFormField()) {
					String value = Streams.asString(in);
					if (CommonUtils.TOKEN_FIELD.equals(name)) {
						token = value;
						/** TODO: validate your token. */
					}
					System.out.println(name + ":" + value);
				} else {
					String fileName = item.getName();
					start = IoUtil.streaming(in, token, fileName);
				}
			}
		} catch (FileUploadException fne) {
			success = false;
			message = "Error: " + fne.getLocalizedMessage();
		} finally {
			try {
				if (success)
					json.put(CommonUtils.START_FIELD, start);
				json.put(CommonUtils.SUCCESS, success);
				json.put(CommonUtils.MESSAGE, message);
			} catch (JSONException e) {
			}

			writer.write(json.toString());
			IoUtil.close(in);
			IoUtil.close(writer);
		}
	}

	@RequestMapping("/download/{fileName:.*}")
	public void download(HttpServletRequest req, HttpServletResponse rep,@PathVariable("fileName")String fileName) {
		BufferedInputStream fis = null;
		File file = null;
		long contentLength = 0l;
		OutputStream os = null;
		fileName = "D:\\ftp\\download\\" + fileName;
		try {
			if (StringUtils.isNotEmpty(fileName) && (file = new File(fileName)).exists()) {
				
				 /*
				  * 断点开始
				  *	响应的格式是:
	              *	Content-Range: bytes [文件块的开始字节]-[文件的总大小 - 1]/[文件的总大小]
				  */
				Range range = null;
				try {
					range = IoUtil.parseRange(req);
				} catch (Exception e) {
					
				}
				contentLength = file.length();
				
				// 允许客户端传送 accept-ranges
	            rep.reset();
	            rep.setHeader("Accept-Ranges", "bytes");
	            //设置Content-Disposition  下载的文件名和格式不然浏览器会是download的东西
		        rep.setHeader("Content-Disposition", "attachment;filename="+file.getName()); 
		        
		        /*
		         * 如果设设置了Content-Length，则客户端会自动进行多线程下载。如果不希望支持多线程，则不要设置这个参数。
	             *  Content-Length: [文件的总大小] - [客户端请求的下载的文件块的开始字节]
		         */
		        rep.setHeader("content-length", String.valueOf(contentLength));
		        
				os = rep.getOutputStream();
				fis = new BufferedInputStream(new FileInputStream(file));
				
				if(range != null)
				// 跳过
				fis.skip(range.getFrom());
				
				byte[] bytes = new byte[CommonUtils.BUFFER_LENGTH];
				int count = 0;
				while ((count = fis.read(bytes)) != -1 ) {
					os.write(bytes, 0, count);
				}
				os.flush();
			}
		} catch (UnsupportedEncodingException e) {
			// TODO filename
			e.printStackTrace();
		} catch (IOException e) {
			// TODO range
			e.printStackTrace();
		} catch(Exception e){
			log.info("客户端断开链接");
		}finally {
			IoUtil.close(fis);
			IoUtil.close(os);
		}

	}
	@RequestMapping("/downloadfile")
	public void downFile(HttpServletResponse response, HttpServletRequest request, String location){
	    BufferedInputStream bis = null;
	    try {
	        File file = new File(location);
	        if (file.exists()) {
	            long p = 0L;
	            long toLength = 0L;
	            long contentLength = 0L;
	            int rangeSwitch = 0; // 0,从头开始的全文下载；1,从某字节开始的下载（bytes=27000-）；2,从某字节开始到某字节结束的下载（bytes=27000-39000）
	            long fileLength;
	            String rangBytes = "";
	            fileLength = file.length();
	 
	            // get file content
	            InputStream ins = new FileInputStream(file);
	            bis = new BufferedInputStream(ins);
	 
	            // tell the client to allow accept-ranges
	            response.reset();
	            response.setHeader("Accept-Ranges", "bytes");
	 
	            // client requests a file block download start byte
	            String range = request.getHeader("Range");
	            if (range != null && range.trim().length() > 0 && !"null".equals(range)) {
	                response.setStatus(javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);
	                rangBytes = range.replaceAll("bytes=", "");
	                if (rangBytes.endsWith("-")) {  // bytes=270000-
	                    rangeSwitch = 1;
	                    p = Long.parseLong(rangBytes.substring(0, rangBytes.indexOf("-")));
	                    contentLength = fileLength - p;  // 客户端请求的是270000之后的字节（包括bytes下标索引为270000的字节）
	                } else { // bytes=270000-320000
	                    rangeSwitch = 2;
	                    String temp1 = rangBytes.substring(0, rangBytes.indexOf("-"));
	                    String temp2 = rangBytes.substring(rangBytes.indexOf("-") + 1, rangBytes.length());
	                    p = Long.parseLong(temp1);
	                    toLength = Long.parseLong(temp2);
	                    contentLength = toLength - p + 1; // 客户端请求的是 270000-320000 之间的字节
	                }
	            } else {
	                contentLength = fileLength;
	            }
	 
	            // 如果设设置了Content-Length，则客户端会自动进行多线程下载。如果不希望支持多线程，则不要设置这个参数。
	            // Content-Length: [文件的总大小] - [客户端请求的下载的文件块的开始字节]
	            response.setHeader("Content-Length", String.valueOf(contentLength));
	 
	            // 断点开始
	            // 响应的格式是:
	            // Content-Range: bytes [文件块的开始字节]-[文件的总大小 - 1]/[文件的总大小]
	            if (rangeSwitch == 1) {
	                String contentRange = new StringBuffer("bytes ").append(new Long(p).toString()).append("-")
	                        .append(new Long(fileLength - 1).toString()).append("/")
	                        .append(new Long(fileLength).toString()).toString();
	                response.setHeader("Content-Range", contentRange);
	                bis.skip(p);
	            } else if (rangeSwitch == 2) {
	                String contentRange = range.replace("=", " ") + "/" + new Long(fileLength).toString();
	                response.setHeader("Content-Range", contentRange);
	                bis.skip(p);
	            } else {
	                String contentRange = new StringBuffer("bytes ").append("0-")
	                        .append(fileLength - 1).append("/")
	                        .append(fileLength).toString();
	                response.setHeader("Content-Range", contentRange);
	            }
	 
	            String fileName = file.getName();
	            response.setContentType("application/octet-stream");
	            response.addHeader("Content-Disposition", "attachment;filename=" + fileName);
	 
	            OutputStream out = response.getOutputStream();
	            int n = 0;
	            long readLength = 0;
	            int bsize = 1024;
	            byte[] bytes = new byte[bsize];
	            if (rangeSwitch == 2) {
	                // 针对 bytes=27000-39000 的请求，从27000开始写数据                    
	                while (readLength <= contentLength - bsize) {
	                    n = bis.read(bytes);
	                    readLength += n;
	                    out.write(bytes, 0, n);
	                }
	                if (readLength <= contentLength) {
	                    n = bis.read(bytes, 0, (int) (contentLength - readLength));
	                    out.write(bytes, 0, n);
	                }                   
	            } else {
	                while ((n = bis.read(bytes)) != -1) {
	                    out.write(bytes,0,n);                                                      
	                }                   
	            }
	            out.flush();
	            out.close();
	            bis.close();
	        } else {
	            if (log.isDebugEnabled()) {
	                log.debug("Error: file " + location + " not found.");
	            }                
	        }
	    } catch (IOException ie) {
	        // 忽略 ClientAbortException 之类的异常
	    } catch (Exception e) {
	        log.error(e.toString());
	    }
	}
	
	private void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json;charset=utf-8");
		resp.setHeader("Access-Control-Allow-Headers", "Content-Range,Content-Type");
		resp.setHeader("Access-Control-Allow-Origin", Configurations.getCrossOrigins());
		resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
	}
}
