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

package org.powertac.genco;
 
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;

import org.powertac.common.Competition;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Very simple service that operates wholesale market actors, activated by the
 * {@link org.powertac.server.CompetitionControlService} once each timeslot.
 * @author John Collins
 */
@Service
public class SimpleGencoService
  extends TimeslotPhaseProcessor
  implements InitializationService
{
  static private Logger log = Logger.getLogger(SimpleGencoService.class.getName());

  @Autowired
  private TimeService timeService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private ServerConfiguration serverConfig;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  @Autowired
  private BrokerProxy brokerProxyService;
  
  @Autowired
  private RandomSeedRepo randomSeedRepo;

  private List<Genco> gencos;
  
  /**
   * Default constructor
   */
  public SimpleGencoService ()
  {
    super();
  }

  @Override
  public void setDefaults ()
  {
    // nothing to do at this point
  }

  /**
   * Creates the gencos and the buyer using the server configuration service.
   */
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    super.init();
    int seedId = 0;
    // create the genco list
    gencos = new ArrayList<Genco>();
    for (Object gencoObj : serverConfig.configureInstances(Genco.class)) {
      Genco genco = (Genco)gencoObj;
      brokerRepo.add(genco);
      genco.init(brokerProxyService, seedId++, randomSeedRepo);
      gencos.add(genco);
    }
    // configure the buyer
    Buyer buyer = new Buyer("buyer");
    serverConfig.configureMe(buyer);
    brokerRepo.add(buyer);
    gencos.add(buyer);
    buyer.init(brokerProxyService, seedId++, randomSeedRepo);
    return "Genco";
  }

  /**
   * Simply receives and stores the list of genco and buyer instances generated
   * by the initialization service.
   */
  public void init(List<Genco> gencos)
  {
    this.gencos = gencos;
  }

  /**
   * Called once/timeslot, simply calls updateModel() and generateOrders() on
   * each of the gencos.
   */
  public void activate(Instant now, int phase)
  {
    log.info("Activate");
    List<Timeslot> openSlots = timeslotRepo.enabledTimeslots();
    Instant when = timeService.getCurrentTime();
    for (Genco genco : gencos) {
      genco.updateModel(when);
      genco.generateOrders(when, openSlots);
    }
  }
}
