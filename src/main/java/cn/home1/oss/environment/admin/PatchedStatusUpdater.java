package cn.home1.oss.environment.admin;

import static cn.home1.oss.lib.common.BasicAuthUtils.basicAuthHeader;
import static org.springframework.http.HttpMethod.GET;

import de.codecentric.boot.admin.event.ClientApplicationStatusChangedEvent;
import de.codecentric.boot.admin.model.Application;
import de.codecentric.boot.admin.model.StatusInfo;
import de.codecentric.boot.admin.registry.StatusUpdater;
import de.codecentric.boot.admin.registry.store.ApplicationStore;
import de.codecentric.boot.admin.web.client.ApplicationOperations;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by Yuliang on 16/11/25.
 */
@Slf4j
public class PatchedStatusUpdater extends StatusUpdater {

  private final ApplicationOperations applicationOps;

  private final RestTemplate restTemplate;

  private final ApplicationStore store;

  private boolean active = false;

  private ApplicationEventPublisher publisher;

  @Autowired
  private BasicAuthFilter basicAuthFilter;
  @Autowired
  private ClientKeyStore clientKeyStore;

  public PatchedStatusUpdater( //
      final ApplicationStore store, //
      final ApplicationOperations applicationOps, //
      final RestTemplate restTemplate
  ) {
    super(store, applicationOps);
    this.applicationOps = applicationOps;
    this.store = store;
    this.restTemplate = restTemplate;
  }

  @Override
  public void updateStatus(final Application application) {

    if (this.active) {
      final StatusInfo oldStatus = application.getStatusInfo();
      final StatusInfo newStatus = queryStatus(application);

      final Application newState = Application.create(application.getName()).withStatusInfo(newStatus).build();
      this.store.save(newState);

      if (!newStatus.equals(oldStatus)) {
        this.publisher.publishEvent(new ClientApplicationStatusChangedEvent(newState, oldStatus, newStatus));
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

  private static final Class<Map<String, Serializable>> RESPONSE_TYPE_MAP = (Class<Map<String, Serializable>>) (Class<?>) Map.class;

  @Override
  protected StatusInfo queryStatus(final Application application) {
//    StatusInfo resultStatus = StatusInfo.ofUnknown();
//
//    log.trace("Updating status for {}", application);
//    try {
//      final String url = application.getHealthUrl();
//      ClientKey clientKey = this.clientKeyStore.find(application.getId());
//
//      if (clientKey == null) {
//        final String infoUrl = getInfoUrl(url);
//        clientKey = this.basicAuthFilter.requestForClientKey(application.getId(), infoUrl);
//        this.clientKeyStore.save(clientKey);
//      }
//
//      final HttpEntity<String> httpEntity = new HttpEntity<>(createBasicAuthHeaders(clientKey));
//      final ResponseEntity<Map<String, Serializable>> response = this.operations.exchange(url, GET, httpEntity, RESPONSE_TYPE_MAP);
//      log.debug("/health for {} responded with {}", application, response);
//
//      if (response.hasBody() && response.getBody().get("status") instanceof String) {
//        resultStatus = StatusInfo.valueOf((String) response.getBody().get("status"));
//      } else if (response.getStatusCode().is2xxSuccessful()) {
//        resultStatus = StatusInfo.ofUp();
//      } else {
//        resultStatus = StatusInfo.ofDown();
//      }
//    } catch (final Exception ex) {
//      if ("OFFLINE".equals(application.getStatusInfo().getStatus())) {
//        log.debug("Couldn't retrieve status for {}", application, ex);
//      } else {
//        log.warn("Couldn't retrieve status for {}", application, ex);
//      }
//      resultStatus = StatusInfo.ofOffline();
//    }
//
//    return resultStatus;

    try {
      final String url = application.getHealthUrl();
      ClientKey clientKey = this.clientKeyStore.find(application.getId());
      if (clientKey == null) {
        final String infoUrl = getInfoUrl(url);
        clientKey = this.basicAuthFilter.requestForClientKey(application.getId(), infoUrl);
        this.clientKeyStore.save(clientKey);
      }
      final HttpEntity<String> httpEntity = new HttpEntity<>(createBasicAuthHeaders(clientKey));
      final ResponseEntity<Map<String, Serializable>> response = this.restTemplate.exchange( //
          url, GET, httpEntity, RESPONSE_TYPE_MAP);

      return convertStatusInfo(response);
    } catch (final Exception ex) {
      if ("OFFLINE".equals(application.getStatusInfo().getStatus())) {
        log.debug("Couldn't retrieve status for {}", application, ex);
      } else {
        log.info("Couldn't retrieve status for {}", application, ex);
      }
      return convertStatusInfo(ex);
    }
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
