package com.yirendai.oss.environment.admin.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by leo on 17/1/10.
 * This class is to show the metrics data of a rest api as an example.
 */
@RestController
public class GreetingController {
  @RequestMapping("/greeting")
  public String greeting() throws InterruptedException {
    Thread.sleep(1000);
    return "Spring boot admin.";
  }
}
