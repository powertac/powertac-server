/*
 * Copyright 2011, 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.samplebroker.core;

import java.net.Authenticator;
import java.net.PasswordAuthentication;


/**
 * This is the top level of the Power TAC server.
 * @author John Collins
 */
public class BrokerMain
{
  //static private Logger log = Logger.getLogger(BrokerMain.class);

  /**
   * Sets up the broker. Single command-line arg is the username
   */
  public static void main (String[] args)
  {
    String username = System.getProperty("java.net.socks.username", "");
    String password = System.getProperty("java.net.socks.password", "");
    if (!username.isEmpty()) {
      Authenticator.setDefault(new ProxyAuthenticator(username, password));
    }

    BrokerRunner runner = new BrokerRunner();
    runner.processCmdLine(args);
    
    // if we get here, it's time to exit
    System.exit(0);
  }

  private static class ProxyAuthenticator extends Authenticator
  {
    private String userName, password;

    private ProxyAuthenticator (String userName, String password)
    {
      this.userName = userName;
      this.password = password;
    }

    protected PasswordAuthentication getPasswordAuthentication ()
    {
      return new PasswordAuthentication(userName, password.toCharArray());
    }
  }
}
