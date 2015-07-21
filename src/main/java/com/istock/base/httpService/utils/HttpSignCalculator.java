/**
 * 
 */
package com.istock.base.httpService.utils;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**接口鉴权算法.
 * 所有参数,先排序,再加上key,算出MD5
 * 
 * 服务端如果需要鉴权,可以调用这个方法,加上接口验证
 * @author senvon.shi
 *
 */
public abstract class HttpSignCalculator {

	private static Logger logger = LoggerFactory.getLogger(HttpSignCalculator.class);
	/**接口签名计算.
	 * @param paramMap 接口参数map,一般从request中获取.
	 * @param signName 签名的请求参数名.
	 * @param signKey  接口所在系统的密钥.
	 * @return 签名串
	 * 
	 * 假设参数为a=x&b=xx&c=xxx
	 * 
	 * 在Http的请求中,参数的顺序是随机的,会干扰生成的签名
	 * 首先对参数按照参数名进行排序
	 * 重新计算参数,a=x&b=xx&c=xxx,再加上signKey
	 * 这个字符串进行MD5签名
	 * 
	 * 签名计算方式MD5(param1=xxx&param2=xxx&param3=xxxx&systemKey)
	 */
	public static String calculateSign(Map<String , Object> paramMap ,Map<String , String> queryParam,String signKey){
		List<String> paramNameList = new ArrayList<String>();
		if(paramMap != null && !paramMap.isEmpty()){
			for(Entry<String , Object> entry : paramMap.entrySet()){
				paramNameList.add(entry.getValue()+"");
			}
		}
		if(queryParam != null && !queryParam.isEmpty()){
			for(String key : queryParam.keySet()){
				paramNameList.add(key);
			}
		}
		
		Collections.sort(paramNameList);
		
		StringBuffer sb = new StringBuffer();
		for(String paramName : paramNameList){
			sb.append("&").append(paramName).append("=").append(paramMap.get(paramName));
		}
		
		if(signKey != null && signKey.length()>0){
			if(sb.length()>0){
				sb.append("&");
			}
			sb.append(signKey);
		}
		
		if(sb.length()>0){
			sb.deleteCharAt(0);
		}
		logger.info("sign calcuate param:{}" , sb.toString());
		return md5AsHex(sb.toString().getBytes());
	}
	
	/**验证一个请求是否合法
	 * @param request 请求
	 * @param signKey 服务器端的密钥
	 * @return 服务器计算请求的签名
	 */
	public static String calculateSign(HttpServletRequest request , String signKey){
		StringBuffer sb = new StringBuffer(paramSort(request));
		if(signKey != null && signKey.length()>0){
			if(sb.length()>0){
				sb.append("&");
			}
			sb.append(signKey);
		}
		logger.info("sign calcuate param:{}" , sb.toString());
		return md5AsHex(sb.toString().getBytes());
	}
	
	public static String md5AsHex(byte[] content){
		try{
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
	        // 使用指定的字节更新摘要
	        mdInst.update(content);
	        // 获得密文
	        byte[] md = mdInst.digest();
	        return toHex(md);
		}catch(Exception e){
			logger.error("can't support MD5 digest" , e);
		}
		return "";
	}
	private static final char hexDigits[]={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	public static String toHex(byte[] content){
		int j = content.length;
        char str[] = new char[j * 2];
        int k = 0;
        for (int i = 0; i < j; i++) {
            byte byte0 = content[i];
            str[k++] = hexDigits[byte0 >>> 4 & 0xf];
            str[k++] = hexDigits[byte0 & 0xf];
        }
        return new String(str);
	}
	
	private static String paramSort(HttpServletRequest request){
		Enumeration<String> paramEnum = request.getParameterNames();
		List<String> paramNameList = new ArrayList<String>();
		while(paramEnum.hasMoreElements()){
			String paramName = paramEnum.nextElement();
			paramNameList.add(paramName);
		}
		Collections.sort(paramNameList);
		
		StringBuffer sb = new StringBuffer();
		for(String paramName : paramNameList){
			sb.append("&").append(paramName).append("=").append(request.getParameter(paramName));
		}
		sb.deleteCharAt(0);
		
		return sb.toString();
	}
}
