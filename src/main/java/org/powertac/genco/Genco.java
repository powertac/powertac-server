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

import org.powertac.common.Broker;
import org.powertac.common.IdGenerator;
import org.powertac.common.PluginConfig;
import org.powertac.common.Order;
import org.powertac.common.Timeslot;
import org.powertac.common.MarketPosition;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.RandomSeed;

/**
 * Represents a producer of power in the transmission domain. Individual
 * models are players on the wholesale side of the Power TAC day-ahead
 * market.
 * @author jcollins
 */
@Domain
public class Genco
  extends Broker
{
  static private Logger log = Logger.getLogger(Genco.class.getName());

  // id values are standardized
  private long id = IdGenerator.createId();
  
  /** Current capacity of this producer in mW */
  private double currentCapacity;
  
  /** Per-timeslot variability */
  private double variability = 0.01;
  
  /** Mean-reversion tendency - portion of variability to revert
   *  back to nominal capacity */
  private double meanReversion = 0.2;
  
  /** True if plant is currently operating */
  private boolean inOperation = true;
  
  /** Proportion of time plant is working */
  private double reliability = 0.98;
  
  /** True if this is a renewable source */
  @SuppressWarnings("unused")
  private boolean renewable = false;
  
  protected BrokerProxy brokerProxyService;
  protected RandomSeed seed;

  // configured parameters  
  //private String name;
  private double nominalCapacity = 100.0;
  private double cost = 1.0;
  private int commitmentLeadtime = 1;
  private double carbonEmissionRate = 0.0; 

  public Genco (String username)
  {
    super(username, true, true);
  }
  
  public void init (BrokerProxy proxy, RandomSeedRepo randomSeedRepo)
  {
    log.info("init " + getUsername());
    this.brokerProxyService = proxy;
    this.seed = randomSeedRepo.getRandomSeed(Genco.class.getName(), id, "update");
  }

  // getters for testing purposes
  public boolean isInOperation ()
  {
    return inOperation;
  }

  public double getNominalCapacity ()
  {
    return nominalCapacity;
  }

  public double getCost ()
  {
    return cost;
  }

  public int getCommitmentLeadtime ()
  {
    return commitmentLeadtime;
  }

  double getCurrentCapacity ()
  {
    return currentCapacity;
  }

  /**
   * Updates this model for the current timeslot, by adjusting
   * capacity, checking for downtime, and creating exogenous
   * commitments.
   */
  public void updateModel (Instant currentTime)
  {
    log.info("Update " + getUsername());
    updateCapacity(seed.nextDouble());
    updateInOperation(seed.nextDouble());
  }
  
  /**
   * Generates Orders in the market to sell available capacity. No Orders
   * are submitted if the plant is not in operation.
   */
  public void generateOrders (Instant now, List<Timeslot> openSlots)
  {
    if (!inOperation){
      log.info("not in operation - no orders");
      return;
    }
    log.info("Generate orders for " + getUsername());
    for (Timeslot slot : openSlots) {
      double availableCapacity = currentCapacity;
      // do we receive these?
      MarketPosition posn = findMarketPositionByTimeslot(slot);
      if (posn != null) {
        // posn.overallBalance is negative if we have sold power in this slot
        availableCapacity += posn.getOverallBalance();
      }
      if (availableCapacity > 0.0) {
        // make an offer to sell
        Order offer =
            new Order(this, slot,
                      -availableCapacity, cost);
	log.debug(getUsername() + " offers " + availableCapacity +
	          " in " + slot.getSerialNumber() + " for " + cost);
	brokerProxyService.routeMessage(offer);
      }
    }
  }
  
  /**
   * Override Broker.receiveMessage(). The only message type we care about is
   * the market position.
   */
  public void receiveMessage (Object object)
  {
    if (object instanceof MarketPosition) {
      MarketPosition posn = (MarketPosition)object;
      addMarketPosition(posn, posn.getTimeslot());
    }
  }
  
  
  private void updateCapacity (double val)
  {
    if (variability > 0.0) {
      setCurrentCapacity(currentCapacity +
                         nominalCapacity * (val * 2 * variability - 
                                            variability) + 
                         variability * meanReversion * (nominalCapacity -
                                                        currentCapacity));
    }
  }
  
  @StateChange
  private void setCurrentCapacity (double val)
  {
    currentCapacity = val;
  }
  
  private void updateInOperation (double val)
  {
    setInOperation(val <= reliability);
  }
  
  @StateChange
  private void setInOperation (boolean op)
  {
    inOperation = op;
  }

  // ------------------ configuration access methods -------------------
  void configure (PluginConfig config)
  {
    //this.config = config
    nominalCapacity = config.getDoubleValue("nominalCapacity", nominalCapacity);
    currentCapacity = nominalCapacity;
    variability = config.getDoubleValue("variability", variability);
    reliability = config.getDoubleValue("reliability", reliability);
    cost = config.getDoubleValue("cost", cost);
    commitmentLeadtime = config.getIntegerValue("commitmentLeadtime",
                                                commitmentLeadtime);
    carbonEmissionRate = config.getDoubleValue("carbonEmissionRate", carbonEmissionRate);
  }
}
