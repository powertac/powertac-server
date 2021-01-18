/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.logtool.common;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RateCore;
import org.powertac.common.TariffSpecification;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;

import org.powertac.logtool.ifc.Analyzer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * An analyzer that populates repos, including brokers, tariffs,
 * orderbooks, timeslots, and customers
 * 
 * @author John Collins
 */
@Service
public class DomainBuilder implements Analyzer
{
  static private Logger log = LogManager.getLogger(DomainBuilder.class.getName());

  @Autowired
  private DomainObjectReader dor;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private TariffRepo tariffRepo;

  @Autowired
  private OrderbookRepo orderbookRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Override
  public void setup ()
  {
    dor.registerNewObjectListener(new BrokerHandler(), Broker.class);
    dor.registerNewObjectListener(new CustomerHandler(), CustomerInfo.class);
    dor.registerNewObjectListener(new TariffSpecHandler(), TariffSpecification.class);
    dor.registerNewObjectListener(new RateHandler(), RateCore.class);
    dor.registerNewObjectListener(new TimeslotHandler(), Timeslot.class);
  }

  @Override
  public void report ()
  {
    // nothing to report
  }
  
  // -------------------------------
  // add new brokers to repo
  class BrokerHandler implements NewObjectListener
  {
    @Override
    public void handleNewObject (Object thing)
    {
      Broker broker = (Broker)thing;
      log.info("add broker " + broker.getUsername());
      brokerRepo.add(broker);
    }
  }
  
  // -------------------------------
  // add new customers to repo
  class CustomerHandler implements NewObjectListener
  {
    @Override
    public void handleNewObject (Object thing)
    {
      CustomerInfo customer = (CustomerInfo)thing;
      customerRepo.add(customer);
    }
  }

  // -------------------------------
  // add new tariff specs to repo
  class TariffSpecHandler implements NewObjectListener
  {
    @Override
    public void handleNewObject (Object thing)
    {
      TariffSpecification spec = (TariffSpecification)thing;
      // attach pending Rates
      ArrayList<RateCore> rates = pendingRates.get(spec.getId());
      if (null != rates)
        for (RateCore rate : rates)
          spec.addRate(rate);
      tariffRepo.addSpecification(spec);
    }
  }

  private HashMap<Long, ArrayList<RateCore>> pendingRates =
          new HashMap<Long, ArrayList<RateCore>>();

  // -------------------------------
  // add new Rates to their Tariffs
  class RateHandler implements NewObjectListener
  {
    @Override
    public void handleNewObject (Object thing)
    {
      RateCore rate = (RateCore)thing;
      TariffSpecification spec =
              tariffRepo.findSpecificationById(rate.getTariffId());
      if (null != spec) {
        // attach rate to spec if it's already known
        spec.addRate(rate);
      }
      else {
        // otherwise save rate for later
        ArrayList<RateCore> rates = pendingRates.get(rate.getTariffId());
        if (null == rates) {
          rates = new ArrayList<RateCore>();
          pendingRates.put(rate.getTariffId(), rates);
        }
        rates.add(rate);
      }
    }
  }
  
  class TimeslotHandler implements NewObjectListener
  {

    @Override
    public void handleNewObject (Object thing)
    {
      Timeslot timeslot = (Timeslot)thing;
      timeslotRepo.add(timeslot);
    }
    
  }
}
