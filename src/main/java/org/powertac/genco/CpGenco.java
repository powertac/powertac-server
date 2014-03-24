/*
 * Copyright 2014 the original author or authors.
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

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.*;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a set of bulk producers distributed across the transmission
 * domain.
 * The assumption is that there will be exactly one of these operating in
 * the wholesale side of the Power TAC day-ahead market.
 * 
 * This instance submits bids in a way that is intended to mimic the price
 * curve at a load node subject to congestion pricing. The function is a
 * polynomial. Its
 * coefficients are configurable, as are the nominal interval between bid prices
 * (to create the piecewise-linear supply curve) and the variability of
 * price and quantity per bid.
 * 
 * @author John Collins
 */
@Domain
@ConfigurableInstance
public class CpGenco extends Broker
{
  static private Logger log = Logger.getLogger(CpGenco.class.getName());

  /** Price and quantity variability. The value is the ratio of sigma to mean */
  private double pSigma = 0.05;
  private double qSigma = 0.1;

  /** Price interval between bids */
  private double priceInterval = 4.0;

  /** Minimum total offered quantity in MWh */
  private double minQuantity = 120.0;

  /** curve generating coefficients as a comma-separated list */
  private List<String> coefficients = Arrays.asList(".007", ".1", "16.0");
  private double[] coefficientArray = null;

  protected BrokerProxy brokerProxyService;
  protected RandomSeed seed;

  private NormalDistribution normal01;
  private QuadraticFunction function;

  public CpGenco (String username)
  {
    super(username, true, true);
  }

  public void
    init (BrokerProxy proxy, int seedId, RandomSeedRepo randomSeedRepo)
  {
    log.info("init(" + seedId + ") " + getUsername());
    this.brokerProxyService = proxy;
    this.seed =
      randomSeedRepo.getRandomSeed(CpGenco.class.getName(), seedId, "bid");
    normal01 = new NormalDistribution(0.0, 1.0);
    normal01.reseedRandomGenerator(seed.nextLong());
    function = new QuadraticFunction();
    if (!function.validateCoefficients(coefficients))
      log.error("wrong number of coefficients for quadratic");
  }

  /**
   * Returns function coefficients as a comma-separated string.
   */
  public List<String> getCoefficients ()
  {
    return coefficients;
  }

  /**
   * Returns coefficients as a array.
   */
  public double[] getCoefficientArray ()
  {
    if (null == coefficientArray) {
      coefficientArray = extractCoefficients(coefficients);
    }
    return coefficientArray;
  }

  // String to array conversion
  double[] extractCoefficients (List<String> coeff)
  {
    double[] result = new double[coeff.size()];
    try {
      for (int i = 0; i < coeff.size(); i++) {
        result[i] = (Double.parseDouble(coeff.get(i)));
      }
      return result;
    }
    catch (NumberFormatException nfe) {
      log.error("Cannot parse " + coeff + " into a number array");
      return new double[0];
    }
  }

  /**
   * Fluent setter for coefficient array
   */
  @ConfigurableValue(valueType = "List",
      description = "Coefficients for the specified function type")
  @StateChange
  public CpGenco withCoefficients (List<String> coeff)
  {
    if (function.validateCoefficients(coeff)) {
      coefficients = coeff;
    }
    else {
      log.error("incorrect number of coefficients");
    }
    return this;
  }

  /**
   * Std deviation ratio for bid price.
   */
  public double getPSigma ()
  {
    return pSigma;
  }

  /**
   * Fluent setter for price variability. The value is ratio of the standard
   * deviation to the nominal bid price for a given bid.
   */
  @ConfigurableValue(valueType = "Double",
      description = "Standard Deviation ratio for bid price")
  @StateChange
  public
    CpGenco withPSigma (double var)
  {
    this.pSigma = var;
    return this;
  }

  /**
   * Std deviation ratio for bid quantity.
   */
  public double getQSigma ()
  {
    return qSigma;
  }

  /**
   * Fluent setter for price variability. The value is ratio of the standard
   * deviation to the nominal bid quantity for a given bid.
   */
  @ConfigurableValue(valueType = "Double",
      description = "Standard Deviation ratio for bid quantity")
  @StateChange
  public
    CpGenco withQSigma (double var)
  {
    this.qSigma = var;
    return this;
  }

  /**
   * Difference between sequential nominal bid prices
   */
  public double getPriceInterval ()
  {
    return priceInterval;
  }

  /**
   * Fluent setter for price interval. Bigger values create a more coarse
   * piecewise approximation of the supply curve.
   */
  @ConfigurableValue(valueType = "Double",
      description = "Nominal price interval between successive bids")
  @StateChange
  public
    CpGenco withPriceInterval (double interval)
  {
    this.priceInterval = interval;
    return this;
  }

  /**
   * Minimum total quantity to offer. The generation function will be run
   * until it hits this value.
   */
  public double getMinQuantity ()
  {
    return minQuantity;
  }

  /**
   * Fluent setter for minimum total quantity.
   */
  @ConfigurableValue(valueType = "Double",
      description = "minimum leadtime for first commitment, in hours")
  @StateChange
  public
    CpGenco withMinQuantity (double qty)
  {
    this.minQuantity = qty;
    return this;
  }

  /**
   * Generates Orders in the market to sell available capacity. No Orders
   * are submitted if the plant is not in operation.
   */
  public void generateOrders (Instant now, List<Timeslot> openSlots)
  {
    log.info("Generate orders for " + getUsername());
    for (Timeslot slot: openSlots) {
      MarketPosition posn =
        findMarketPositionByTimeslot(slot.getSerialNumber());
      double start = 0.0;
      if (posn != null) {
        // posn.overallBalance is negative if we have sold power in this slot
        start = -posn.getOverallBalance();
      }
      // make offers up to minQuantity
      while (start < minQuantity) {
        log.debug("start qty = " + start);
        double[] ran = normal01.sample(2);
        double price = function.getY(start);
        double std = price * getPSigma();
        price += ran[0] * std;
        double dx = function.getDeltaX(start);
        std = dx * getQSigma();
        dx = Math.max(0.0, ran[1] * std + dx); // don't go backward
        Order offer = new Order(this, slot.getSerialNumber(), -dx, price);
        log.debug("new order (ts, qty, price): (" + slot.getSerialNumber()
                  + ", " + (-dx) + ", " + price + ")");
        brokerProxyService.routeMessage(offer);
        start += dx;
      }
    }
  }

  /**
   * Function of the form price = a*qty^2 + b*qty + c
   * Probably this should be done with Newton-Rapson, but it's a pain
   * to re-define the polynomial for each bid.
   */
  class QuadraticFunction
  {
    double minGranules = 20;
    double epsilon = 0;

    boolean validateCoefficients (List<String> coefficients)
    {
      double[] test = extractCoefficients(coefficients);
      if (test.length != 3)
        return false;
      return true;
    }

    // lazy-evaluator for epsilon finds a value that gives some level of
    // granularity for the last bid
    double getEpsilon ()
    {
      if (epsilon == 0.0) {
        double[] ca = getCoefficientArray();
        double dx =
          priceInterval * 2.0 * ca[0] + ca[1];
        epsilon = dx / minGranules;
      }
      return epsilon;
    }

    // returns the delta-x for a given x value and the nominal y-interval
    // given by priceInterval. We'll use a fixed epsilon to avoid having to
    // invert the function.
    double getDeltaX (double startX)
    {
      double lastX = startX;
      double startY = getY(lastX);
      while (getY(lastX) < startY + priceInterval) {
        lastX += getEpsilon();
      }
      return (lastX - startX);
    }

    // returns a price given a qty
    double getY (double x)
    {
      double[] ca = getCoefficientArray();
      return (ca[0] * x * x + ca[1] * x + ca[2]);
    }
  }

}
