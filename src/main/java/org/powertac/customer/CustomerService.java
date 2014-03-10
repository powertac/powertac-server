/*
 * Copyright (c) 2014 by John Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.customer;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author John Collins
 */
@Service
public class CustomerService
extends TimeslotPhaseProcessor
implements InitializationService, NewTariffListener
{
  static private Logger log = Logger.getLogger(CustomerService.class.getName());

  @Autowired
  private TimeService timeService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private ServerConfiguration serverConfig;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  // Customer model collection
  private ArrayList<String> modelTypes;
  private ArrayList<AbstractCustomer> models;

  @Override
  public void setDefaults ()
  {
    // Obsolete
  }

  @Override
  public String
    initialize (Competition competition, List<String> completedInits)
  {
    super.init();
    modelTypes = new ArrayList<String>();
    models = new ArrayList<AbstractCustomer>();
    
    // populate the models list
    // initialize individual customers
    return "Customer";
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.TimeslotPhaseProcessor#activate(org.joda.time.Instant, int)
   */
  @Override
  public void activate (Instant time, int phaseNumber)
  {
    for (AbstractCustomer model : models) {
      model.step();
    }
  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    for (AbstractCustomer model : models) {
      model.evaluateTariffs(tariffs);
    }
  }

}
