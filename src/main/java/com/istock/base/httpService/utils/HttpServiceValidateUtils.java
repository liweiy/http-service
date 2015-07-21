package com.istock.base.httpService.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.ws.rs.Path;

/**参数类型校验.
 * httpService使用http暴露,导致一些java的类型不支持.
 * 最典型的如Date类型,之类的复杂类型,在传输上最好使用string+格式替代,比如String xxDate,格式为,yyyyMMdd
 * java的Map类型不支持,在map内部虽然可以表示为json格式,但是目前主流的http服务暴露都不支持参数类型为map,以后支持了再说.
 * @author senvon.shi
 *
 */
public abstract class HttpServiceValidateUtils {

	/**校验httpService,参数不能是Map,Date等模糊类型
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public static boolean validateClass(Class clazz) {
		if(clazz != null){
			boolean result = true;
//			Method[] methodArray = clazz.getMethods();
//			for(Method method : methodArray){
//				com.vip.finance.httpService.annotation.Method httpMethod = method.getAnnotation(com.vip.finance.httpService.annotation.Method.class);
//				if(httpMethod != null){
//					Class[] argClass = method.getParameterTypes();
					if(clazz != null){
						if(Map.class.isAssignableFrom(clazz) && Date.class.isAssignableFrom(clazz)){
							throw new RuntimeException("unsupport parameter type , class ["+clazz.getName()+"] the attribute  type is Map");
						}else if(clazz.isPrimitive()){
							return true;
						}else if(clazz.isArray() || Collection.class.isAssignableFrom(clazz)){
							Class componentClass = clazz.getComponentType();
							if(componentClass != null){
								return result && validateClass(componentClass);
							}
						}else {
							Field[] fieldArray = clazz.getFields();
							for(Field field : fieldArray){
								Class fieldClass = field.getType();
								result = result && validateClass(fieldClass);
							}
							return result;
						}
					}
					return result;
//				}
//			}
		}
		return false;
	}
	
	public static boolean validate(Class clazz) {
		if(clazz != null){
			boolean result = true;
			Method[] methodArray = clazz.getMethods();
			for(Method method : methodArray){
				Path httpMethod = method.getAnnotation(Path.class);
				if(httpMethod != null){
					Class[] argClass = method.getParameterTypes();
					if(argClass != null){
						for(Class argType : argClass){
							result = result && validateClass(argType);
						}
						return result;
					}
				}else{
					return result;
				}
			}
		}
		return false;
	}
}
