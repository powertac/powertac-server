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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.interfaces.BootstrapState;
import org.powertac.common.interfaces.CustomerServiceAccessor;
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
 * Manages a set of customer models. Each must be configurable as
 * ConfigurableInstances. Each gets supplied a set of service interfaces.
 * In each timeslot, each model has its step() method called. When tariffs
 * are published, each has its evaluateTariffs() method called. At the end
 * of a boot session, each has its bootstrap state saved to the boot record.
 * 
 * @author John Collins
 */
@Service
public class CustomerModelService
extends TimeslotPhaseProcessor
implements InitializationService, BootstrapState, NewTariffListener,
  CustomerServiceAccessor
{
  static private Logger log =
      LogManager.getLogger(CustomerModelService.class.getName());

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
  //private ArrayList<Class<AbstractCustomerDeprecated>> modelTypes;
  private ArrayList<AbstractCustomer> models;

  @Override
  public String
    initialize (Competition competition, List<String> completedInits)
  {
    if (!completedInits.contains("DefaultBroker")
        || !completedInits.contains("TariffMarket"))
      return null;
    super.init();
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
      Collection<?> instances =
          serverConfig.configureInstances(modelEx.getClass());
      for (Object modelObj: instances) {
        AbstractCustomer model = (AbstractCustomer) modelObj;
        log.info("Adding model " + model.getName());
        models.add(model);
        model.setServiceAccessor(this);
        model.initialize();
        // set default tariff here to make models testable outside Spring.
        for (CustomerInfo cust: model.getCustomerInfos()) {
          tariffMarketService.subscribeToTariff(tariffMarketService
                                                .getDefaultTariff(cust.getPowerType()), cust, cust.getPopulation());
          customerRepo.add(cust);
          model.handleInitialSubscription(tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(cust));
        }
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
      log.info("Step model " + model.getName());
      model.step();
    }
  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    for (AbstractCustomer model : models) {
      log.info("Evaluating tariffs for " + model.getName());
      model.evaluateTariffs(tariffs);
    }
  }

  // test support methods
  List<AbstractCustomer> getModelList ()
  {
    return models;
  }

  @Override
  public void saveBootstrapState ()
  {
    serverConfig.saveBootstrapState(models);
    for (AbstractCustomer model : models) {
      // some models have to save local state
      model.saveBootstrapState();
    }
  }

  // ==============================
  // CustomerServiceAccessor API
  // ==============================

  @Override
  public CustomerRepo getCustomerRepo ()
  {
    return customerRepo;
  }

  @Override
  public RandomSeedRepo getRandomSeedRepo ()
  {
    return randomSeedRepo;
  }

  @Override
  public TariffRepo getTariffRepo ()
  {
    return tariffRepo;
  }

  @Override
  public TariffSubscriptionRepo getTariffSubscriptionRepo ()
  {
    return tariffSubscriptionRepo;
  }

  @Override
  public TimeslotRepo getTimeslotRepo ()
  {
    return timeslotRepo;
  }

  @Override
  public TimeService getTimeService()
  {
    return timeService;
  }

  @Override
  public WeatherReportRepo getWeatherReportRepo ()
  {
    return weatherReportRepo;
  }

  @Override
  public ServerConfiguration getServerConfiguration ()
  {
    return serverConfig;
  }

  @Override
  public TariffMarket getTariffMarket ()
  {
    return tariffMarketService;
  }
}
