package cn.home1.oss.environment.admin;

import de.codecentric.boot.admin.config.AdminServerProperties;
import de.codecentric.boot.admin.config.EnableAdminServer;
import de.codecentric.boot.admin.registry.StatusUpdater;
import de.codecentric.boot.admin.registry.store.ApplicationStore;
import de.codecentric.boot.admin.web.client.ApplicationOperations;

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

import cn.home1.oss.boot.autoconfigure.AppProperties;
import cn.home1.oss.lib.common.crypto.Cryptos;
import cn.home1.oss.lib.common.crypto.EncodeDecryptor;
import cn.home1.oss.lib.common.crypto.KeyExpression;

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
  private Environment environment;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private ApplicationOperations applicationOperations;

  @Autowired
  private AdminServerProperties adminServerProperties;

  @Autowired
  private ApplicationStore applicationStore;

  public static void main(final String[] args) {
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
  public RestTemplate restTemplate(final RestTemplateBuilder builder) {
    return builder.build();
  }

  @Bean
  public EncodeDecryptor decryptor() {
    final String adminPrivateKey = this.environment.getProperty("app.adminPrivateKey");
    return Cryptos.decryptor(new KeyExpression(adminPrivateKey));
  }

  @Primary
  @Bean
  @ConditionalOnMissingBean
  public StatusUpdater statusUpdater() {

    log.info("Registering patched version of de.codecentric.boot.admin.registry.StatusUpdates");

    final RestTemplate restTemplate = new RestTemplate();
    restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
      @Override
      protected boolean hasError(final HttpStatus statusCode) {
        return false;
      }
    });

    final StatusUpdater statusUpdater = new PatchedStatusUpdater(this.applicationStore, this.applicationOperations, restTemplate);
    statusUpdater.setStatusLifetime(this.adminServerProperties.getMonitor().getStatusLifetime());

    return statusUpdater;
  }

  @Override
  public void onApplicationEvent(final ContextRefreshedEvent event) {
    final PatchedStatusUpdater statusUpdater = (PatchedStatusUpdater) this.applicationContext.getBean("statusUpdater");
    statusUpdater.setActive(true);
  }
}
