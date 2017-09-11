package cn.home1.oss.environment.admin;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Yuliang Jin on 16/10/26.
 */
public class ClientKeyStore {

  private final ConcurrentMap<String, ClientKey> keysMap = new ConcurrentHashMap<>();

  public ClientKey save(final ClientKey clientKey) {
    return this.keysMap.put(clientKey.getServiceId(), clientKey);
  }

  public Collection<ClientKey> findAll() {
    return this.keysMap.values();
  }

  public ClientKey find(final String serviceId) {
    return this.keysMap.get(serviceId);
  }

  public ClientKey delete(final String serviceId) {
    return this.keysMap.remove(serviceId);
  }

  public boolean isAvailable(final String serviceId) {
    return find(serviceId) != null;
  }
}
