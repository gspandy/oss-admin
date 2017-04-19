package com.yirendai.oss.environment.admin;

import static com.google.common.base.Charsets.UTF_8;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.context.RequestContext;
import com.yirendai.oss.lib.common.crypto.EncodeDecryptor;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Map;

/**
 * Created by leo on 16/11/7.
 */
@Slf4j
public class PostFilterForClientInfo extends AbstractZuulFilter {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ClientKeyStore clientKeyStore;

  @Autowired
  private EncodeDecryptor decryptor;

  public PostFilterForClientInfo() {
    super(0, "post");
  }

  @Override
  public boolean shouldFilter() {
    return RequestContext.getCurrentContext().getRequest().getRequestURL().toString().contains("/info");
  }

  @Override
  @SneakyThrows
  public Object run() {
    final RequestContext currentContext = RequestContext.getCurrentContext();
    final String requestUri = currentContext.getRequest().getRequestURI();
    final String serviceId = ClientKey.serviceIdFrom(requestUri);

    final InputStream responseDataStream = currentContext.getResponseDataStream();
    final String json = IOUtils.toString(responseDataStream, UTF_8);
    final Map<String, Object> infoObject = this.objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
    });

    final String[] usernamePassword = ClientKey.usernamePasswordFromInfoObject(infoObject, this.decryptor);
    if (!this.clientKeyStore.isAvailable(serviceId) && usernamePassword != null) {
      final ClientKey clientKey = new ClientKey(serviceId, usernamePassword[0], usernamePassword[1]);
      this.clientKeyStore.save(clientKey);
    }

    currentContext.setResponseDataStream(IOUtils.toInputStream( //
        this.objectMapper.writeValueAsString(infoObject), UTF_8));
    return null;
  }
}
