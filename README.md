# http-service
access the interface use http&amp;json.

开发那么多年,厌烦了CXF,但回头看看好像也没有比较好的框架和协议可以替代CXF

一方面,不是所有的公司都有能力搭建一套dubbo,也不是所有的功能都需要使用cxf的方式暴露

有时候就一个接口暴露,一个接口访问,要引入一个CXF整个包,我认为那是一种有病的行为
恶心CXF的地方主要有以下几点
1.重量级,CXF的引入会让一个应用增加至少10M的大小
2.CXF本身的版本不兼容.之前被CXF蹂躏过,2.3.x版本和2.4.x几2.5以后的版本都是相互不兼容
3.SOAP本身就不是一个标准协议,都是各种杂七杂八的协议拼凑而成,导致CXF依赖的jar包冲突严重

CXF流行的优点不能放弃
1.接口声明,接口调用,方便好用

所以,httpservice,继承CXF的优点,把底层协议从SOAP替换成HTTP+JSON

使用方式
maven引入
&lt;dependency&gt;
		&lt;groupId&gt;com.9istock.base&lt;/groupId&gt;
		&lt;artifactId&gt;http-service&lt;/artifactId&gt;
		&lt;version&gt;1.0.0-SNAPSHOT&lt;/version&gt;
		&lt;scope&gt;test&lt;/scope&gt;
&lt;/dependency&gt;

spring的方式使用client

&lt;bean name="messageService3" class="com.istock.base.httpService.client.HttpProxyFactoryCglib"&gt;
    &lt;property name="targetClass" value="com.ncf.sampleService.MessageService"&gt;&lt;/property&gt;
    &lt;property name="endPoint" value="http://localhost:8081/sampleService"&gt;&lt;/property&gt;
    &lt;property name="signKey" value="xxxxxxxxxxx"&gt;&lt;/property&gt;
    &lt;property name="systemCode" value="002"&gt;&lt;/property&gt;
    &lt;property name="needSign" value="true"&gt;&lt;/property&gt;
&lt;/bean&gt;

在代码里面直接使用@autowaired自动装载接口实现
@Resource(name="messageService3")
private MessageService messageService;

或者使用webClient的方式实现.
MessageService messageService = HttpServiceClient.createClient(MessageService.class, "http://localhost:8081/sampleService", null, false, null, null, null);

接口的声明方式
@Path("message")
public interface MessageService {

	public int sendMessage(MessageInfo message);
	
	@Consumes("application/json;charset=utf-8")
	public int sendMessage2(MessageInfo message);
	
	@Consumes("application/json;charset=utf-8")
	public int sendMessage3(MessageInfo message,@QueryParam("name")String name);
	
	public List<MessageInfo> queryMessage();
	
	public int testList(@QueryParam("list") String[] list);
}

使用rest标准的annotation
@Consumes用来标识request的参数组装类型,如果是json,requestBody整体的提交方式就会变成json格式
如果没有声明方式,默认使用post,接口参数会自动使用post方式提交.参数和参数出现重名,自己解决.
@QueryParam,如果在@Consumes里面,该参数将使用url的queryString提交
如果不在@Consumes里面,只是为了一个变量名.

实现部分不做干预,建议使用springMVC的方式包装
spring的MVC定义controller,可以实现一个接口.

@Controller
@RequestMapping("message")
public class MessageServiceExpose implements MessageService {
	@Override
	@RequestMapping("sendMessage")
	public @ResponseBody int sendMessage(MessageInfo message) {
		return 1;
	}

	@Override
	@RequestMapping("queryMessage")
	public @ResponseBody List<MessageInfo> queryMessage() {
		List<MessageInfo> result = new ArrayList<MessageInfo>();
		MessageInfo m1 = new MessageInfo();
		m1.setContent("this is m1");
		m1.setId(1);
		m1.setReceiver("来点中文1");
		m1.setSender("就是要中文2");
		m1.setType(TypeEnum.SUCCESS);
		result.add(m1);
		
		MessageInfo m2 = new MessageInfo();
		m2.setContent("this is m2");
		m2.setId(2);
		m2.setReceiver("senvon s2");
		m2.setSender("senvon r2");
		m2.setType(TypeEnum.FAIL);
		result.add(m2);
		
		MessageInfo m3 = new MessageInfo();
		m3.setContent("this is m3");
		m3.setId(3);
		m3.setReceiver("senvon s3");
		m3.setSender("senvon r3");
		m3.setType(TypeEnum.SUCCESS);
		result.add(m3);
		
		MessageInfo m4 = new MessageInfo();
		m4.setContent("this is m4");
		m4.setId(4);
		m4.setReceiver("senvon s4");
		m4.setSender("senvon r4");
		m4.setType(TypeEnum.SUCCESS);
		result.add(m4);
		return result;
	}

	@Override
	@RequestMapping("sendMessage2")
	public @ResponseBody int sendMessage2(@RequestBody MessageInfo message) {
		System.out.println(message.getType());
		return 2;
	}

	@Override
	@RequestMapping("sendMessage3")
	public @ResponseBody int sendMessage3(@RequestBody MessageInfo message, @RequestParam String name) {
		// TODO Auto-generated method stub
		System.out.println("====================="+name);
		return 3;
	}

	@Override
	@RequestMapping("testList")
	public @ResponseBody int testList(String[] list) {
		System.out.println(ToStringBuilder.reflectionToString(list));
		return 1;
	}
}

如果超过50个应用,效率要求比较高,强烈建议使用dubbo
