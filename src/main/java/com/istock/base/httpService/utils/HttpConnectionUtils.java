package com.istock.base.httpService.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.HttpMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

public class HttpConnectionUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpConnectionUtils.class);
	
	
	private static String HEADER_CONTENT_TYPE = "Content-type";
	private static String HEADER_ACCEPT = "Accept";
	public static String loadJson(String url , Map<String , String> queryParams ,Map<String , Object> bodyParams, Map<String , String> header , String method , String mediaType){
		HttpURLConnection connection = null;
		
		try{
			
			StringBuffer queryParam = new StringBuffer();
			for(Entry<String ,String> entry : queryParams.entrySet()){
				queryParam.append("&").append(entry.getKey()).append("=").append(entry.getValue());
			}
			logger.info("ready to send post http request:{} ,params:{},body:{} , header:{}" , new Object[]{url , queryParam.length()>0?queryParam.deleteCharAt(0):queryParam ,JSON.toJSONString(bodyParams), header});
			String targetUrl = url;
			if(!queryParams.isEmpty()){
				if(url.indexOf("?")>0){
					targetUrl += queryParam.toString();
				}else{
					targetUrl += "?"+queryParam.toString();
				}
			}
			URL addressUrl = new URL(targetUrl);
			connection = (HttpURLConnection) addressUrl.openConnection();
			connection.setRequestMethod(method);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setReadTimeout(5000);
			connection.setConnectTimeout(3000);
			connection.setInstanceFollowRedirects(true);
			
			boolean isPostJson = isPostJson(mediaType);
			if(isPostJson){
				connection.addRequestProperty(HEADER_CONTENT_TYPE, mediaType);
			}else{
				if(method == HttpMethod.POST){
					connection.addRequestProperty(HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded;charset=UTF-8");
				}else{
					connection.addRequestProperty(HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
				}
			}
			
			connection.addRequestProperty(HEADER_ACCEPT, "application/json");
			connection.setRequestProperty("Accept-Charset", "utf-8");
			if(!header.isEmpty()){
				for(Entry<String , String> entry : header.entrySet()){
					connection.addRequestProperty(entry.getKey(), entry.getValue());
				}
			}
			String body = "";
			if(!bodyParams.isEmpty()){
				String targetMediaType = connection.getRequestProperty(HEADER_CONTENT_TYPE);
				if(isFormSubmit(targetMediaType)){
					StringBuffer bodyBuffer = new StringBuffer();
					Map<String , String> paramMap = new HashMap<String , String>();
					for(Entry<String ,Object> entry : bodyParams.entrySet()){
//						bodyBuffer.append("&").append(entry.getKey()).append("=").append(entry.getValue());
						paramMap.putAll(generateParam(entry.getKey() , entry.getValue().getClass() , entry.getValue()));
					}
					for(Entry<String , String> entry : paramMap.entrySet()){
						if(entry.getKey() != null && entry.getKey().length()>0){
							bodyBuffer.append("&").append(entry.getKey()).append("=").append(entry.getValue());
						}
					}
					if(bodyBuffer.length()>0){
						bodyBuffer.deleteCharAt(0);
					}
					body = bodyBuffer.toString();
				}else if(isPostJson(targetMediaType)){
					for(Entry<String ,Object> entry : bodyParams.entrySet()){
						body = JSON.toJSONString(entry.getValue());
					}
				}
				
			}
			connection.connect();
			OutputStream os = connection.getOutputStream();
			os.write(body.getBytes("UTF-8"));
			
			InputStream is = connection.getInputStream();
			String result = readResult(is);
			logger.info("http response :{}" ,result);
			return result;
		}catch(Exception e){
			logger.error("send request error:" , e);
		}
		return "";
	}
	
	/**使用POST方式提交请求,解析body对象的参数
	 * @param paramName
	 * @param paramClass
	 * @param paramValue
	 * @return
	 * @throws Exception
	 */
	private static Map<String , String> generateParam(String paramName , Class<?> paramClass, Object paramValue) throws Exception{
		Map<String , String> result = new HashMap<String , String>();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if(paramValue != null){
//			Class paramClass = paramValue.getClass();
			if(paramClass.isPrimitive() || Number.class.isAssignableFrom(paramClass) || String.class.isAssignableFrom(paramClass)){
				result.put(paramName, paramValue + "");
			}else if(paramValue instanceof Collection || paramClass.isArray()){
				Object[] array = null;
				if(paramValue instanceof Collection){
					Collection<Object> collection = (Collection)paramValue;
					array = collection.toArray(new Object[0]);
				}
				if(paramClass.isArray()){
					array = (Object[]) paramValue;
				}
				if(array != null){
					for(int i =0;i<array.length;i++){
						Object item = array[i];
						if(item.getClass().isPrimitive() || item instanceof Number || item instanceof Character || item instanceof String){
							String str = result.get(paramName);
							if(str == null || str.trim().length()<=0){
								str = item+"";
							}else{
								str += ","+item;
							}
							result.put(paramName, str);
						}else{
							Field[] fieldArray = item.getClass().getDeclaredFields();
							for(Field field : fieldArray){
								field.setAccessible(true);
								String str = result.get(field.getName());
								if(str == null || str.trim().length()<=0){
									str = field.get(item)+"";
								}else{
									str += ","+field.get(item);
								}
								result.put(field.getName(), str);
							}
						}
					}
				}
			}else if(Date.class.isAssignableFrom(paramClass)){
				result.put(paramName, format.format((Date)paramValue));
			}else if(paramClass.isEnum()){
				result.put(paramName , ((Enum)paramValue).name());
			}
			else {
				Field[] fieldArray = paramClass.getDeclaredFields();
				for(Field field : fieldArray){
					field.setAccessible(true);
					Object fieldValue = field.get(paramValue);
					if(fieldValue != null){
						String str = result.get(field.getName());
						
						if(str == null || str.trim().length()<=0){
							str = field.get(paramValue)+"";
						}else{
							str += ","+field.get(paramValue);
						}
						
						result.put(field.getName(), str);
					}
				}
				Class<?> paramClazz = paramClass.getSuperclass();
				while(!paramClazz.equals(Object.class)){
					result.putAll(generateParam(null , paramClazz,paramValue ));
					paramClazz = paramClazz.getSuperclass();
				}
			}
		}
		return result;
	}
	
	private static String readResult(InputStream is) throws Exception{
		ByteArrayOutputStream ba = new ByteArrayOutputStream();
		if(is != null){
			byte[] byteArray = new byte[2048];
			int count = -1;
			while((count = is.read(byteArray))>0){
				ba.write(byteArray , 0 , count);
			}
			is.close();
			return new String(ba.toByteArray() , "UTF-8");
		}
		return "";
	}
	
	private static boolean isFormSubmit(String mediaType){
		return mediaType.indexOf("x-www-form-urlencoded")>=0;
	}
	
	private static boolean isPostJson(String mediaType){
		return mediaType.indexOf("application/json")>=0;
	}
}
