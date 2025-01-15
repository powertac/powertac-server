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

package org.powertac.factoredcustomer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
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
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.factoredcustomer.CustomerFactory.CustomerCreator;
import org.powertac.factoredcustomer.interfaces.FactoredCustomer;
import org.powertac.factoredcustomer.interfaces.StructureInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Key class that processes the configuration file and creates a collection of customers
 * during the initialization process.  It also delegates tariff selection callbacks and
 * timeslot activation (i.e., capacity reporting) to the collection of customers.
 *
 * @author Prashant Reddy
 */
@Service  // allow autowiring
public class FactoredCustomerService extends TimeslotPhaseProcessor
    implements InitializationService, NewTariffListener, CustomerServiceAccessor
{
  private static Logger log =
      LogManager.getLogger(FactoredCustomerService.class.getName());

  @Autowired
  private TimeService timeService;

  @Autowired
  private TariffMarket tariffMarketService;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private WeatherReportRepo weatherReportRepo;

  @Autowired
  private WeatherForecastRepo weatherForecastRepo;

  @Autowired
  private ServerConfiguration serverConfiguration;

  private List<FactoredCustomer> customers;
  private CustomerFactory customerFactory;
  private boolean newTariffs = false; // When true, check for new subscriptions

  public FactoredCustomerService ()
  {
    super();
  }

  /**
   * This is called once at the beginning of each game.
   */
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    if (!completedInits.contains("DefaultBroker") || !completedInits.contains("TariffMarket")) {
      log.debug("Waiting for DefaultBroker and TariffMarket to initialize");
      return null;
    }

    super.init();
    customers = new ArrayList<>(); // recycle between games
    customerFactory = new CustomerFactory();
    newTariffs = false;
    tariffMarketService.registerNewTariffListener(this);

    registerAvailableCustomerCreators();

    Config.initializeInstance(serverConfiguration);
    Config config = Config.getInstance();
    config.configure();

    Map<String, StructureInstance> customerStructures =
        config.getStructures().get("CustomerStructure");

    log.info("Creating factored customers from configuration structures...");
    for (StructureInstance instance : customerStructures.values()) {
      CustomerStructure customerStructure = (CustomerStructure) instance;
      FactoredCustomer customer = customerFactory.processStructure(customerStructure);
      if (customer != null) {
        customer.initialize(this);
        customers.add(customer);
      }
      else {
        throw new Error("Could not create factored customer for structure: " +
            customerStructure.getName());
      }
    }
    log.info("Successfully initialized " + customers.size() +
        " factored customers from " + customerStructures.size() + " structures");
    return "FactoredCustomer";
  }

  // mockable component access methods - package visibility
  public TimeService getTimeService ()
  {
    return timeService;
  }

  public CustomerRepo getCustomerRepo ()
  {
    return customerRepo;
  }

  public TariffRepo getTariffRepo ()
  {
    return tariffRepo;
  }

  public org.powertac.common.repo.TimeslotRepo getTimeslotRepo ()
  {
    return timeslotRepo;
  }

  public RandomSeedRepo getRandomSeedRepo ()
  {
    return randomSeedRepo;
  }

  public TariffSubscriptionRepo getTariffSubscriptionRepo ()
  {
    return tariffSubscriptionRepo;
  }

  @Override
  public TariffMarket getTariffMarket ()
  {
    return tariffMarketService;
  }

  public WeatherReportRepo getWeatherReportRepo ()
  {
    return weatherReportRepo;
  }

  public WeatherForecastRepo getWeatherForecastRepo ()
  {
    return weatherForecastRepo;
  }

  private void registerAvailableCustomerCreators ()
  {
    customerFactory.registerDefaultCreator(DefaultFactoredCustomer.getCreator());
    log.info("Registered default factored customer creator");

    List<String> creatorNames = new ArrayList<>();
    creatorNames.add("org.powertac.factoredcustomer.LearningCustomerCreator");

    for (String name : creatorNames) {
      try {
        CustomerCreator creator =
                (CustomerCreator) Class.forName(name)
                .getDeclaredConstructor().newInstance();
        customerFactory.registerCreator(creator);
        log.info("Registered creator: " + name);
      }
      catch (ClassNotFoundException ignored) {
      }
      catch (Exception e) {
        throw new Error("Could not register creator for name: "
            + name + "; caught exception: " + e);
      }
    }
  }

  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    // Find the subset of tariffs to evaluate
    for (FactoredCustomer customer : customers) {
      customer.evaluateTariffs();
    }
    newTariffs = true;
  }

  private void updatedSubscriptionRepo ()
  {
    // Find the subset of tariffs to evaluate
    log.info("Time to handle new subscriptions");
    for (FactoredCustomer customer : customers) {
      customer.updatedSubscriptionRepo();
    }
  }

  @Override
  public void activate (Instant now, int phase)
  {
    if (newTariffs) {
      // possible new subscriptions in last timeslot
      newTariffs = false;
      updatedSubscriptionRepo();
    }
    for (FactoredCustomer customer : customers) {
      customer.handleNewTimeslot();
    }
  }

  /**
   * package scope for testing
   **/
  List<FactoredCustomer> getCustomers ()
  {
    return customers;
  }

  @Override
  public ServerConfiguration getServerConfiguration ()
  {
    // TODO Auto-generated method stub
    return serverConfiguration;
  }

  @Override
  public XMLMessageConverter getMessageConverter ()
  {
    // TODO Auto-generated method stub
    return null;
  }
}
