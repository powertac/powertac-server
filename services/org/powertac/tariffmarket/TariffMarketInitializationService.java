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
package org.powertac.tariffmarket;

import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;

public class TariffMarketInitializationService 
    implements InitializationService
{
  static private Logger log = Logger.getLogger(TariffMarketInitializationService.class.getName());

  @Autowired
  private TariffMarketService tariffMarketService;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Override
  public void setDefaults ()
  {
    pluginConfigRepo.makePluginConfig("TariffMarket", "")
            .addConfiguration("tariffPublicationFee", "-100.0")
            .addConfiguration("tariffRevocationFee", "-100.0")
            .addConfiguration("publicationInterval", "6");
  }
  
  @Override
  public String initialize (Competition competition, List completedInits)
  {
    int index = completedInits.indexOf("AccountingService");
    if (index == -1) {
      return null;
    }
    PluginConfig tariffMarketConfig = pluginConfigRepo.findByRoleName("TariffMarket");
    if (tariffMarketConfig == null) {
      log.error("PluginConfig for TariffMarket does not exist");
    }
    else {
      tariffMarketService.init(tariffMarketConfig);
      return "TariffMarket";
    }
    return "fail";
  }
  
  public void shutDown ()
  {
    //tariffMarketService.shutDown();
  }
}
