package com.yirendai.oss.environment.admin;

import static com.yirendai.oss.lib.common.BasicAuthUtils.basicAuthHeader;

import de.codecentric.boot.admin.event.ClientApplicationStatusChangedEvent;
import de.codecentric.boot.admin.model.Application;
import de.codecentric.boot.admin.model.StatusInfo;
import de.codecentric.boot.admin.registry.StatusUpdater;
import de.codecentric.boot.admin.registry.store.ApplicationStore;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Created by Yuliang on 16/11/25.
 */
@Slf4j
public class PatchedStatusUpdater extends StatusUpdater {

  private boolean active = false;

  private final ApplicationStore store;

  private ApplicationEventPublisher publisher;

  private final RestTemplate restTemplate;

  @Autowired
  private ClientKeyStore clientKeyStore;

  @Autowired
  private BasicAuthFilter basicAuthFilter;

  public PatchedStatusUpdater(final RestTemplate restTemplate, final ApplicationStore store) {
    super(restTemplate, store);
    this.restTemplate = restTemplate;
    this.store = store;
  }

  @Override
  public void updateStatus(final Application application) {

    if (active) {
      StatusInfo oldStatus = application.getStatusInfo();
      StatusInfo newStatus = queryStatus(application);

      Application newState = Application.create(application).withStatusInfo(newStatus).build();
      store.save(newState);

      if (!newStatus.equals(oldStatus)) {
        publisher.publishEvent(
            new ClientApplicationStatusChangedEvent(newState, oldStatus, newStatus));
      }
    } else {
      log.info("Application not started yet. Application update process not executed");
    }
  }

  public void setActive(final boolean active) {
    log.debug("Setting active property to: {}", active);
    this.active = active;
  }

  public boolean isActive() {
    return this.active;
  }

  @Override
  public void setApplicationEventPublisher(final ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  protected StatusInfo queryStatus(final Application application) {

    StatusInfo resultStatus = StatusInfo.ofUnknown();

    log.trace("Updating status for {}", application);
    try {
      final String url = application.getHealthUrl();
      ClientKey clientKey = this.clientKeyStore.find(application.getId());

      if (clientKey == null) {
        final String infoUrl = getInfoUrl(url);
        clientKey = this.basicAuthFilter.requestForClientKey(application.getId(), infoUrl);
        this.clientKeyStore.save(clientKey);
      }

      final HttpEntity<String> stringHttpEntity = new HttpEntity<>(createBasicAuthHeaders(clientKey));
      final ResponseEntity<Map<String, Object>> response = this.restTemplate.exchange( //
          url, HttpMethod.GET, stringHttpEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);
      log.debug("/health for {} responded with {}", application, response);

      if (response.hasBody() && response.getBody().get("status") instanceof String) {
        resultStatus = StatusInfo.valueOf((String) response.getBody().get("status"));
      } else if (response.getStatusCode().is2xxSuccessful()) {
        resultStatus = StatusInfo.ofUp();
      } else {
        resultStatus = StatusInfo.ofDown();
      }
    } catch (final Exception ex) {
      if ("OFFLINE".equals(application.getStatusInfo().getStatus())) {
        log.debug("Couldn't retrieve status for {}", application, ex);
      } else {
        log.warn("Couldn't retrieve status for {}", application, ex);
      }
      resultStatus = StatusInfo.ofOffline();
    }

    return resultStatus;
  }

  private String getInfoUrl(final String url) {
    return url.substring(0, url.lastIndexOf('/') + 1) + "info";
  }

  HttpHeaders createBasicAuthHeaders(final ClientKey clientKey) {
    HttpHeaders httpHeaders = new HttpHeaders();
    String authHeader = basicAuthHeader(clientKey.getUserName(), clientKey.getPassword());
    httpHeaders.set("Authorization", authHeader);
    return httpHeaders;
  }
}
