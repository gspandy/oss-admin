package com.yirendai.oss.environment.admin;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Yuliang Jin on 16/10/26.
 */
public class ClientKeyStore {

  private final ConcurrentMap<String, ClientKey> keysMap = new ConcurrentHashMap<>();


  public ClientKey save(ClientKey clientKey) {
    return keysMap.put(clientKey.getServiceId(), clientKey);
  }

  public Collection<ClientKey> findAll() {
    return keysMap.values();
  }

  public ClientKey find(String serviceId) {
    return keysMap.get(serviceId);
  }

  public ClientKey delete(String serviceId) {
    return keysMap.remove(serviceId);
  }

  public boolean isAvailable(String serviceId) {
    ClientKey clientKey = find(serviceId);
    return (clientKey == null) ? false : true;
  }
}
