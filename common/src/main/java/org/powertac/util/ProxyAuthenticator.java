package org.powertac.util;

import java.net.Authenticator;
import java.net.PasswordAuthentication;


public class ProxyAuthenticator extends Authenticator
{
  private String username;
  private String password;

  public ProxyAuthenticator (boolean useSocks)
  {
    String proxyHost;
    String proxyPort;
    if (useSocks) {
      proxyHost = System.getProperty("socksProxyHost", "");
      proxyPort = System.getProperty("socksProxyPort", "");
      username = System.getProperty("java.net.socks.username", "");
      password = System.getProperty("java.net.socks.password", "");
    }
    else {
      proxyHost = System.getProperty("http.proxyHost", "");
      proxyPort = System.getProperty("http.proxyPort", "");
      username = System.getProperty("http.proxyUser", "");
      password = System.getProperty("http.proxyPassword", "");
    }

    if (!proxyHost.isEmpty()) {
      System.out.printf("\nConnecting via proxy %s:%s\n", proxyHost, proxyPort);
    }
    if (!username.isEmpty()) {
      System.out.printf("Username : %s\n\n", username);
      Authenticator.setDefault(this);
    }
  }

  public PasswordAuthentication getPasswordAuthentication ()
  {
    return new PasswordAuthentication(username, password.toCharArray());
  }
}
