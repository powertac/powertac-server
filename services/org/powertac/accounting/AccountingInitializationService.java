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
package org.powertac.accounting;

import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.RandomSeed;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.repo.PluginConfigRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Pre-game initialization for the accounting service
 * @author John Collins
 */
@Service
public class AccountingInitializationService 
    implements InitializationService
{
  static private Logger log = Logger.getLogger(AccountingInitializationService.class.getName());

  @Autowired
  private AccountingService accountingService;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private RandomSeedRepo randomSeedService;
  private RandomSeed randomGen;
  
  private double minInterest = 0.04;
  private double maxInterest = 0.12;
  
  public void setDefaults ()
  {
    randomGen = randomSeedService.getRandomSeed("AccountingInitializationService",
                                                 0l, "interest");
    //randomGen = new Random(randomSeed);

    double interest = (minInterest + 
		       (randomGen.nextDouble() *
			(maxInterest - minInterest)));

    log.info("bank interest: " + interest);
    pluginConfigRepo.makePluginConfig("AccountingService", "init")
        .addConfiguration("bankInterest",
                          Double.toString(interest));
  }

  public String initialize (Competition competition, List<String> completedInits)
  {
    PluginConfig accountingConfig = pluginConfigRepo.findByRoleName("AccountingService");
    if (accountingConfig == null) {
      log.error("PluginConfig for AccountingService does not exist");
    }
    else {
      accountingService.init(accountingConfig);
      return "AccountingService";
    }
    return "fail";
  }

  public double getMinInterest ()
  {
    return minInterest;
  }

  public AccountingInitializationService setMinInterest (double minInterest)
  {
    this.minInterest = minInterest;
    return this;
  }

  public double getMaxInterest ()
  {
    return maxInterest;
  }

  public AccountingInitializationService setMaxInterest (double maxInterest)
  {
    this.maxInterest = maxInterest;
    return this;
  }
}
