package com.yirendai.oss.environment.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by leo on 16/11/28.
 */
@Controller
public class AdminController {
  @RequestMapping("/login")
  public String login() {
    return "login";
  }
}
