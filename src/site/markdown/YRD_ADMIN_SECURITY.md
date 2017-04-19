# yrd-admin security说明文档
yrd-admin是一个比较特殊的应用，特殊在于除了系统本身的认证和授权之外，yrd-admin作为一个监控和管理的其他系统的服务（也对自身进行监控和管理），会定时地去请求被监控系统的actuator
接口，这也需要做一些基本的认证和授权处理。比如，yrd-admin请求本身的`/health`接口，就需要使用basic认证的方式去请求。本文档将从两方面说明yrd-admin所涉及的所有security配置。
## yrd-admin actuator的basic authorization
默认spring boot应用，包括yrd-admin，只要加入`spring-boot-starter-security`依赖后，就添加了basic authentication认证。actuator 
endpoints也会在该security的保护机制下。该配置由类`AuthenticationManagerConfiguration`创建，采用basic 
auth内存认证。但是在加入yrd-admin的form表达认证后，actuator的basic auth内存认证就失效了。因此需要做好相应的处理。
## yrd-admin本身的认证和授权
作为一个管理系统，需要提供基本的认证和授权支持，来规范哪些用户对系统资源有查看权限，哪些用户对系统资源有修改权限。目前我们采用form表单内存认证的方式，创建了三个固定的用户，有两种角色USER，ADMIN。
## 注意事项
目前这种解决方案是临时的，actuator endpoints的security信息要同时在application
.yml和以下`WebSecurityConfig`类中配置。因为admin系统自身在请求`/health`等接口时，要去系统properties中查找`security.user.name`，`security.user
.password`。而认证时是读取的`WebSecurityConfig`类中定义的用户信息，所以目前两处需要保持一致。
## security配置
包括部分actuator endpoints的basic auth认证和yrd-admin的form表单认证。

    @Configuration
    @EnableWebSecurity
    public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
      @Override
      protected void configure(HttpSecurity http) throws Exception {
    
        /**
         * Below config is a temporary solution to protect management endpoints.
         */
        http
            .authorizeRequests()
            .antMatchers("/health", "/env", "/metrics", "/jolokia", "/")
            .fullyAuthenticated().and()
            .httpBasic();
        http
          .csrf().disable()
          .authorizeRequests()
            .antMatchers("/", "/index.html").hasRole("USER")
            .antMatchers("/api/applications/**/env",
                "/api/applications/**/jolokia",
                "/api/applications/**/heapdump")
            .hasRole
            ("ADMIN")
            .anyRequest().permitAll()
            .and()
          .formLogin()
            .loginPage("/login")
            .permitAll()
            .and()
          .logout()
            .permitAll();
      }
    
      @Autowired
      public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .inMemoryAuthentication()
            .withUser("user").password("123456").roles("USER", "ADMIN").and()
            .withUser("yrd").password("123456").roles("USER").and()
            .withUser("admin").password("123456").roles("USER", "ADMIN");
      }
    }
    
## spring boot admin客户端security说明
作为spring boot admin的客户端，可能采用多种security机制，比如原生的spring security, 
oss的lib-security等。而所有要接入yrd-admin的客户端应用，有一个共同的前提，对management endpoints的认证的用户名密码都是从应用的`security.user.name`， `security
.user.password`这两个配置去获取，任何启用了security，又不设置该用户的都将接入yrd-admin失败。因此对客户端有两个基本要求： 1. 配置`security.user.name`， `security
.user.password`； 2. 第一点中配置的用户要能通过security的认证。（默认实现了`WebSecurityConfigurerAdapter`的配置类，`security.user.name`， `security
.user.password`的配置用户将失效，不能通过security认证，参考yrd-admin解决方法。）
### basic authentication配置
通过`security.user.name`， `security.user.password`配置security，提供了最基本的安全认证，目前可以很容易接入spring boot admin。
### oss lib-security
如果客户端使用该lib进行security配置，客户端必要的security配置，如果要接入yrd-admin，同意需要配置`security.user.name`， `security.user.password`。
### spring security + basic authentication配置
参考yrd-admin当前的配置方法， yrd-admin添加了继承自WebSecurityConfigurerAdapter的security配置类，导致默认的`security.user.name`， `security.user
.password`配置失效，所以要进行简单的处理，将失效的配置用户加入到新的security配置类中：

      @Autowired
      public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        SecurityProperties.User user = securityProperties.getUser();
        auth
            .inMemoryAuthentication()
            .withUser(user.getName()).password(user.getPassword()).roles(user.getRole().get(USER_ROLE_FIRST_INDEX), user
            .getRole().get(USER_ROLE_SECOND_INDEX)).and()
            .withUser("admin").password("admin_pass").roles(USER_ROLE_NAME, ADMIN_ROLE_NAME).and()
            .withUser("yrd").password("yrd").roles(USER_ROLE_NAME);
      }