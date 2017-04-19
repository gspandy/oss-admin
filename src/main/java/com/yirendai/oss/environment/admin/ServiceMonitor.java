package com.yirendai.oss.environment.admin;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.stereotype.Component;

/**
 * Created by leo on 17/1/10.
 * This class is to gather the times a method is invoked and the execution time for a method.
 */
@Aspect
@Component
public class ServiceMonitor {

  private final CounterService counterService;

  private final GaugeService gaugeService;

  public ServiceMonitor(CounterService counterService, GaugeService gaugeService) {
    this.counterService = counterService;
    this.gaugeService = gaugeService;
  }

  @Autowired
  public ServiceMonitor serviceMonitor(final CounterService counterService, final GaugeService gaugeService) {
    return new ServiceMonitor(counterService, gaugeService);
  }

  @Before("execution(* com.yirendai.oss.environment.admin.controller.*.*(..))")
  public void countServiceInvoke(JoinPoint joinPoint) {
    this.counterService.increment("meter." + joinPoint.getSignature() + "-invokeNum");
  }

  @Around("execution(* com.yirendai.oss.environment.admin.controller.*.*(..))")
  public Object latencyService(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    long start = System.currentTimeMillis();
    Object proceed = proceedingJoinPoint.proceed();
    long end = System.currentTimeMillis();
    this.gaugeService.submit( proceedingJoinPoint.getSignature().toString() + "-invokeTime", (double)end - start);
    return proceed;
  }
}
