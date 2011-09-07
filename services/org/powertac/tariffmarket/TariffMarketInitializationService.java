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
package org.powertac.tariffmarket

import java.util.List;

import org.powertac.common.Competition;
import org.powertac.common.PluginConfig
import org.powertac.common.interfaces.InitializationService

class TariffMarketInitializationService 
    implements InitializationService
{
  static transactional = true

  def tariffMarketService // autowire
  def tariffRateService
  
  @Override
  public void setDefaults ()
  {
    tariffMarketService.setup()
    PluginConfig tariffMarketConfig =
        new PluginConfig(roleName: 'TariffMarket',
                         configuration: [tariffPublicationFee: '-100.0',
                                         tariffRevocationFee: '-100.0',
                                         publicationInterval: '6'])
    tariffMarketConfig.save()
  }
  
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    if (!completedInits.find{'AccountingService' == it}) {
      return null
    }
    PluginConfig tariffMarketConfig = PluginConfig.findByRoleName('TariffMarket')
    if (tariffMarketConfig == null) {
      log.error "PluginConfig for TariffMarket does not exist"
    }
    else {
      tariffMarketService.init(tariffMarketConfig)
      tariffRateService.init()
      return 'TariffMarket'
    }
    return 'fail'
  }
}
