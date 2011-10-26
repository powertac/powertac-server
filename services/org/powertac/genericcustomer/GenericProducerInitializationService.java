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
package org.powertac.genericcustomer;

import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Pre-game initialization for the Generic Consumer
 * @author Antonios Chrysopoulos
 */

@Service
public class GenericProducerInitializationService implements InitializationService
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error() appropriately. Use
   * log.debug() for output you want to see in testing or debugging.
   */
  static private Logger log = Logger.getLogger(GenericProducerInitializationService.class.getName());

  @Autowired
  private GenericProducerService genericProducerService;

  @Autowired
  private PluginConfigRepo pluginConfigRepo;

  /**
   * This method is called during pre-game to set defaults. In this example the default value of the
   * parameter is hard-coded, but in general it would make sense to take it from a config file. The
   * result is a PluginConfig instance that can be viewed and modified through the web interface
   * before a game is started.
   */
  public void setDefaults ()
  {
    pluginConfigRepo.makePluginConfig("GenericProducer", "").addConfiguration("population", "100").addConfiguration("numberOfProducers", "2");
  }

  /**
   * Called during game startup to set initial conditions for Generic Customer Service. In order to
   * begin this Service the Default Broker must be initialized first.
   */
  public String initialize (Competition competition, List<String> completedInits)
  {
    if (!completedInits.contains("DefaultBroker")) {
      log.debug("waiting for DefaultBroker to initialize");
      return null;
    }
    PluginConfig config = pluginConfigRepo.findByRoleName("GenericProducer");
    if (config == null) {
      log.error("PluginConfig for GenericProducer does not exist.");
      return "fail";
    }
    genericProducerService.init(config);
    return "GenericProducer";
  }
}
