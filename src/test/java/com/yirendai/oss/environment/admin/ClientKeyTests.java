package com.yirendai.oss.environment.admin;

import com.yirendai.oss.lib.common.crypto.Cryptos;
import com.yirendai.oss.lib.common.crypto.EncodeDecryptor;
import com.yirendai.oss.lib.common.crypto.KeyExpression;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by leo on 16/12/21.
 */
public class ClientKeyTests {

  @Test
  public void testServiceIdFrom() {
    Arrays.asList("/api/applications/996369b3/logfile",
        "/api/applications/996369b3/refresh")
        .stream()
        .map(url -> ClientKey.serviceIdFrom(url))
        .forEach(serviceId -> Assert.assertEquals("996369b3", serviceId));
  }

  @Test
  public void testUsernamePasswordFromInfoObject() {
    String adminPrivateKey = "RSA1024_PRIV_PKCS1:MIICXQIBAAKBgQC" +
        "/gmBcdQZxiQmhQrP1awAZuuOl4snl7cEV8n65osVO7CdqxXG5mUYNVr6siwuTm/SsmBV+86JISlzvMK/Bxwsmf/ApZicgItChmDuU9TCaZIksqnpbtONnCm/sHWwa/2hqPTjdc0LC+jQ/FCU2b9vpbSId0Wg28/gtoGaLWbsm/QIDAQABAoGBAI7dOfl/K5FjA5YTZqB8dBS9wLmtl6Q5W0N+JV9iuAKKVVVnedFVMFcfERsyly5Et6BRzCdqpPN81htxnIvYas2+Nvu5be1NwPAYLW2NUFDRzEAH/vWOLhY2F5uo24AJBHRRCRnLiqq/8aZ9STDdzS8WlHBg5kOforoqREXwmxKBAkEA/lFwu3ftQQyIavV56za+o6C8W/S7GqCWurOo3C+kKHOpUtqRjkacdEoxFCjs92RNJzd+fhGMzUhupKGX1+uKjQJBAMDGmdVknB8iLxm0CWjuRm+q0ciK0Ech2tB+39DsUvVNguvJAbxD+CMiusJ7dCFbVy1G9rjQR2gTLEI52BM0qjECQDHVb4usolb+x7R9yZgnsA+MLZyvRgKfuSl4jvwmcbpjf6h2n9MLTxkSeK+EnXqUsvGeVDEL61VGfjfQWlq7EvkCQQC1/IcLUeCk75OBc1oSyiaKkrta09kN3eMBQ1UtmXwzgYof53GQ9qWRHd8rbHpUZzNkVgLitBVFJhx5JLxcXTJxAkBalCMEuhXGYBkLMhcstJzs+Gk07FkkgV1fC4Kf9Xeu5YLkxS+tjkZt3iwfUo4Ll3m3fWhguLUtf05jUgWClx0t";

    EncodeDecryptor decryptor = Cryptos.decryptor(new KeyExpression(adminPrivateKey));

    HashMap<String, Object> stringObjectHashMap = new HashMap<>();
    LinkedHashMap<String, Object> gitInfoMap = new LinkedHashMap<>();

    gitInfoMap.put("time", "27.12.2016 @ 02:09:47 GMT-08:00");
    gitInfoMap.put("id", "a0409aa");

    LinkedHashMap<String, Object> managementAuthenticationInfo = new LinkedHashMap<>();

    managementAuthenticationInfo.put("password", "UJKfmehxcxZG6YBo0sERdH958Jr1IJF8sy861/wrq01eCrcW9NLH1FU0umDD/ppbMcIvc" +
        "+78Mf7wUpNkzqlHg06Lf2wEs9iHZwqBKVZP0V3s26cVjiDV/zg0EJol3y3P/iQIzuwD5iEmhnvvNUG8DnVVqMdLw0/t6l/JwpuDBZg=");

    managementAuthenticationInfo.put("encrypted", true);

    managementAuthenticationInfo.put("username",
        "dVHvias9Oc1se7SMpzI37TwZYKKTGbfpq107MKJkXRPwYqrBSactJUHUXg6Y59NTEZ8zxtR8Qph4vEL0xOLQV2LLu23RT1sLpSQofdWZ6jK4dPgLO2TMXf9d4a7J2EgxO7Brbwvo8XA+HAtuewjd8UzPLql+vT693O3N7LDujDw=");

    stringObjectHashMap.put("git", gitInfoMap);
    stringObjectHashMap.put("managementAuthentication", managementAuthenticationInfo);
    String[] usernamePassword = ClientKey.usernamePasswordFromInfoObject(stringObjectHashMap, decryptor);

    //Test decryption.
    Assert.assertEquals("admin", usernamePassword[0]);
    Assert.assertEquals("admin_pass", usernamePassword[1]);

    //Test modify encrypted info.
    Assert.assertEquals("******", ((Map<String, Object>) stringObjectHashMap.get("managementAuthentication")).get
        ("username"));
    Assert.assertEquals("******", ((Map<String, Object>) stringObjectHashMap.get("managementAuthentication")).get
        ("password"));
  }
}
