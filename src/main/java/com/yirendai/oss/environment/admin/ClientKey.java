package com.yirendai.oss.environment.admin;

import com.yirendai.oss.lib.common.crypto.EncodeDecryptor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by Yuliang Jin on 16/10/26.
 */
@Data
@Slf4j
public class ClientKey implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String CONSTANT_STRING_FOR_DISPLAY = "******";
  private static final String CONSTANT_STRING_FOR_USERNAME= "username";
  @SuppressWarnings("squid:S2068")
  private static final String CONSTANT_STRING_FOR_PASSWORD= "password";
  private static final String CONSTANT_STRING_FOR_MANAGEMENT_AUTHENTICATION= "managementAuthentication";

  private String serviceId;
  private String userName;
  private String password;

  public ClientKey(String serviceId, String userName, String password) {
    this.serviceId = serviceId;
    this.userName = userName;
    this.password = password;
  }

  public static String serviceIdFrom(final String requestUri) {
    log.debug("Request Uri is {}.", requestUri);
    return requestUri.split("/")[3];
  }

  /**
   * To get the plain username and password; To modify the encrypted info to a constant String.
   * @param infoObject
   * @param decryptor
   * @return
   */
  public static String[] usernamePasswordFromInfoObject( //
      final Map<String, Object> infoObject, final EncodeDecryptor decryptor) {
    final String[] result;
    if (infoObject != null && infoObject.containsKey(CONSTANT_STRING_FOR_MANAGEMENT_AUTHENTICATION)) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> managementAuth = (Map<String, Object>) infoObject.get(CONSTANT_STRING_FOR_MANAGEMENT_AUTHENTICATION);
      final Boolean encrypted = (Boolean) managementAuth.get("encrypted");
      final String username;
      final String password;
      if (encrypted) {
        username = decryptor.decrypt((String) managementAuth.get(CONSTANT_STRING_FOR_USERNAME));
        password = decryptor.decrypt((String) managementAuth.get(CONSTANT_STRING_FOR_PASSWORD));
      } else {
        username = (String) managementAuth.get(CONSTANT_STRING_FOR_USERNAME);
        password = (String) managementAuth.get(CONSTANT_STRING_FOR_PASSWORD);
      }

      managementAuth.put(CONSTANT_STRING_FOR_USERNAME, CONSTANT_STRING_FOR_DISPLAY);
      managementAuth.put(CONSTANT_STRING_FOR_PASSWORD, CONSTANT_STRING_FOR_DISPLAY);

      infoObject.put(CONSTANT_STRING_FOR_MANAGEMENT_AUTHENTICATION, managementAuth);

      result = new String[]{username, password};
    } else {
      result = null;
    }
    return result;
  }
}
