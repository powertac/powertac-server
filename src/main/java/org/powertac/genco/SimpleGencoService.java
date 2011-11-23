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
 
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;

import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Very simple service, activated by the
 * {@link org.powertac.server.CompetitionControlService} once each timeslot.
 * @author John Collins
 */
@Service
class SimpleGencoService
  extends TimeslotPhaseProcessor
{
  static private Logger log = Logger.getLogger(SimpleGencoService.class.getName());

  @Autowired
  private TimeService timeService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;

  private List<Genco> gencos;
  
  /**
   * Default constructor
   */
  public SimpleGencoService ()
  {
    super();
  }

  /**
   * Simply receives and stores the list of genco and buyer instances generated
   * by the initialization service.
   */
  public void init(List<Genco> gencos)
  {
    this.gencos = gencos;
    super.init();
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
