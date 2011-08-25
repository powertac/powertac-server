/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This is the top level of the Power TAC server.
 * @author John Collins
 */
public class PowerTacServer
{
  private static CompetitionControlService cc = null;

  /**
   * Sets up the container, sets up logging, and starts the
   * CompetitionControl service
   */
  public static void main (String[] args)
  {
    ApplicationContext context =
      new ClassPathXmlApplicationContext("config/powertac.xml");
    cc = (CompetitionControlService)context.getBean("competitionControl");

    System.out.println("Server BootStrap");
    //participantManagementService.initialize();
    cc.preGame();
  }
}
