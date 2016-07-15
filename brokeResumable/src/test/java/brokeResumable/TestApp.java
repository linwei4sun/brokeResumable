/**
 * 
 */
package brokeResumable;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ObjectUtils.Null;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.springMVC.brokeResumable.client.BrokeResumableDownload;
import com.springMVC.brokeResumable.client.BrokeResumableInterface;
import com.springMVC.brokeResumable.client.BrokeResumableUpload;
import com.springMVC.brokeResumable.helper.BrokeResumableFile;
import com.springMVC.brokeResumable.helper.Range;

/**
 * @author sunlw
 *
 */
public class TestApp {
	private static Logger log = Logger.getLogger(TestApp.class);
	
	@Test
	public void testSpring(){
//		ClassPathResource resource = new ClassPathResource("spring-bean.xml");
//		DefaultListableBeanFactory defaultListableBeanFactory = new DefaultListableBeanFactory();
//		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(defaultListableBeanFactory);
//		beanDefinitionReader.loadBeanDefinitions(resource);
//		Range range = defaultListableBeanFactory.getBean(Range.class);
//		System.out.println(range.toString());
		
		
		@SuppressWarnings("resource")
		ApplicationContext context = new FileSystemXmlApplicationContext("classpath:spring-bean.xml");
		Range range2 = context.getBean("RangeT", Range.class);
		log.info(range2.toString());
	}
	
	public static void main(String[] args){
		testDownload();
//		BrokeResumableDownload brokeResumableDownload1 = new BrokeResumableDownload("CentOS-7-x86_64-DVD-1511.iso");
//		Thread thread1 = new Thread(brokeResumableDownload1);
//		thread1.start();
//		try {
//			brokeResumableDownload1.download();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	@Test
	public void testUpload(){
		BrokeResumableUpload brokeResumable1 = new BrokeResumableUpload();
		brokeResumable1.setData(new BrokeResumableFile("http://localhost:8080/tk", 
											"http://localhost:8080/upload", 
											"D:\\ftp\\download\\Sublime Text Build 3114 x64 Setup.exe"));
		try {
			brokeResumable1.upload();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 多线程测试断点上传
	 */
	public static void testUploadThread(){
//		BrokeResumableInterface brokeResumable = new BrokeResumable();
		BrokeResumableUpload brokeResumable = new BrokeResumableUpload();
		BrokeResumableUpload brokeResumable1 = new BrokeResumableUpload();
		BrokeResumableUpload brokeResumable2 = new BrokeResumableUpload();
		try {
			brokeResumable.setData(new BrokeResumableFile(
										"http://localhost:8080/tk", 
										"http://localhost:8080/upload", 
										"D:\\ftp\\download\\CentOS-7-x86_64-DVD-1511.iso"));
			brokeResumable1.setData(new BrokeResumableFile("http://localhost:8080/tk", 
											"http://localhost:8080/upload", 
											"D:\\ftp\\download\\jdk-8u91-windows-x64.exe"));
			brokeResumable2.setData(new BrokeResumableFile("http://localhost:8080/tk", 
											"http://localhost:8080/upload", 
											"D:\\ftp\\download\\Sublime Text Build 3114 x64 Setup.exe"));
			
			Thread thread = new Thread(brokeResumable);
			Thread thread1 = new Thread(brokeResumable1);
			Thread thread2 = new Thread(brokeResumable2);
			
			thread.start();
			thread1.start();
			thread2.start();
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	@Test
	public void testStringHashCode(){
		Pattern pattern = Pattern.compile(".*");
		Matcher matcher = pattern.matcher("CentOS-7-x86_64-DVD-1511.iso");
		log.info(matcher.matches());
	}
	/**
	 * 多线程测试断点下载
	 */
	public static void testDownload(){
		BrokeResumableDownload brokeResumableDownload = new BrokeResumableDownload("CentOS-7-x86_64-DVD-1511.iso");
		BrokeResumableDownload brokeResumableDownload1 = new BrokeResumableDownload("jdk-8u91-windows-x64.exe");
		BrokeResumableDownload brokeResumableDownload2 = new BrokeResumableDownload("Sublime Text Build 3114 x64 Setup.exe");
		try {
			Thread thread = new Thread(brokeResumableDownload);
			Thread thread1 = new Thread(brokeResumableDownload1);
			Thread thread2 = new Thread(brokeResumableDownload2);
			thread.start();
			thread1.start();
			thread2.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
