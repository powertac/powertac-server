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
package org.powertac.accountingservice

import java.util.List;

import org.powertac.common.Competition;
import org.powertac.common.PluginConfig
import org.powertac.common.interfaces.InitializationService

/**
 * Pre-game initialization for the accounting service
 * @author John Collins
 */
class AccountingInitializationService 
    implements InitializationService
{
  static transactional = true
  
  def accountingService //autowire
  def randomSeedService // autowire
  Random randomGen
  
  double minInterest = 0.04
  double maxInterest = 0.12
  
  @Override
  public void setDefaults ()
  {
   long randomSeed = randomSeedService.nextSeed('AccountingInitializationService',
                                                'init', 'interest')
    randomGen = new Random(randomSeed)

    double interest = (minInterest + 
		       (randomGen.nextDouble() *
			(maxInterest - minInterest)))

    log.info("bank interest: ${interest}")
    PluginConfig accounting =
    new PluginConfig(roleName:'AccountingService',
                     configuration: [bankInterest: Double.toString(interest)])
    accounting.save()
  }

  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    PluginConfig accountingConfig = PluginConfig.findByRoleName('AccountingService')
    if (accountingConfig == null) {
      log.error "PluginConfig for AccountingService does not exist"
    }
    else {
      accountingService.init(accountingConfig)
      return 'AccountingService'
    }
    return 'fail'
  }
}
