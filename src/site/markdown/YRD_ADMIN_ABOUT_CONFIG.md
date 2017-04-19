# yrd-admin服务端配置
## 配置说明
  * `spring.boot.admin.url`
    * The admin server urls to register at。即需要注册的目标admin的url。
  * `spring.boot.admin.autoDeregistration`
      * Enable automatic deregistration on shutdown。
## 常见问题
  * 本地启动admin后，会有两个admin实例，其中一个是从eureka获取的实例，另一个不知从哪里冒出来，并且在不断地重复注册、取消注册的操作。
      * 原因：这是因为采用了admin自身直连admin的错误配置。应该取消该配置`spring.boot.admin.url`。