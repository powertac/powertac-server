/*
 * Copyright (c) 2012-2020 by John Collins
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
package org.powertac.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.msg.MarketBootstrapData;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Probe object that can be used by customer models and other players
 * to generate cost estimates for tariffs, including a risk-adjusted
 * estimates of the actual cost of
 * variable-rate tariffs. There are four values for a variable-rate tariff
 * that must be combined to generate an estimate: the broker's claim of
 * the expectedMean price, the brokers commitment to a maxValue of the
 * price, the actual experienced realizedPrice, and the amount of power
 * that has been sold through the tariff. The assumption is that the
 * actual realizedPrice would be more predictive for a tariff
 * with a more substantial price history (large amount of power sold).
 * 
 * Note that some of the data needed to estimate price comes from the
 * Tariff's Rates, which may apply at certain times and/or under certain
 * tier threshold conditions, and some comes from the Tariff itself.
 * 
 * The price estimate is generated as <br/>
 * &nbsp;&nbsp;
 * alpha * (wtExpected * expectedMean + wtMax * maxValue)<br/>
 * &nbsp;&nbsp;&nbsp;&nbsp;
 * + (1 - alpha) * realizedPrice<br/>
 * where alpha = 1 - wtRealized * (1 - 1 / (1 + totalKWh / soldThreshold)).
 * 
 * In the case where multiple variable Rates apply, the values of 
 * expectedMean and maxValue are the usage-weighted means of the
 * corresponding values from the individual Rates.
 * 
 * Usage: A Customer may need to evaluate multiple tariff offerings by
 * estimating cost over a week or more of 1-hour timeslots. Tier usage
 * is computed over a 24-hour period. Therefore, the recommended usage
 * is to create a single instance of this class for each customer model,
 * and re-initialize it for each tariff. Therefore, only a default
 * constructor is provided, along with an init() method that clears out
 * state and optionally sets parameter values.
 * 
 * Evaluation of time-of-use rates depends on being able to compute the hour
 * of the day for some arbitrary offset. This functionality depends on the
 * Joda Time "default time zone" being set to DateTimeZone.UTC. This normally
 * happens in the TimeService, but may also need to be done in test code.
 * 
 * @author John Collins
 */
public class TariffEvaluationHelper
{
  static private Logger log =
          LogManager.getLogger(TariffEvaluationHelper.class.getName());

  // weights
  private double wtExpected = 0.6;
  private double wtMax = 0.4;
  private double wtRealized = 0.8;
  private double soldThreshold = 10000.0;

  // Expected regulation quantities in kWh / timeslot
  private double expCurtail = 0.0;
  private double expDischarge = 0.0;
  private double expDown = 0.0;

  // Regulation discount coefficients
  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Ratio at which up-regulation discount function is 0.5")
  private Double upregHalf = 4.4;
  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Slope of up-regulation discount function")
  private Double upregSlope = 3.0;
  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Ratio at which down-regulation discount function is 0.5")
  private Double downregHalf = 0.7;
  @ConfigurableValue(valueType = "Double",
          publish = true,
          description = "Slope of down-regulation discount function")
  private Double downregSlope = 15.0;

  // Bootstrap market data for computing regulation reward
  //private MarketBootstrapData marketBootstrapData;

  // normalized weights
  private double normWtExpected = 0.0;
  private double normWtMax = 0.0;

  // evaluation state
  private double alpha = 0.0;
  private Tariff tariff = null;
  
  // need time service, but this is not a Spring managed bean
  private TimeService timeService;

  /**
   * Default constructor
   */
  public TariffEvaluationHelper ()
  {
    super();
  }

  /**
   * Initializes, setting parameters, then normalize the weights for
   * expectedMean and maxValue.
   */
  public void init (double wtExpected, double wtMax,
                    double wtRealized, double soldThreshold)
  {
    setWtExpected(wtExpected);
    setWtMax(wtMax);
    setWtRealized(wtRealized);
    this.soldThreshold = soldThreshold;
    init();
  }
  
  /**
   * Initializes, without changing parameter settings
   */
  public void init ()
  {
    alpha = 0.0;
    tariff = null;
    normalizeWeights();
    if (null == timeService) {
      try {
        timeService =
            (TimeService) SpringApplicationContext.getBean("timeService");
      }
      catch (IllegalStateException ise) {
        log.warn("SpringApplicationContext is closed");
      }
    }
    // for non-Spring test environment
    if (null == timeService) {
      timeService = TimeService.getInstance();
      //log.warn("Direct retrieval of instance " + timeService);
    }
  }
  
  /**
   * Initializes cost factors and normalizes
   */
  public void
  initializeCostFactors (double wtExpected, double wtMax,
                         double wtRealized, double soldThreshold)
  {
    this.init(wtExpected, wtMax, wtRealized, soldThreshold);
  }

  /**
   * Initializes regulation factors. Applicable only for tariffs with
   * {@link RegulationRate}s. See Section 4.1.1 of the spec for
   * details, especially Eq. 5. Signs are from the standpoint of the
   * customer -- expectedCurtailment and expectedDischarge are negative,
   * expectedDownReg is positive.
   * 
   * If this method is not called, default values are zero for all factors.
   */
  public void initializeRegulationFactors (double expectedCurtailment,
                                           double expectedDischarge,
                                           double expectedDownReg)
  {
    this.expCurtail = expectedCurtailment;
    this.expDischarge = expectedDischarge;
    this.expDown = expectedDownReg;
  }

  /**
   * Initializes regulation rate discount factors. Applicable only for
   * tariffs with {@link RegulationRate}s.
   */
  public void initializeRegulationDiscount (double upregHalf,
                                            double upregSlope,
                                            double downregHalf,
                                            double downregSlope)
  {
    this.upregHalf = upregHalf;
    this.upregSlope = upregSlope;
    this.downregHalf = downregHalf;
    this.downregSlope = downregSlope;
  }

  /**
   * Retrieves the market bootstrap data from the Competition.
   * Needed to discount regulation rates.
   */
  public MarketBootstrapData getMarketBootstrapData ()
  {
    return Competition.currentCompetition().getMarketBootstrapData();
  }

  /**
   * Estimate the total cost of buying the given amounts of power
   * from the given tariff, starting in the timeslot identified by startIndex.
   * Payments include usage charges, and periodic payments just in case
   * <code>includePeriodicCharge</code> is true. They do not
   * include signup or withdrawal charges.
   * Note that there is a strong assumption that the projected usage
   * is for a single customer, not the total population in some model.
   * This assumption is embedded in the structure of usage tiers in the
   * tariff.
   */
  public double estimateCost (Tariff tariff, double[] usage,
                              Instant start,
                              boolean includePeriodicCharge)
  {
    init();
    this.tariff = tariff;
    computeAlpha(tariff);
    //double dailyUsage = 0.0;
    double result = 0.0;
    Instant time = start;
    if (null == time) {
      log.error("Time is null!");
      time = timeService.getCurrentTime();
    }
    for (int index = 0; index < usage.length; index++) {
      time = time.plus(TimeService.HOUR);
      result += tariff.getUsageCharge(time, usage[index], this);
      if (includePeriodicCharge)
        result += tariff.getPeriodicPayment() / 24.0;
//      if (time.toDateTime().getHourOfDay() == 0) {
//        //reset the daily usage counter
//        dailyUsage = 0.0;
//      }
//      else {
//        dailyUsage += usage[index];
//      }
    }
    // Account for regulation. In general, customers get paid (+) for up-reg
    // and must pay (-) for down. Also, in general, the amounts are positive
    // for up-reg and negative for down-reg. This explains the signs used here.
    double adj = 0.0;
    if (tariff.hasRegulationRate()) {
      //RegulationRate regRate =
      //        tariff.getTariffSpecification().getRegulationRates().get(0);
      if (null == getMarketBootstrapData())
        adj += computeNaiveReg(tariff);
      else
        adj += computeDiscountedReg(tariff);
      log.info("regulation adjustment {}", adj);
    }
    return result + adj * usage.length;
  }

  // discount regulation using logistic function
  // note that this is never called in a boot session
  double computeDiscountedReg (Tariff tariff)
  {
    double result = 0.0;
    double mktPrice = getMarketBootstrapData().getMeanMarketPrice() / -1000.0; // kWh

    // Discount with logistic function
    // 1-1/(1+exp(-3*(x-4))) produces 0.95 for upreg rate 3 times mkt price,
    // 0.5 for rate 4 time mean mkt price
    double upregPrice = tariff.getRegulationCharge(-1.0, false); 
    double upregPriceRatio = upregPrice / mktPrice;
    double upregDiscount = upRegulationDiscount(upregPriceRatio);
    double upreg = (expCurtail + expDischarge);
    result += upregDiscount * tariff.getRegulationCharge(upreg, false);
    double downregPrice = tariff.getRegulationCharge(1.0, false); 
    double downregPriceRatio = 2.0 + downregPrice / mktPrice;
    double downregDiscount = downRegulationDiscount(downregPriceRatio);
    result += downregDiscount * tariff.getRegulationCharge(expDown, false);
    log.info("mp {}, upregRatio {}, upreg discount {}, downregRatio {}, downreg discount {}",
             mktPrice, upregPriceRatio, upregDiscount,
             downregPriceRatio, downregDiscount);
    return result;
  }

  // Computes up-regulation evaluation discount using a logistic curve having
  // a slope and 50% point given by the upregSlope and upregHalf parameters.
  double upRegulationDiscount (double priceRatio)
  {
    return 1.0 - 1.0 / (1.0 + Math.exp(-1.0
                                       * upregSlope
                                       * (priceRatio - upregHalf)));
  }

  // Computes up-regulation evaluation discount using a logistic curve having
  // a slope and 50% point given by the
  // downregSlope and downregHalf parameters.
  double downRegulationDiscount (double priceRatio)
  {
    return 1.0 - 1.0 / (1.0 + Math.exp(-1.0
                                       * downregSlope
                                       * (priceRatio - downregHalf)));
  }

  // Computes non-discounted regulation value for bootstrap use
  double computeNaiveReg (Tariff tariff)
  {
    double result = 0.0;
    double upreg = expCurtail + expDischarge;
    result += tariff.getRegulationCharge(upreg, false);
    result += tariff.getRegulationCharge(expDown, false);
    return result;
  }

  /**
   * Estimate cost for a profile starting in the next timeslot
   */
  public double estimateCost (Tariff tariff, double[] usage,
                              boolean includePeriodicCharge)
  {
    return estimateCost(tariff, usage,
                        timeService.getCurrentTime(),
                        includePeriodicCharge);
  }

  /**
   * Returns aggregate estimated cost, including periodic charges
   */
  public double estimateCost (Tariff tariff, double[] usage, Instant start)
  {
    return estimateCost(tariff, usage, start, true);
  }

  /**
   * Returns aggregate estimated cost, including periodic charges,
   * starting in the next timeslot.
   */
  public double estimateCost (Tariff tariff, double[] usage)
  {
    return estimateCost(tariff, usage, true);
  }

  private void computeAlpha (Tariff tariff)
  {
    alpha = 1.0 - getWtRealized()
            * (1.0 - 1.0 / (1.0 + tariff.getTotalUsage()
                                  / getSoldThreshold()));
  }

  /**
   * Returns the cost estimate in the form of an array of the same shape
   * as the usage vector. Each element of the result corresponds to the
   * corresponding element of the usage array. Periodic charges are included
   * just in case <code>includePeriodicCharge</code> is true.
   */
  public double[] estimateCostArray (Tariff tariff, double[] usage,
                                     boolean includePeriodicCharge)
  {
    init();
    this.tariff = tariff;
    computeAlpha(tariff);
//    double dailyUsage = 0.0;
    double[] result = new double[usage.length];
    Instant time = timeService.getCurrentTime();
    for (int index = 0; index < usage.length; index++) {
      time = time.plus(TimeService.HOUR);
      result[index] = tariff.getUsageCharge(time, usage[index], this);
      if (includePeriodicCharge)
        result[index] += tariff.getPeriodicPayment() / 24.0;
//      if (timeService.getHourOfDay() == 0) {
//        //reset the daily usage counter
//        dailyUsage = 0.0;
//      }
//      else {
//        dailyUsage += usage[index];
//      }
    }
    return result;    
  }
  
  /**
   * Returns a cost estimate in array form, including periodic charges.
   */
  public double[] estimateCostArray (Tariff tariff, double[] usage)
  {
    return estimateCostArray(tariff, usage, true);
  }

  /**
   * Combines the expectedMean and maxValue of a Rate as specified by
   * parameters.
   */
  double getWeightedValue (Rate rate)
  {
    return (alpha * (getNormWtExpected() * rate.getExpectedMean()
                     + getNormWtMax() * rate.getMaxValue())
            + (1.0 - alpha) * tariff.getRealizedPrice());
  }
  
  /**
   * Parameter access
   */
  // expected
  public double getWtExpected ()
  {
    return wtExpected;
  }
  
  public double getNormWtExpected ()
  {
    return normWtExpected;
  }
  
  public void setWtExpected (double wt)
  {
    wtExpected = wt;
    normalizeWeights();
  }

  // max
  public double getWtMax ()
  {
    return wtMax;
  }

  public double getNormWtMax ()
  {
    return normWtMax;
  }

  public void setWtMax (double wt)
  {
    wtMax = wt;
    normalizeWeights();
  }

  // wr
  public double getWtRealized ()
  {
    return wtRealized;
  }

  public void setWtRealized (double wt)
  {
    if (wt < 0.0 || wt > 1.0) {
      log.error("realizedPrice weight " + wt + " out of range");
      wt = Math.min(Math.max(wt, 0.0), 1.0);
    }
    wtRealized = wt;
  }

  // st
  public double getSoldThreshold ()
  {
    return soldThreshold;
  }

  public void setSoldThreshold (double st)
  {
    soldThreshold = st;
  }

  /**
   * Returns the expected-curtailment-per-timeslot quantity
   */
  public double getExpectedCurtailment ()
  {
    return expCurtail;
  }

  /**
   * Returns the expected-discharge-per-timeslot quantity
   */
  public double getExpectedDischarge ()
  {
    return expDischarge;
  }

  /**
   * Returns the expected-down-regulation-per-timeslot quantity
   */
  public double getExpectedDownRegulation ()
  {
    return expDown;
  }

  public double getExpectedRegulation ()
  {
    return expCurtail + expDischarge + expDown;
  }

  public Double getUpregHalf ()
  {
    return upregHalf;
  }

  public Double getUpregSlope ()
  {
    return upregSlope;
  }

  public Double getDownregHalf ()
  {
    return downregHalf;
  }

  public Double getDownregSlope ()
  {
    return downregSlope;
  }

  public static Logger getLog ()
  {
    return log;
  }

  public double getExpCurtail ()
  {
    return expCurtail;
  }

  public double getExpDischarge ()
  {
    return expDischarge;
  }

  public double getExpDown ()
  {
    return expDown;
  }

  public double getAlpha ()
  {
    return alpha;
  }

  public Tariff getTariff ()
  {
    return tariff;
  }

  public TimeService getTimeService ()
  {
    return timeService;
  }

  // normalizes the weights for expected and max so they add to 1
  private void normalizeWeights ()
  {
    double sum = wtExpected + wtMax;
    normWtExpected = wtExpected / sum;
    normWtMax = wtMax / sum;
  }
}
