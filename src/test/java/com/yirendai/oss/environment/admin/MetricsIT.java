package com.yirendai.oss.environment.admin;

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by leo on 17/1/10.
 * Integration test for metrics of RESTful api.
 */
@ActiveProfiles("it.env")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@Slf4j
public class MetricsIT {
  @Autowired
  private TestRestTemplate testRestTemplate;

  private final String targetCounterString = "\"counter.servo.string_com.yirendai.oss.environment.admin.controller" +
      ".greetingcontroller.greeting()-invokenum\":1";


  @Test
  public void metricsTest() {
    this.testRestTemplate.withBasicAuth("admin", "admin_pass");
    String greeting = this.testRestTemplate.getForObject("/greeting", String.class);
    String metrics = this.testRestTemplate.withBasicAuth("admin", "admin_pass").getForObject("/metrics", String.class);
    assertThat(metrics).contains("gauge.servo.string_com.yirendai.oss.environment.admin.controller.greetingcontroller" +
        ".greeting()-invoketime");
    assertThat(metrics).contains(targetCounterString);
  }
}
