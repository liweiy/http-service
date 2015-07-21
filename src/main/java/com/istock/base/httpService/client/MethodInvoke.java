package com.istock.base.httpService.client;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.istock.base.httpService.utils.HttpServiceValidateUtils;

public class MethodInvoke implements MethodInterceptor{

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Class<?> targetClass;//代理的接口
	private String endPoint;//暴露地址
	
	private String signName = "sign";//默认的签名在header中的key
	private boolean needSign =false;//是否需要签名
	private String signKey ;//签名值
	private String systemCode;//系统编号
	private String methodFix;//后缀
	
	private HttpProcessor processor;
	
	public Object create() throws Exception{
		HttpServiceValidateUtils.validate(targetClass);
		processor = new HttpProcessor(targetClass ,endPoint ,signName , needSign ,signKey, systemCode , methodFix);
		if(processor == null){
			throw new NoSuchBeanDefinitionException(targetClass , " can't inilize ");
		}
		Enhancer enhancer = new Enhancer();
		enhancer.setInterfaces(new Class[]{targetClass});
		enhancer.setCallback(this);
		return enhancer.create();
	}
	
	@Override
	public Object intercept(Object target, Method targetMethod, Object[] params,MethodProxy methodProxy) throws Throwable {
		//当出现一个动态调用的时候,判断当前方法是不是接口中的方法,如果不是接口方法,直接使用原始的调用
		//当spring在初始化的时候,会调用类型equals,hashCode等方法
		if (!Modifier.isAbstract(targetMethod.getModifiers()))
		{
			return methodProxy.invokeSuper(target, params);
		}
		
		try{
			return processor.process(targetMethod, params);
		}catch(Exception e){
			logger.error("httpService access error " , e);
		}
		return null;
	}
	
	public Class<?> getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public void setEndPoint(String endPoint) {
		this.endPoint = endPoint;
	}

	public String getSignName() {
		return signName;
	}

	public void setSignName(String signName) {
		this.signName = signName;
	}

	public boolean isNeedSign() {
		return needSign;
	}

	public void setNeedSign(boolean needSign) {
		this.needSign = needSign;
	}

	public String getSignKey() {
		return signKey;
	}

	public void setSignKey(String signKey) {
		this.signKey = signKey;
	}

	public String getSystemCode() {
		return systemCode;
	}

	public void setSystemCode(String systemCode) {
		this.systemCode = systemCode;
	}

	public String getMethodFix() {
		return methodFix;
	}

	public void setMethodFix(String methodFix) {
		this.methodFix = methodFix;
	}

}
