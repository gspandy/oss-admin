package com.yirendai.oss.environment.admin;

import com.netflix.zuul.ZuulFilter;

/**
 * Created by leo on 17/1/6.
 */
public abstract class AbstractZuulFilter extends ZuulFilter {
  private final int filterOrder;
  private final String filterType;

  protected AbstractZuulFilter(final int filterOrder, final String filterType) {
    this.filterOrder = filterOrder;
    this.filterType = filterType;
  }

  @Override
  public int filterOrder() {
    return this.filterOrder;
  }

  @Override
  public String filterType() {
    return this.filterType;
  }

  @Override
  public boolean shouldFilter() {
    return true;
  }
}
