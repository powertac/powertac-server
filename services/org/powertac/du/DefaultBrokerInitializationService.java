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
package org.powertac.du;

import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Here we support the pre-game and start-of-game behaviors driven by the
 * CompetitionControlService. This class is instantiated by Spring, so in
 * general the constructor can be omitted.
 * @author John Collins
 */
@Service
public class DefaultBrokerInitializationService
implements InitializationService
{
  static private Logger log = Logger.getLogger(DefaultBrokerInitializationService.class.getName());

  @Autowired
  private DefaultBrokerService defaultBrokerService;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  /**
   * Creates the default broker instance, and a PluginConfig instance that
   * includes rates for standard default tariffs.
   */
  public void setDefaults ()
  {
    // create the default broker instance, register it with the repo
    brokerRepo.add(defaultBrokerService.createBroker("default broker"));
    // set default tariff parameters
    pluginConfigRepo.makePluginConfig("defaultBroker", "init")
      .addConfiguration("consumptionRate", "-0.5") // -0.50/kwh
      .addConfiguration("productionRate", "0.02") // 0.02/kwh
      .addConfiguration("initialBidKWh", "1000.0")
      .addConfiguration("buyLimitPrice", "-50.0")
      .addConfiguration("sellLimitPrice", "1.0");
  }

  /**
   * Initializes the default broker service.
   */
  public String initialize (Competition competition, List<String> completedInits)
  {
    int index = completedInits.indexOf("TariffMarket");
    if (index == -1) {
      return null;
    }
    PluginConfig config = pluginConfigRepo.findByRoleName("defaultBroker");
    if (config == null) {
      log.error("PluginConfig for Generic does not exist.");
      return "fail";
    }
    defaultBrokerService.init(config);
    return "DefaultBroker";
  }
}
