package com.yirendai.oss.environment.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Created by leo on 16/11/28.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  private SecurityProperties securityProperties;

  private static final String ADMIN_ROLE_NAME = "ADMIN";
  private static final String USER_ROLE_NAME = "USER";
  private static final int USER_ROLE_FIRST_INDEX = 0;
  private static final int USER_ROLE_SECOND_INDEX = 1;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    /**
     * Below config is a temporary solution to protect management endpoints.
     */
    http
        .authorizeRequests()
        .antMatchers( //
            "/health", //
            "/env", //
            "/metrics", //
            "/jolokia", //
            "/dump", //
            "/shutdown", //
            "/beans", //
            "/trace" //
        )
        .fullyAuthenticated().and()
        .httpBasic();

    http
      .csrf().disable()
      .authorizeRequests()
        .antMatchers("/", "/index.html").hasRole(USER_ROLE_NAME)
        .antMatchers("/api/applications/**/env",
            "/api/applications/**/jolokia",
            "/api/applications/**/heapdump")
        .hasRole(ADMIN_ROLE_NAME)
        .anyRequest().permitAll()
        .and()
      .formLogin()
        .loginPage("/login")
        .defaultSuccessUrl("/")
        .permitAll()
        .and()
      .logout()
        .permitAll();
  }

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    SecurityProperties.User user = securityProperties.getUser();
    auth
        .inMemoryAuthentication()
        .withUser(user.getName()).password(user.getPassword()).roles(user.getRole().get(USER_ROLE_FIRST_INDEX), user
        .getRole().get(USER_ROLE_SECOND_INDEX)).and()
        .withUser("user").password("user_pass").roles(USER_ROLE_NAME, ADMIN_ROLE_NAME).and()
        .withUser("yrd").password("yrd").roles(USER_ROLE_NAME);
  }
}
