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
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author John Collins
 */
@Service
public class CustomerModelService
extends TimeslotPhaseProcessor
implements InitializationService, NewTariffListener
{
  static private Logger log = Logger.getLogger(CustomerModelService.class.getName());

  @Autowired
  private TimeService timeService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private ServerConfiguration serverConfig;

  @Autowired
  private WeatherReportRepo weatherReportRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private TariffMarket tariffMarketService;

  // Customer model collection
  //private ArrayList<Class<AbstractCustomer>> modelTypes;
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
    tariffMarketService.registerNewTariffListener(this);
    //modelTypes = new ArrayList<Class<AbstractCustomer>>();
    models = new ArrayList<AbstractCustomer>();
    // extract the model types
    ServiceLoader<AbstractCustomer> loader =
      ServiceLoader.load(AbstractCustomer.class);
    // Populate and initialize the models.
    // Note that the instances loaded by the service loader are discarded --
    // the real instances are created by serverConfig.
    Iterator<AbstractCustomer> modelIterator = loader.iterator();
    while (modelIterator.hasNext()) {
      AbstractCustomer modelEx = modelIterator.next();
      for (Object modelObj:
           serverConfig.configureInstances(modelEx.getClass())) {
        AbstractCustomer model = (AbstractCustomer) modelObj;
        models.add(model);
        model.setServices(randomSeedRepo, weatherReportRepo,
                          tariffRepo, tariffSubscriptionRepo);
        model.initialize();
        // set default tariff here to make models testable outside Spring.
        CustomerInfo cust = model.getCustomerInfo();
        tariffMarketService.subscribeToTariff(tariffMarketService
            .getDefaultTariff(cust.getPowerType()), cust, cust.getPopulation());
        customerRepo.add(model.getCustomerInfo());
      }
    }
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

  // test support methods
  List<AbstractCustomer> getModelList ()
  {
    return models;
  }
}
