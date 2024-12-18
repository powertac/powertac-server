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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;

import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.Order;
import org.powertac.common.RandomSeed;
import org.powertac.common.Timeslot;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import java.util.List;

/**
 * Represents a producer of power in the transmission domain. Individual
 * models are players on the wholesale side of the Power TAC day-ahead
 * market.
 * @author jcollins
 */
@Domain
@ConfigurableInstance
public class Genco
  extends Broker
{
  static private Logger log = LogManager.getLogger(Genco.class.getName());

  // id values are standardized
  //private long id = IdGenerator.createId();
  
  /** Current capacity of this producer in mW */
  private double currentCapacity;
  
  /** Per-timeslot variability */
  private double variability = 0.01;
  
  /** Mean-reversion tendency - portion of variability to revert
   *  back to nominal capacity */
  private double meanReversion = 0.2;
  
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
  
  public void init (BrokerProxy proxy, int seedId, RandomSeedRepo randomSeedRepo)
  {
    log.info("init(" + seedId + ") " + getUsername());
    this.brokerProxyService = proxy;
    this.seed = randomSeedRepo.getRandomSeed(Genco.class.getName(), seedId, "update");
    currentCapacity = nominalCapacity;
  }

  /** True if plant is currently operating */
  public boolean isInOperation ()
  {
    return inOperation;
  }

  /**
   * Nominal or mean capacity of plant. This is the value toward which the
   * mean-reverting random walk reverts.
   */
  public double getNominalCapacity ()
  {
    return nominalCapacity;
  }
  
  /**
   * Fluent setter for nominal capacity
   */
  @ConfigurableValue(valueType = "Double",
      description = "nominal output capacity of this genco in MW")
  @StateChange
  public Genco withNominalCapacity (double capacity)
  {
    this.nominalCapacity = capacity;
    this.currentCapacity = capacity;
    return this;
  }

  /**
   * Ask price for energy from this plant.
   */
  public double getCost ()
  {
    return cost;
  }
  
  /**
   * Fluent setter for nominal capacity
   */
  @ConfigurableValue(valueType = "Double",
      description = "minimum payment/mwh needed to operate this plant")
  @StateChange
  public Genco withCost (double cost)
  {
    this.cost = cost;
    return this;
  }
  
  /**
   * Maximim amount by which capacity can change/timeslot in random walk.
   */
  public double getVariability ()
  {
    return variability;
  }
  
  /**
   * Fluent setter for variability.
   */
  @ConfigurableValue(valueType = "Double",
      description = "max ratio for a step in capacity random walk")
  @StateChange
  public Genco withVariability (double var)
  {
    this.variability = var;
    return this;
  }
  
  /**
   * Probability that this plant will submit asks in any given timeslot
   */
  public double getReliability ()
  {
    return reliability;
  }
  
  /**
   * Fluent setter for reliability.
   */
  @ConfigurableValue(valueType = "Double",
      description = "probability that plant will participate in wholesale market")
  @StateChange
  public Genco withReliability (double reliability)
  {
    this.reliability = reliability;
    return this;
  }

  /**
   * Leadtime to commit energy from this plant, expressed in number of
   * timeslots. Plant will not send orders to the market within this
   * leadtime unless it has at least partially committed power for the
   * timeslot in question.
   */
  public int getCommitmentLeadtime ()
  {
    return commitmentLeadtime;
  }
  
  /**
   * Fluent setter for commitment leadtime.
   */
  @ConfigurableValue(valueType = "Integer",
      description = "minimum leadtime for first commitment, in hours")
  @StateChange
  public Genco withCommitmentLeadtime (int leadtime)
  {
    this.commitmentLeadtime = leadtime;
    return this;
  }
  
  /**
   * Rate at which this plant emits carbon, relative to a coal-fired 
   * thermal plant.
   */
  public double getCarbonEmissionRate ()
  {
    return carbonEmissionRate;
  }
  
  /**
   * Fluent setter for carbonEmissionRate.
   */
  @ConfigurableValue(valueType = "Double",
      description = "carbon emissions/mwh, relative to coal-fired plant")
  @StateChange
  public Genco withCarbonEmissionRate (double rate)
  {
    this.carbonEmissionRate = rate;
    return this;
  }
  
  /**
   * Current capacity, varies by a mean-reverting random walk.
   */
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
    int skip = (commitmentLeadtime
                - Competition.currentCompetition().getDeactivateTimeslotsAhead());
    if (skip < 0)
      skip = 0;
    for (Timeslot slot : openSlots) {
      int slotNum = slot.getSerialNumber();
      double availableCapacity = currentCapacity;
      // do we receive these?
      MarketPosition posn = findMarketPositionByTimeslot(slotNum);
      if (skip-- > 0 && (posn == null || posn.getOverallBalance() == 0.0))
        continue;
      if (posn != null) {
        // posn.overallBalance is negative if we have sold power in this slot
        availableCapacity += posn.getOverallBalance();
      }
      if (availableCapacity > Competition.currentCompetition()
          .getMinimumOrderQuantity()) {
        // make an offer to sell
        Order offer = new Order(this, slotNum, -availableCapacity, cost);
        log.debug(getUsername() + " offers " + availableCapacity + " in "
                  + slotNum + " for " + cost);
        brokerProxyService.routeMessage(offer);
      }
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
  public void setCurrentCapacity (double val)
  {
    currentCapacity = val;
  }

  private void updateInOperation (double val)
  {
    setInOperation(val <= reliability);
  }

  @StateChange
  public void setInOperation (boolean op)
  {
    inOperation = op;
  }
}
