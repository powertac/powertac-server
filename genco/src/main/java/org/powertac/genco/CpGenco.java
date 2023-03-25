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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.Order;
import org.powertac.common.RandomSeed;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.ContextService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import java.time.Instant;
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
  static private Logger log = LogManager.getLogger(CpGenco.class.getName());

  // Needed services
  private WeatherReportRepo weatherReportRepo;
  private WeatherForecastRepo weatherForecastRepo;
  private RandomSeedRepo randomSeedRepo;

  // needed for saving bootstrap state
  private TimeslotRepo timeslotRepo;
  
  /** Price and quantity variability. The value is the ratio of sigma to mean */
  private double pSigma = 0.1;
  private double qSigma = 0.1;

  /** Nominal price interval between bids */
  private double priceInterval = 4.0;

  /** Minimum total offered quantity in MWh */
  private double minQuantity = 120.0;

  /** Minimum quantity in MWh for individual orders */
  private double minBidQuantity = 15.0;

  /** curve generating coefficients as a comma-separated list */
  private List<String> coefficients = Arrays.asList(".007", ".1", "16.0");
  private double[] coefficientArray = null;
  private double[][] timeslotCoefficients; // ring buffer
  private int ringOffset = -1; // uninitialized
  private int lastTsGenerated = -1;

  /** add a knee to the curve where slope rises */
  private double kneeDemand = 25.0;
  private double kneeSlope = 5.0;

  /** random-walk parameters */
  private double rwaSigma = 0.004;
  private double rwaOffset = 0.0025;
  private double rwcSigma = 0.005;
  private double rwcOffset = 0.002;

  /** extreme cooling and heating load thresholds and slopes */
  @ConfigurableValue(valueType = "Double", dump=false,
          description = "Scale factor from MisoBuyer")
  private double misoScaleFactor = 0.049; // scale

  private double coolingThreshold = 35.0;
  private double coolingSlope = 1200.0;
  private double heatingThreshold = 0.0;
  private double heatingSlope = 400.0;

  protected BrokerProxy brokerProxyService;
  protected RandomSeed seed;

  private NormalDistribution normal01;
  private QuadraticFunction function = new QuadraticFunction();

  public CpGenco (String username)
  {
    super(username, true, true);
  }

  public void init (BrokerProxy proxy, int seedId,
                    ContextService context)
  {
    log.info("init(" + seedId + ") " + getUsername());
    this.brokerProxyService = proxy;
    this.timeslotRepo = (TimeslotRepo)context.getBean("timeslotRepo");
    this.randomSeedRepo = (RandomSeedRepo)context.getBean("randomSeedRepo");
    this.weatherReportRepo =
            (WeatherReportRepo)context.getBean("weatherReportRepo");
    this.weatherForecastRepo =
            (WeatherForecastRepo)context.getBean("weatherForecastRepo");
    // set up the random generator
    this.seed =
      randomSeedRepo.getRandomSeed(CpGenco.class.getName(), seedId, "bid");
    normal01 = new NormalDistribution(0.0, 1.0);
    normal01.reseedRandomGenerator(seed.nextLong());
    // set up the supply-curve generating function
    if (!function.validateCoefficients(coefficients))
      log.error("wrong number of coefficients for quadratic");
    int to = Competition.currentCompetition().getTimeslotsOpen();
    timeslotCoefficients = new double[to][getCoefficients().size()];
  }

  /**
   * Generates Orders in the market to sell remaining available capacity.
   */
  public void generateOrders (Instant now, List<Timeslot> openSlots)
  {
    log.info("Generate orders for " + getUsername());
    for (Timeslot slot: openSlots) {
      function.setCoefficients(getTsCoefficients(slot));
      MarketPosition posn =
        findMarketPositionByTimeslot(slot.getSerialNumber());
      double start = 0.0;
      if (posn != null) {
        // posn.overallBalance is negative if we have sold power in this slot
        start = -posn.getOverallBalance();
      }
      // make offers up to minQuantity
      double minQtyForSlot = getAdjustedMinQty(slot.getSerialNumber());
      while (start < minQtyForSlot) {
        log.debug("start qty = " + start);
        double[] ran = normal01.sample(2);
        double price = function.getY(start);
        price += ran[0] * getPSigma();
        double dx = Math.max(function.getDeltaX(start), getMinBidQuantity());
        double std = dx * getQSigma();
        dx = Math.max(0.0, ran[1] * std + dx); // don't go backward
        Order offer = new Order(this, slot.getSerialNumber(), -dx, price);
        log.debug("new order (ts, qty, price): (" + slot.getSerialNumber()
                  + ", " + (-dx) + ", " + price + ")");
        brokerProxyService.routeMessage(offer);
        start += dx;
      }
    }
  }

  // Converts a timeslot to its index in the coefficient ring buffer,
  // filling the correct slot in the ring if needed.
  private double[] getTsCoefficients (Timeslot slot)
  {
    int horizon = timeslotCoefficients.length;
    if (-1 == ringOffset) {
      // first encounter
      ringOffset = slot.getSerialNumber();
      lastTsGenerated = slot.getSerialNumber();
      walkCoefficients(getCoefficientArray(), timeslotCoefficients[0]);
      logCoefficients(slot.getSerialNumber(), timeslotCoefficients[0]);
    }
    int index = (slot.getSerialNumber() - ringOffset) % horizon;
    if (slot.getSerialNumber() > lastTsGenerated) {
      int prev = (slot.getSerialNumber() - ringOffset - 1) % horizon;
      walkCoefficients(timeslotCoefficients[prev], timeslotCoefficients[index]);
      logCoefficients(slot.getSerialNumber(), timeslotCoefficients[index]);
      lastTsGenerated = slot.getSerialNumber();
    }
    return timeslotCoefficients[index];
  }

  // log the coefficients for ts
  private void logCoefficients (int serialNumber, double[] ds)
  {
    log.info("Coefficients for ts " + serialNumber
             + ": [" + ds[0] + ", " + ds[1] + ", " + ds[2] + "]");
  }
  
  // Adjust the min quantity to deal with emergency cooling or heating load
  // in extreme temperatures
  int lastTimeslot = -1;
  WeatherForecast currentForecast = null;
  double getAdjustedMinQty (int timeslotIndex)
  {
    int currentTimeslot = timeslotRepo.currentSerialNumber();
    if (currentTimeslot > lastTimeslot) {
      // update lastTimeslot, retrieve new weather forecast
      lastTimeslot = currentTimeslot;
      currentForecast = weatherForecastRepo.currentWeatherForecast();
    }
    // use the correct forecast
    WeatherForecastPrediction prediction =
            currentForecast.getPredictions().get(timeslotIndex - currentTimeslot - 1);
    double temp = prediction.getTemperature();
    double adj = getEmergencyAdjustment(temp);
    if (adj != 0.0) {
      log.info("Emergency capacity adjustment {} at temp {}", adj, temp);
    }
    return getMinQuantity() + adj;
  }
  
  double getEmergencyAdjustment (double temperature)
  {
    double result = 0.0;
    if (temperature > getCoolingThreshold()) {
      // cooling emergency
      result = (temperature - getCoolingThreshold()) * getCoolingSlope();
    }
    else if (temperature < getHeatingThreshold()) {
      // heating emergency
      result = (getHeatingThreshold() - temperature) * getHeatingSlope();
    }
    return result;
  }

  // String to array conversion
  double[] extractCoefficients (List<String> coeff)
  {
    double[] result = new double[coeff.size()];
    try {
      for (int i = 0; i < coeff.size(); i++) {
        result[i] = (Double.parseDouble((String)coeff.get(i)));
      }
      return result;
    }
    catch (NumberFormatException nfe) {
      log.error("Cannot parse " + coeff + " into a number array");
      return new double[0];
    }
  }

  // Runs one step of the per-timeslot random-walk
  private void walkCoefficients (double[] s0, double[] s1)
  {
    double[] ran = normal01.sample(2); // two samples
    double[] coef = getCoefficientArray();
    s1[0] = s0[0] + ran[0] * rwaSigma * coef[0] + (coef[0] - s0[0]) * rwaOffset;
    s1[1] = s0[1];
    s1[2] = s0[2] + ran[1] * rwcSigma * coef[2] + (coef[2] - s0[2]) * rwcOffset;
  }

  /**
   * Saves coefficients for the current timeslot in the form needed for
   * configuration at the start of the sim session, then adds them to the
   * bootstrap state.
   */
  public void saveBootstrapState (ServerConfiguration serverConfig)
  {
    int horizon = timeslotCoefficients.length;
    int index = (timeslotRepo.currentSerialNumber() - ringOffset) % horizon;
    ArrayList<String> newCoeff = new ArrayList<String>();
    for (Double coeff : timeslotCoefficients[index]) {
      newCoeff.add(coeff.toString());
    }
    coefficients = newCoeff;
    serverConfig.saveBootstrapState(this);
  }

  // ------------ getters & setters -----------------
  /**
   * Returns function coefficients as an array of Strings
   */
  public List<String> getCoefficients ()
  {
    ArrayList<String> result = new ArrayList<String>();
    for (Object thing : coefficients)
      result.add((String)thing);
    return result;
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

  /**
   * Fluent setter for coefficient array
   */
  @ConfigurableValue(valueType = "List",
      bootstrapState = true, dump=false,
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
  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Standard Deviation ratio for bid price")
  @StateChange
  public CpGenco withPSigma (double var)
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
  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Standard Deviation ratio for bid quantity")
  @StateChange
  public CpGenco withQSigma (double var)
  {
    this.qSigma = var;
    return this;
  }

  /**
   * Random-walk sigma for the quadratic coefficient
   */
  public double getRwaSigma ()
  {
    return rwaSigma;
  }

  /**
   * Fluent setter for the random-walk sigma value applied to the
   * quadratic coefficient.
   */
  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Random-walk std dev ratio for quadratic coefficient")
  @StateChange
  public CpGenco withRwaSigma (double var)
  {
    this.rwaSigma = var;
    return this;
  }

  /**
   * Random-walk offset for the quadratic coefficient
   */
  public double getRwaOffset()
  {
    return rwaOffset;
  }

  /**
   * Fluent setter for the random-walk offset value applied to the
   * quadratic coefficient.
   */
  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Random-walk offset ratio for quadratic coefficient")
  @StateChange
  public CpGenco withRwaOffset (double var)
  {
    this.rwaOffset = var;
    return this;
  }

  /**
   * Random-walk sigma for the constant coefficient
   */
  public double getRwcSigma ()
  {
    return rwcSigma;
  }

  /**
   * Fluent setter for the random-walk sigma value applied to the
   * constant coefficient.
   */
  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Random-walk std dev ratio for constant coefficient")
  @StateChange
  public CpGenco withRwcSigma (double var)
  {
    this.rwcSigma = var;
    return this;
  }

  /**
   * Random-walk offset for the constant coefficient
   */
  public double getRwcOffset()
  {
    return rwcOffset;
  }

  /**
   * Fluent setter for the random-walk offset value applied to the
   * constant coefficient.
   */
  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Random-walk offset ratio for constant coefficient")
  @StateChange
  public CpGenco withRwcOffset (double var)
  {
    this.rwcOffset = var;
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
  public CpGenco withPriceInterval (double interval)
  {
    this.priceInterval = interval;
    return this;
  }

  /**
   * Minimum total quantity to offer. The generation function will be run
   * until it hits this value.
   */
  public double getMinBidQuantity ()
  {
    return minBidQuantity;
  }

  /**
   * Fluent setter for minimum total offer quantity.
   */
  @ConfigurableValue(valueType = "Double",
      description = "minimum quantity in MWh for individual Offers")
  @StateChange
  public CpGenco withMinBidQuantity (double qty)
  {
    this.minBidQuantity = qty;
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
   * Fluent setter for minimum total offer quantity.
   */
  @ConfigurableValue(valueType = "Double",
      description = "minimum total quantity to be offered in each timeslot")
  @StateChange
  public CpGenco withMinQuantity (double qty)
  {
    this.minQuantity = qty;
    return this;
  }

  /**
   * Congestion threshold at which slope increases
   */
  public double getKneeDemand ()
  {
    return kneeDemand;
  }

  /**
   * Fluent setter for the congestion threshold
   */
  @ConfigurableValue(valueType = "Double",
      description = "congestion demand threshold")
  @StateChange
  public CpGenco withKneeDemand (double demand)
  {
    this.kneeDemand = demand;
    return this;
  }

  /**
   * Congestion threshold at which slope increases
   */
  public double getKneeSlope()
  {
    return kneeSlope;
  }

  /**
   * Capacity adjustment threshold for extreme cooling load
   */
  @ConfigurableValue(valueType = "Double",
          description = "emergency threshold for extreme cooling load")
  @StateChange
  public CpGenco withCoolingThreshold (double temp)
  {
    this.coolingThreshold = temp;
    return this;
  }
  
  /**
   * Threshold temperature above which total capacity must be adjusted to deal
   * with extreme cooling load. 
   */
  public double getCoolingThreshold ()
  {
    return coolingThreshold;
  }

  /**
   * Capacity adjustment coefficient for extreme cooling load.
   */
  @ConfigurableValue(valueType = "Double",
          description = "per-degree adjustment for extreme heat")
  @StateChange
  public CpGenco withCoolingSlope (double slope)
  {
    this.coolingSlope = slope;
    return this;
  }
  
  /**
   * Per-degree increase in total capacity needed to deal
   * with extreme cooling load. 
   */
  public double getCoolingSlope ()
  {
    return coolingSlope * misoScaleFactor;
  }

  /**
   * Capacity adjustment threshold for extreme heating load.
   */
  @ConfigurableValue(valueType = "Double",
          description = "emergency threshold for extreme heating load")
  @StateChange
  public CpGenco withHeatingThreshold (double temp)
  {
    this.heatingThreshold = temp;
    return this;
  }
  
  /**
   * Threshold temperature below which total capacity must be adjusted to deal
   * with extreme heating load. 
   */
  public double getHeatingThreshold ()
  {
    return heatingThreshold;
  }

  /**
   * Capacity adjustment coefficient for extreme heating load.
   */
  @ConfigurableValue(valueType = "Double",
          description = "per-degree adjustment for extreme cold")
  @StateChange
  public CpGenco withHeatingSlope (double slope)
  {
    this.heatingSlope = slope;
    return this;
  }
  
  /**
   * Per-degree increase in total capacity needed to deal
   * with extreme cooling load. 
   */
  public double getHeatingSlope ()
  {
    return heatingSlope * misoScaleFactor;
  }

  /**
   * Fluent setter for the congestion threshold
   */
  @ConfigurableValue(valueType = "Double",
      description = "congestion demand slope multiplier")
  @StateChange
  public CpGenco withKneeSlope (double mult)
  {
    this.kneeSlope = mult;
    return this;
  }

  /**
   * Function of the form price = a*qty^2 + b*qty + c
   * Probably this should be done with Newton-Rapson, but it's a pain
   * to re-define the polynomial for each bid.
   */
  class QuadraticFunction
  {
    // coefficients
    double a = 1.0;
    double b = 1.0;
    double c = 1.0;

    //final double kneeDemand = 25; //20; // 10; // 30; // 0;// 35; //50;

    boolean validateCoefficients (List<String> coefficients)
    {
      double[] test = extractCoefficients(coefficients);
      if (test.length != 3)
        return false;
      return true;
    }

    void setCoefficients (double[] coef)
    {
      a = coef[0];
      b = coef[1];
      c = coef[2];
    }

    // returns the delta-x for a given x value and the nominal y-interval
    // given by priceInterval. This is the quadratic formula with
    // c = as^2 + bs + p where s is startX and p is priceInterval 
    double getDeltaX (double startX)
    {
      // store
      double aOrig = a;
      double bOrig = b;
      double cOrig = c;
      double priceIntervalOrig = priceInterval;

      if (startX >= kneeDemand) {
        scaleCoefficients(); 
      }


      double endX =
        -b / (2.0 * a)
            + Math.sqrt(b * b
                        + 4.0 * a * (a * startX * startX
                                     + b * startX
                                     + priceInterval)) / (2.0 * a);

      
      // restore
      a = aOrig;
      b = bOrig;
      c = cOrig;
      priceInterval = priceIntervalOrig;

      return (endX - startX);
    }

    // returns a price given a qty
    double getY (double x)
    {
      // store
      double aOrig = a;
      double bOrig = b;
      double cOrig = c;
      double priceIntervalOrig = priceInterval;
      if (x >= kneeDemand) {
        scaleCoefficients(); 
      }
    
      double result = (a * x * x + b * x + c);
    
      // restore
      a = aOrig;
      b = bOrig;
      c = cOrig;
      priceInterval = priceIntervalOrig;
      return result;
    }

    private void scaleCoefficients() {
      double y0 = a * kneeDemand * kneeDemand + b * kneeDemand + c;
      a *= kneeSlope; // 4; // 3; // 4; // 3; // 2;
      b *= kneeSlope; // 4; // 3; // 4; // 3; // 2;
      c = y0 - a * kneeDemand * kneeDemand - b * kneeDemand;
      priceInterval *= 2;
    }
  }

}
