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
package org.powertac.du;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.PluginConfig;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.state.StateChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Default broker implementation. We do the implementation in a service, because
 * the default broker is a singleton and it's convenient.
 * @author John Collins
 */
@Service
public class DefaultBrokerService 
implements TimeslotPhaseProcessor, BrokerMessageListener
{
  static private Logger log = Logger.getLogger(DefaultBrokerService.class.getName());

  @Autowired
  private CompetitionControl competitionControlService;
  
  @Autowired
  private TariffMarket tariffMarketService;
  
  private Broker face;
  private double defaultConsumptionRate = 1.0;
  private double defaultProductionRate = -0.01;
  
  /** when to invoke this service in per-timeslot processing */
  private int simulationPhase = 1;
  
  /**
   * Default constructor, called once when the server starts, before
   * any application-specific initialization has been done.
   */
  public DefaultBrokerService ()
  {
    super();
  }
  
  /**
   * Called by initialization service once at the beginning of each game.
   * Sets up and publishes default tariffs.
   * Open question: when do customers subscribe to default tariff?
   */
  void init (PluginConfig config, Broker defaultBroker)
  {
    face = defaultBroker;
    // create and publish default tariffs
    double consumption = (config.getDoubleValue("consumptionRate",
                                                defaultConsumptionRate));
    TariffSpecification defaultConsumption =
      new TariffSpecification(face, PowerType.CONSUMPTION)
        .addRate(new Rate().withValue(consumption));
    tariffMarketService.setDefaultTariff(defaultConsumption);

    double production = (config.getDoubleValue("productionRate",
                                                defaultProductionRate));
    TariffSpecification defaultProduction =
      new TariffSpecification(face, PowerType.PRODUCTION)
        .addRate(new Rate().withValue(production));
    tariffMarketService.setDefaultTariff(defaultProduction);

    competitionControlService.registerTimeslotPhase(this, simulationPhase);
    
  }

  public void activate (Instant time, int phaseNumber)
  {
    // TODO Implement per-timeslot behavior
  }

  public void receiveMessage (Object msg)
  {
    // TODO Implement per-message behavior. Note that incoming messages
    // from brokers arrive in a JMS thread, so you need to synchronize
    // access to shared data structures. See AuctionService for an example.
    
    // If you need to handle a number of different message types, it may make
    // make sense to use a reflection-based dispatcher. Both
    // TariffMarketService and AccountingService work this way.
  }

}
