package com.yirendai.oss.environment.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.zuul.context.RequestContext;
import com.yirendai.oss.lib.common.BasicAuthUtils;
import com.yirendai.oss.lib.common.crypto.EncodeDecryptor;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

/**
 * Description: This filter is to add basic authentication headers to requests for management endpoints
 * from the admin client.
 * Created by Yuliang Jin on 16/10/26.
 */
@Slf4j
public class BasicAuthFilter extends AbstractZuulFilter{

  @Autowired
  private ClientKeyStore clientKeyStore;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private EncodeDecryptor decryptor;

  public BasicAuthFilter() {
    super(0, "pre");
  }

  @SneakyThrows
  @Override
  public Object run() {
    final RequestContext currentContext = RequestContext.getCurrentContext();
    final String requestUri = currentContext.getRequest().getRequestURI();
    if (requestUri.contains("/info")) {
      log.info(requestUri + " passed basic auth filter.");
      return null;
    }
    log.info("Basic Auth Filter is working...");
    log.info("Request URI is {}.", requestUri);
    log.debug("Path info for RUI {} is: {}.", requestUri, currentContext.getRequest().getPathInfo());

    final String serviceId = ClientKey.serviceIdFrom(requestUri);
    ClientKey clientKey = this.clientKeyStore.find(serviceId);
    if (clientKey == null) {
      log.debug("Service '{}' is requesting for client key.", serviceId);
      final String infoUrl = getInfoUrl(currentContext.getRequest().getRequestURL().toString());
      try {
        clientKey = requestForClientKey(serviceId, infoUrl);
      } catch (final Exception ex) {
        log.debug("Service '{}' got error when requesting for client key.", serviceId, ex);
      }
      this.clientKeyStore.save(clientKey);
    }

    if (clientKey != null) {
      String basicAuthCode = BasicAuthUtils.basicAuthHeader(clientKey.getUserName(), clientKey.getPassword());
      currentContext.addZuulRequestHeader("Authorization", basicAuthCode);
    }
    return null;
  }

  /**
   * @param serviceId serviceId
   * @param infoUrl   url for /info endpoint
   * @return ClientKey of the target admin client.
   */
  public ClientKey requestForClientKey(final String serviceId, final String infoUrl) throws IOException {
    final String json = restTemplate.getForObject(infoUrl, String.class);
    final Map<String, Object> infoObject = this.objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
    });

    final String[] usernamePassword = ClientKey.usernamePasswordFromInfoObject(infoObject, this.decryptor);
    final ClientKey clientKey;
    if (usernamePassword != null) {
      clientKey = new ClientKey(serviceId, usernamePassword[0], usernamePassword[1]);
    } else {
      clientKey = new ClientKey(serviceId, "", "");
    }
    return clientKey;
  }

  private String getInfoUrl(String requestUri) {
    return requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + "info";
  }
}
