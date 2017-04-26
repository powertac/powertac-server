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

import org.powertac.util.ProxyAuthenticator;


/**
 * This is the top level of a Power TAC broker implementation. Command-line
 * processing and all other functions are delegated to an instance of
 * BrokerRunner.
 * 
 * @author John Collins
 */
public class BrokerMain
{
  /**
   * Sets up the broker. Single command-line arg is the username
   */
  public static void main (String[] args)
  {
    // Check for proxy settings, useSocks=true for brokers
    new ProxyAuthenticator(true);

    BrokerRunner runner = new BrokerRunner();
    runner.processCmdLine(args);

    // if we get here, it's time to exit
    System.exit(0);
  }
}
