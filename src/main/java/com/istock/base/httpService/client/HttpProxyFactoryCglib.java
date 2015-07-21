package com.istock.base.httpService.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartFactoryBean;

/**利用cglib,创建一个客户端.
 * @author senvon.shi
 *
 */
public class HttpProxyFactoryCglib implements SmartFactoryBean {

	private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private String targetClass;//代理的接口
	private String endPoint;//暴露地址
	
	private String signName = "sign";//默认的签名在header中的key
	private boolean needSign =false;//是否需要签名
	private String signKey ;//签名值
	private String systemCode;//系统编号
	private String methodFix;//后缀
	
	public void setTargetClass(String targetClass) {
		this.targetClass = targetClass;
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}

	@Override
	public Class<?> getObjectType() {
		try {
			if(targetClass != null &&targetClass.trim().length()>0){
				return Class.forName(targetClass);
			}
		} catch (ClassNotFoundException e) {
			logger.error("can't found the proxy class:["+targetClass+"]" , e);
		}
//		return Object.class;
		return null;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public Object getObject() throws Exception {
		if(targetClass == null || targetClass.trim().length()<=0){
			throw new Exception("please check config the attribute targetClass is missing");
		}
		
		Class<?> clazz = Class.forName(targetClass);
		MethodInvoke invoker = new MethodInvoke();
		invoker.setEndPoint(endPoint);
		invoker.setMethodFix(methodFix);
		invoker.setNeedSign(needSign);
		invoker.setSignKey(signKey);
		invoker.setSignName(signName);
		invoker.setSystemCode(systemCode);
		invoker.setTargetClass(clazz);
		return invoker.create();
	}

	public void setSignName(String signName) {
		this.signName = signName;
	}

	public void setNeedSign(boolean needSign) {
		this.needSign = needSign;
	}

	public void setSignKey(String signKey) {
		this.signKey = signKey;
	}

	public void setSystemCode(String systemCode) {
		this.systemCode = systemCode;
	}

	@Override
	public boolean isPrototype() {
		return false;
	}

	@Override
	public boolean isEagerInit() {
		return false;
	}

	public void setMethodFix(String methodFix) {
		this.methodFix = methodFix;
	}
}
