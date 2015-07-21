package com.istock.base.httpService.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import com.alibaba.fastjson.JSON;
import com.istock.base.httpService.utils.HttpConnectionUtils;
import com.istock.base.httpService.utils.HttpSignCalculator;

/**Http执行器.
 * 对接口声明进行扫描,确定需要访问的地址和参数组装方式.
 * 调用Http 调用接口.
 * @author senvon.shi
 *
 */
public class HttpProcessor {
	
	private Class<?> targetClass;
	private String endPoint;
	
	private String signName;
	private boolean needSign;
	private String signKey ;
	private String systemCode;
	private String methodFix = ".htm";
	
	public HttpProcessor(Class<?> targetClass , String endPoint ,String signName , boolean needSign ,String signKey, String systemCode , String methodFix){
		this.targetClass = targetClass;
		this.endPoint = endPoint;
		this.signKey = signKey;
		this.signName = signName;
		this.needSign = needSign;
		this.systemCode = systemCode;
		if(methodFix != null && methodFix.trim().length()>0){
			this.methodFix = methodFix;
		}
	}
	
	/**处理请求.
	 * @param method 调用的方法.
	 * @param params 调用方法的参数.
	 * @return
	 */
	public Object process(Method method ,  Object[] params) throws Exception{
		if(method == null){
			throw new Exception("the Method is null , please check the "+targetClass+" version");
		}
		
		//使用默认的方法名和后缀.
		String methodUrl = method.getName() + this.methodFix;
//		String methodUrl = method.getName();
		if(method.isAnnotationPresent(Path.class)){
			Path resource = method.getAnnotation(Path.class);
			//如果方法上存在Resource的注解申明,使用注解.
			if(resource.value() != null && resource.value().length()>0){
				methodUrl = resource.value();
			}
		}
		
		//寻找类上的注解
		if(targetClass.isAnnotationPresent(Path.class)){
			Path resource = (Path)targetClass.getAnnotation(Path.class);
			//如果类上的Resource注解存在,加在方法url的最前面.
			if(resource.value() != null && resource.value().length()>0){
				methodUrl = appendUrl(resource.value() , methodUrl);
			}
		}
		
		String mediaType = "application/x-www-form-urlencoded";
		if(targetClass.isAnnotationPresent(Consumes.class)){
			Consumes consume = (Consumes) targetClass.getAnnotation(Consumes.class);
			if(consume != null && consume.value().length>0){
				mediaType = consume.value()[0];
			}
		}
		
		
		//默认使用POST方法提交
		String restfulMethod = HttpMethod.POST;
		if(method.isAnnotationPresent(HttpMethod.class)){
			//如果方法上存在Method的注解,使用注解.
			HttpMethod httpMethod = method.getAnnotation(HttpMethod.class);
			restfulMethod = httpMethod.value();
		}
		if(method.isAnnotationPresent(Consumes.class)){
			Consumes consume = (Consumes) method.getAnnotation(Consumes.class);
			if(consume != null && consume.value().length>0){
				mediaType = consume.value()[0];
			}
		}
		
		//寻找方法上的所有参数的注解.
		Annotation[][]parameterAnnotations = method.getParameterAnnotations();
		if(params != null){
			Map<String , Object> bodyParams = new HashMap<String , Object>();
			Map<String , String> queryParams = new HashMap<String , String>();
			for(int i =0 ; i<params.length ; i++){
				Annotation[] parameterAnnotation = parameterAnnotations[i];
				String paramName = null;
				
				Object paramValue = params[i];
				QueryParam queryParamAnnotation = findAnnotation(parameterAnnotation , QueryParam.class);
				if(queryParamAnnotation != null){
					paramName = queryParamAnnotation.value();
					if(paramValue == null){
						queryParams.put(paramName, "");
					}else if(paramValue.getClass().isArray() || paramValue instanceof Collection){
						Object[] array = null;
						if(paramValue.getClass().isArray()){
							array = (Object[]) paramValue;
						}else{
							Collection<Object> collection = (Collection)paramValue;
							array = collection.toArray(new Object[0]);
						}
						StringBuffer sb = new StringBuffer();
						for(Object item: array){
							if(sb.length()<=0){
								sb.append(item);
							}else{
								sb.append(",").append(item);
							}
						}
						queryParams.put(paramName, sb.toString());
					}else{
						queryParams.put(paramName, paramValue + "");
					}
					
				}else{
					bodyParams.clear();
					bodyParams.put(paramName, paramValue);
				}
			}
			
			//生成最终的访问请求url
			String requestUrl = appendUrl(this.endPoint , methodUrl);
			
			//接口签名写在Http请求的head里面.
			Map<String , String> headerMap = new HashMap<String , String>();
			if(needSign){
				//String sign = calculateSign(urlParams);
				String sign = HttpSignCalculator.calculateSign(bodyParams,queryParams ,  this.signKey);
				headerMap.put(signName, sign);
				headerMap.put("systemCode", systemCode);
			}
			String json = HttpConnectionUtils.loadJson(requestUrl,  queryParams,bodyParams ,headerMap, restfulMethod , mediaType);
			
			//根据返回值返回对应的对象.
			Class<?> returnType = method.getReturnType();
			if(returnType != null){
				if(returnType.isArray() || returnType.isAssignableFrom(Collection.class)){
					return JSON.parseArray(json, returnType.getComponentType());
				}
				return JSON.parseObject(json , returnType);
			}
		}
		return null;
	}
	
	private <T> T findAnnotation(Annotation[] annotationArray , Class<T> clazz){
		if(annotationArray != null){
			for(Annotation an : annotationArray){
				if(clazz.isAssignableFrom(an.annotationType())){
					return (T)an;
				}
			}
		}
		return null;
	}
	
	
	
	/**2个url相加.去掉中间的"/"
	 * @param firstUrl
	 * @param secondUrl
	 * @return
	 */
	private String appendUrl(String firstUrl , String secondUrl){
		StringBuffer sb = new StringBuffer(firstUrl);
		if(firstUrl.endsWith("/")){
			if(secondUrl.startsWith("/")){
				sb.append(secondUrl.substring(1));
			}else{
				sb.append(secondUrl);
			}
		}else{
			if(secondUrl.startsWith("/")){
				sb.append(secondUrl);
			}else{
				sb.append("/"+secondUrl);
			}
		}
		
		return sb.toString();
	}
}
