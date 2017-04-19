# yrd-admin功能介绍
## 基本功能介绍
+ Show health status
+ Show details, like
  + JVM & memory metrics
  + Counter & gauge metrics
  + Datasource metrics
  + Cache metrics
+ Show build-info number
* Follow and download logfile
* View jvm system- & environment-properties
* Support for Spring Cloud's postable /env- &/refresh-endpoint
* Easy loglevel management (currently for Logback only)
* Interact with JMX-beans
* View thread dump
* View traces
* Hystrix-Dashboard integration
* Download heapdump
* Notification on status change (via mail, Slack, Hipchat, ...)
* Event journal of status changes (non persistent)
## spring boot admin图例说明
1. admin 客户端首页
![configserver.png](src/readme/spring-boot-admin-1.png)
2. 首页memory、jvm等信息
![configserver.png](src/readme/spring-boot-admin-2.png)
3. Metrics标签页面
![configserver.png](src/readme/spring-boot-admin-3.png)
4. Environment标签页面
![configserver.png](src/readme/spring-boot-admin-4.png)
5. Logging标签页面
![configserver.png](src/readme/spring-boot-admin-5.png)
6. JMX标签页面
![configserver.png](src/readme/spring-boot-admin-6.png)
7. Threads签页面
![configserver.png](src/readme/spring-boot-admin-7.png)
8. Trace标签页面
![configserver.png](src/readme/spring-boot-admin-8.png)
