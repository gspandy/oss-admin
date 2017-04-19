package com.yirendai.oss.environment.admin;

import com.yirendai.oss.boot.autoconfigure.AppProperties;
import com.yirendai.oss.lib.common.crypto.Cryptos;
import com.yirendai.oss.lib.common.crypto.EncodeDecryptor;
import com.yirendai.oss.lib.common.crypto.KeyExpression;

import de.codecentric.boot.admin.config.AdminServerProperties;
import de.codecentric.boot.admin.config.EnableAdminServer;
import de.codecentric.boot.admin.registry.StatusUpdater;
import de.codecentric.boot.admin.registry.store.ApplicationStore;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAutoConfiguration
@EnableAdminServer
@EnableDiscoveryClient
@EnableZuulProxy
@EnableConfigurationProperties(value = {AppProperties.class})
@SpringBootApplication
@Slf4j
public class AdminApplication implements ApplicationListener<ContextRefreshedEvent> {

  @Autowired
  Environment environment;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  AdminServerProperties adminServerProperties;

  @Autowired
  private ApplicationStore applicationStore;

  public static void main(String[] args) {
    SpringApplication.run(AdminApplication.class, args);
  }

  @Bean
  @ConditionalOnMissingBean
  public ClientKeyStore clientKeyStoreListener() {
    return new ClientKeyStore();
  }

  @Bean
  public BasicAuthFilter basicAuthFilter() {
    return new BasicAuthFilter();
  }

  @Bean
  public PostFilterForClientInfo postFilterForClientInfo() {
    return new PostFilterForClientInfo();
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }

  @Bean
  public EncodeDecryptor decryptor() {
    String adminPrivateKey = this.environment.getProperty("app.adminPrivateKey");
    return Cryptos.decryptor(new KeyExpression(adminPrivateKey));
  }

  @Primary
  @Bean
  @ConditionalOnMissingBean
  public StatusUpdater statusUpdater() {

    log.info("Registering patched version of de.codecentric.boot.admin.registry.StatusUpdates");

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

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    PatchedStatusUpdater statusUpdater = (PatchedStatusUpdater) applicationContext.getBean("statusUpdater");
    statusUpdater.setActive(true);
  }
}
