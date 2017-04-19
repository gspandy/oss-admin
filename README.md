# oss-admin微服务监控管理中心
oss-admin是通过对spring boot admin开源项目进行系列功能增强，并使用docker进行部署的一个系统监控和管理平台。
## 基本功能
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

## 功能增强
+ 开发oss-lib-adminclient, 支持对actuator接口的basic authorization认证
+ oss-admin服务端通过ZuulFilter，携带客户端用户名、密码进行actuator接口交互
+ oss-admin开发了本身的认证和授权功能
+ oss-admin多租户认证（二期开发）


