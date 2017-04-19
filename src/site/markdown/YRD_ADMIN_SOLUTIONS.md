# yrd-admin开发遇到的问题、解决方案
### 项目背景
#### admin简介
 1. spring boot应用的接口大体分为两类， 业务接口和管理接口，并且有单独的security机制， 管理接口主要由actuator来提供，包括系统的健康状态，环境变量信息，日志级别管理，线程信息，堆栈信息等。对这些接口必须进行严格的权限控制，spring boot通常采用basic authentication的security机制来保护这些接口。
 2. spring boot admin可以理解成一个spring boot应用管理接口的一个UI平台，直接调用客户端的管理接口进行客户端管理。这就要求admin在访问客户端的这些管理接口时，根据不同的客户端设置，提供对应的basic auth的认证信息。而原生spring boot admin并不支持客户端key的获取。
 3. 我们通过开发客户端的jar包的方式，将客户端的用户名、密码进行非对称加密，通过/info接口发布出去，只有admin能够对/info接口中的basic auth信息进行解密和存储。
 4. admin获取客户端的用户名、密码后进行存储，在访问客户端的管理接口时，通过ZuulFilter提供的拦截方式，自动添加客户端的认证信息，从而实现对客户端management endpoints的访问。

#### admin客户端应用注册逻辑
  * admin启动，通过`DiscoveryClientConfiguration`初始化一个`ApplicationDiscoveryListener`，用于监听`InstanceRegisteredEvent`，`HeartbeatEvent`等事件。
  * 当监听到新的客户端信息时，进行admin注册，注册后触发一个`ClientApplicationRegisteredEvent`事件。
  * `StatusUpdateApplicationListener`监听到该事件，调用bean `statusUpdater`的`updateStatus`方法进行状态更新，这里使用从eureka获取的客户端真实`healthUrl`。
  * 完成注册


#### 问题
admin在获取客户端状态时访问从eureka获取到的healthUrl，而该接口需要提供basic auth的，因此原生系统中在不添加任何认证信息的前提下，health接口返回401， admin由于获取不到客户端的真实status信息，默认将客户端状态处理为`DOWN`， 这就是admin首页中所有客户端的Status都为`DOWN`的原因。
### 解决方案
针对访问health返回401的问题，归结为访问该接口之前，添加basic auth信息。
#### AOP方式
 通过aop在调用状态更新前，修改listener中处理函数的入参，将health url改为走zuul代理的方式，从而使用已有的BasicAuthFilter类来添加认证信息，例如把healthUrl从`http://10.106.204.112:8700/health`改为` http://10.106.204.112:8700/api/applications/${serviceId}/health`。具体步骤：
 * 引入依赖`spring-boot-starter-aop`
 * 添加AOP类，使用注解`@Component`和`@Aspect`
 * 定义切点，使用注解`@Pointcut`，并配置切点表达式，指定目标类的目标方法
 * 配置环绕通知`@Around`，并实现修改目标方法入参的逻辑

#### 替换bean的方式
不改变health的url，直接通过替换context现有StatusUpdater bean，改变updateStatus方法的实现逻辑，请求health前，添加认证信息。具体步骤：
 * 定义一个继承自`StatusUpdater`的类
 * 重写`queryStatus`方法，请求healthUrl时，使用RestTemplate添加Basic auth认证
 * 在配置类中将`PatchedStatusUpdater`声明为`@Primary`类型的`StatusUpdater`bean, 从而取代原有系统中的`StatusUpdater`bean。

#### 总结
采用AOP方式由于改变了监听链中的传递参数，违反了编程中immutablity的原则，导致admin启动后的其他数据异常，引入更多潜在的bug。而采用替换特定bean的方法，能清晰准确地定位改动的影响范围，使得对系统的修改更加环保。因此解决该问题采用了后一种方式。

### 代码示例
#### aop方案
`StatusUpdateAspect`切面的定义
```
@Component
@Order(5)
@Aspect
public class StatusUpdateAspect {
  @Pointcut("execution(* de.codecentric.boot.admin.registry.StatusUpdateApplicationListener.onClientApplicationRegistered(de.codecentric.boot.admin.event.ClientApplicationRegisteredEvent))")
  public void updateStatus() {
  }

  @Before("updateStatus()")
  public void onClientApplicationRegistered(JoinPoint joinPoint) {
    LOGGER.info("Begin to update application status.");
  }

  @Around("updateStatus()")
  public Object processEvent(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    ClientApplicationRegisteredEvent clientApplicationRegisteredEvent = (ClientApplicationRegisteredEvent) proceedingJoinPoint.getArgs()[0];
    Application application = clientApplicationRegisteredEvent.getApplication();
    Application.Builder builder = Application.create(application.getName()).withId(application.getId());

    builder.withManagementUrl(application.getManagementUrl())
           .withServiceUrl(application.getServiceUrl())
           .withStatusInfo(application.getStatusInfo())
           .withHealthUrl(getHealthUrl(application));
    Application transformedApplication = builder.build();
    ClientApplicationRegisteredEvent transformedClientApplicationRegisteredEvent = new ClientApplicationRegisteredEvent
        (transformedApplication);
    return proceedingJoinPoint.proceed(new Object[]{transformedClientApplicationRegisteredEvent});
  }
}```
#### 替换容器中bean方法的具体逻辑
定义`StatusUpdater`的子类，重写`updateStatus`方法
```
public class PatchedStatusUpdater extends StatusUpdater{
  private boolean active = false;

  private final ApplicationStore store;

  private ApplicationEventPublisher publisher;

  private final RestTemplate restTemplate;

  @Autowired
  private ClientKeyStore clientKeyStore;

  @Autowired
  private BasicAuthFilter basicAuthFilter;

  public PatchedStatusUpdater(RestTemplate restTemplate, ApplicationStore store) {
    super(restTemplate, store);
    this.restTemplate = restTemplate;
    this.store = store;
  }

  @Override
  public void updateStatus(Application application) {

    if (active) {
      StatusInfo oldStatus = application.getStatusInfo();
      StatusInfo newStatus = queryStatus(application);

      Application newState = Application.create(application).withStatusInfo(newStatus).build();
      store.save(newState);

      if (!newStatus.equals(oldStatus)) {
        publisher.publishEvent(
            new ClientApplicationStatusChangedEvent(newState, oldStatus, newStatus));
      }
    } else {
      log.info("Application not started yet. Application update process not executed");
    }
  }```
在主配置类中声明`StatusUpdater` bean
```
@Primary
@Bean
@ConditionalOnMissingBean
public StatusUpdater statusUpdater() {
  RestTemplate template = new RestTemplate();
  template.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
  template.setErrorHandler(new DefaultResponseErrorHandler() {
    @Override
    protected boolean hasError(HttpStatus statusCode) {
      return false;
    }
  });
  StatusUpdater statusUpdater = new PatchedStatusUpdater(template, applicationStore);
  statusUpdater.setStatusLifetime(adminServerProperties.getMonitor().getStatusLifetime());

  return statusUpdater;
}
```
