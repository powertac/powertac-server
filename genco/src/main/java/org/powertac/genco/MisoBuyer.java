/*
 * Copyright 2017 by John Collins.
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
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.state.Domain;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.List;

/**
 * Buys energy to meet demand in a large wholesale market. Demand is
 * determined by running a model composed of a mean value, daily and weekly
 * seasonal components, accompanying white noise, and 
 * a residual trend modeled by a smoothed zero-reverting random walk,
 * originally trained on two years of MISO north-central region actual demand.
 * The result is further adjusted using matching temperature data to reflect
 * heating/cooling load.
 * 
 * @author John Collins
 */
@Domain
@ConfigurableInstance
public class MisoBuyer extends Broker
{
  static private Logger log = LogManager.getLogger(MisoBuyer.class.getName());

  /** daily seasonal pattern, starting at midnight */
  private double[] daily =
    {-2393.83341, -2688.62378, -2792.78342, -2660.37941,
     -2150.46655, -1174.19920, -51.62303, 718.08210,
     1134.23055, 1413.31114, 1562.54868, 1577.97041,
     1542.29658, 1432.91036, 1278.64975, 1163.69339,
     1156.57610, 1231.43676, 1181.07369, 1043.90000,
     678.13380, -181.69625, -1136.53189, -1884.67637};
  private int dailyOffset = 0;

  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Std Deviation of random component for daily decomposition")
  private double dailySd = 962.1;

  /** weekly seasonal pattern, starting Monday midnight */
  private double[] weekly =
      // Monday
    {-391.956124, -290.683849, -190.908896, -95.941919,
     -9.247313, 67.748080, 136.276149, 199.551517,
     259.200890, 313.829945, 362.222979, 404.930426,
     442.639849, 475.720723, 504.416722, 529.129761,
     550.764179, 570.508517, 588.361846, 602.249309,
     610.772267, 614.360477, 614.586834, 613.460046,
     // Tuesday
     612.133501, 611.296938, 611.532207, 612.732043,
     613.768341, 614.117323, 614.812678, 616.355549,
     618.885444, 621.676821, 624.064983, 625.993192,
     627.009478, 627.471051, 628.021006, 628.753266,
     629.507393, 630.115352, 629.997488, 629.098931,
     628.161883, 627.483189, 626.728283, 625.954901,
     // Wednesday
     625.331741, 624.923167, 624.703018, 624.606700,
     624.968025, 625.652540, 625.862297, 625.737377,
     626.178309, 627.357172, 628.824034, 630.389750,
     632.076111, 633.756634, 635.328074, 636.819004,
     638.049602, 638.373596, 637.695212, 636.723976,
     636.427187, 636.970439, 637.781538, 638.210263,
     // Thursday
     637.533390, 635.553704, 632.108640, 626.988126,
     620.496738, 613.695340, 607.934378, 603.624349,
     600.281932, 598.144188, 597.303552, 596.420628,
     594.815396, 592.637864, 589.973517, 586.716927,
     582.221489, 575.388487, 566.432799, 557.630960,
     550.424576, 543.957684, 536.750036, 528.266843,
     // Friday
     518.038246, 505.243999, 489.644433, 471.064326,
     448.697725, 420.854854, 386.380165, 346.484864,
     305.453093, 268.834939, 238.531056, 211.783197,
     185.122345, 157.207042, 127.763091,  94.898191,
     51.980893, -12.858454, -103.177756, -203.860646,
     -297.669207, -381.460148, -459.586583, -535.636295,
     // Saturday
     -612.571639, -691.071811, -767.985936, -839.357742,
     -902.197286, -954.919676, -998.738977, -1036.815195,
     -1071.487920, -1102.300876, -1127.929623, -1148.535939,
     -1164.805963, -1177.982452, -1189.640789, -1201.242393,
     -1215.083427, -1233.606003, -1257.531599, -1285.099968,
     -1313.843867, -1341.722391, -1366.294963, -1384.891082,
     // Sunday
     -1382.742458, -1390.097279, -1394.546495, -1396.139033,
     -1392.962053, -1383.777293, -1369.444464, -1352.444565,
     -1336.640762, -1326.272731, -1322.323138, -1320.852110,
     -1317.297154, -1309.794006, -1297.312685, -1276.913022,
     -1239.673187, -1170.634150, -1064.955721, -940.217736,
     -817.917112, -703.353560, -593.813764, -493.662345};
  private int weeklyOffset = 0;

  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Std deviation of random component for weekly decomposition")
  private double weeklySd = 586.1;

  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Mean value of demand timeseries")
  private double mean = 13660.0;

  @ConfigurableValue(valueType = "Double", dump=false,
      description = "Std deviation of residual random walk")
  private double walkSd = 60;

  @ConfigurableValue(valueType = "Double", dump=false,
      description = "mean-reversion parameter for residual random walk")
  private double walkz = 0.007;

  @ConfigurableValue(valueType = "Double", dump=false,
      description = "exponential smoothing parameter for residual random walk")
  private double walkAlpha = 0.02;

  // Ratio of Power TAC market size to MISO market size
  private double scaleFactor = 670.0 / 13660.0;

  // Heating and cooling degree-hours are smoothed sequences, which means
  // the value in the previous timeslot is used to compute the value for
  // the current timeslot. In each timeslot we get 24 forecasts, each of which
  // must be used to re-compute the smoothed values for those timeslots.
  // Each of these parameters is configurable through a fluent setter.

  private double coolThreshold = 20.0;
  private double coolCoef = 1200.0;
  private double heatThreshold = 17.0;
  private double heatCoef = -170.0;
  private double tempAlpha = 0.1;

  private int timeslotOffset = 0;
  private int timeslotsOpen = 0;

  //private ContextService service;
  private BrokerProxy brokerProxyService;
  private WeatherReportRepo weatherReportRepo;
  private WeatherForecastRepo weatherForecastRepo;
  private RandomSeed tsSeed;
  private double minOrderQty = 0.0;

  // needed for saving bootstrap state
  private TimeslotRepo timeslotRepo;

  private ComposedTS timeseries;

  public MisoBuyer (String username)
  {
    super(username, true, true);
  }

  public void init (BrokerProxy proxy, int seedId, ContextService service)
  {
    log.info("init(" + seedId + ") " + getUsername());
    timeslotsOpen = Competition.currentCompetition().getTimeslotsOpen();
    this.brokerProxyService = proxy;
    //this.service = service;
    this.timeslotRepo = (TimeslotRepo)service.getBean("timeslotRepo");
    this.weatherReportRepo =
        (WeatherReportRepo)service.getBean("weatherReportRepo");
    this.weatherForecastRepo =
        (WeatherForecastRepo)service.getBean("weatherForecastRepo");
    RandomSeedRepo randomSeedRepo =
        (RandomSeedRepo)service.getBean("randomSeedRepo");
    // set up the random generator
    this.tsSeed =
      randomSeedRepo.getRandomSeed(MisoBuyer.class.getName(), seedId, "ts");
    // compute offsets for daily and weekly seasonal data
    int ts = timeslotRepo.currentSerialNumber();
    timeslotOffset = ts;
    ZonedDateTime start = timeslotRepo.getDateTimeForIndex(ts);
    dailyOffset = start.get(ChronoField.HOUR_OF_DAY);
    weeklyOffset = (start.get(ChronoField.DAY_OF_WEEK) - 1) * 24 + dailyOffset;
    timeseries = new ComposedTS();
    timeseries.initialize(ts);
  }

  /**
   * Generates Orders in the market to sell remaining available capacity.
   */
  public void generateOrders (Instant now, List<Timeslot> openSlots)
  {
    log.info("Generate orders for " + getUsername());
    double[] tempCorrections =
        computeWeatherCorrections();
    int i = 0;
    for (Timeslot slot: openSlots) {
      int index = slot.getSerialNumber();
      MarketPosition posn =
        findMarketPositionByTimeslot(index);
      double start = 0.0;
      double demand = computeScaledValue(index, tempCorrections[i++]);
      if (posn != null) {
        // posn.overallBalance is negative if we have sold power in this slot
        start = posn.getOverallBalance();
      }
      double needed = demand - start;
      if (Math.abs(needed) < getMinOrderQty())
        continue;
      Order offer = new Order(this, index, needed, null);
      log.info(getUsername() + " orders " + needed +
                  " ts " + index);
      brokerProxyService.routeMessage(offer);
    }
  }

  // Lazy accessor for Competition.minimumOrderQuantity
  private double getMinOrderQty ()
  {
    if (minOrderQty == 0.0)
      minOrderQty = Competition.currentCompetition().getMinimumOrderQuantity();
    return minOrderQty;
  }

  // Computes weather-based demand corrections for each forecast.
  // Note that this code produces demand corrections for each forecast
  // prediction, not necessarily for each open timeslot.
  private double lastHeat = 0.0;
  private double lastCool = 0.0;
  double[] computeWeatherCorrections ()
  {
    WeatherReport weather = weatherReportRepo.currentWeatherReport();
    WeatherForecastPrediction[] forecasts = getForecastArray();
    // smooth the current heat and cool sequences
    // Note that "heat" refers to energy needed for heating
    double thisHeat =
        Math.min(0.0, (weather.getTemperature() - heatThreshold));
    lastHeat = tempAlpha * thisHeat + (1.0 - tempAlpha) * lastHeat;
    double[] smoothedHeat =
        smoothForecasts(lastHeat, heatThreshold, -1.0, forecasts);
    double thisCool =
        Math.max(0.0,(weather.getTemperature() - coolThreshold));
    lastCool = tempAlpha * thisCool + (1.0 - tempAlpha) * lastCool;
    double[] smoothedCool =
        smoothForecasts(lastCool, coolThreshold, 1.0, forecasts);

    double[] result = new double[forecasts.length];
    Arrays.fill(result, 0.0);
    for (int i = 0; i < forecasts.length; i += 1) {
      result[i] += smoothedHeat[i] * heatCoef;
      result[i] += smoothedCool[i] * coolCoef;
    }

    return result;
}

  // Smooths a forecast sequence. Heat and cool sequences are smoothed using
  // the same alpha value. The sign parameter is used to filter relevant
  // differences -- positive for cooling, negative for heating.
  double[] smoothForecasts (double start, double threshold, double sign,
                            WeatherForecastPrediction[] forecasts)
  {
    double[] result = new double[forecasts.length];
    double last = start;
    for (int i = 0; i < forecasts.length; i += 1) {
      double next = forecasts[i].getTemperature() - threshold;
      if (Math.signum(next) != Math.signum(sign))
        next = 0.0;
      last = tempAlpha * next + (1.0 - tempAlpha) * last;
      result[i] = last;
    }
    return result;
  }

  // Converts prediction list to array, indexed by time offset
  WeatherForecastPrediction[] getForecastArray ()
  {
    WeatherForecast forecast = weatherForecastRepo.currentWeatherForecast();
    List<WeatherForecastPrediction> fcsts = forecast.getPredictions();
    WeatherForecastPrediction[] result =
        new WeatherForecastPrediction[fcsts.size()];
    fcsts.forEach(fcst -> result[fcst.getForecastTime() - 1] = fcst);
    return result;
  }

  // timeseries parameter access
  double getDailyValue (int ts)
  {
    int index = 
        Math.floorMod((ts - getTimeslotOffset() + getDailyOffset()),
                      daily.length);
    return daily[index];
  }

  double getWeeklyValue (int ts)
  {
    int index =
        Math.floorMod((ts - getTimeslotOffset() + getWeeklyOffset()),
                      weekly.length);
    return weekly[index];
  }

  // Returns the scaled timeseries value for timeslot ts, adjusted for
  // weather
  double computeScaledValue (int ts, double weatherCorrection)
  {
    double result = timeseries.getValue(ts);
    result += weatherCorrection;
    return result * scaleFactor;
  }

  // parameter and data access
  double getMean ()
  {
    return mean;
  }

  int getTimeslotOffset ()
  {
    return timeslotOffset;
  }

  int getDailyOffset ()
  {
    return dailyOffset;
  }

  int getWeeklyOffset ()
  {
    return weeklyOffset;
  }

  // configurable parameters, fluent setters
  public double getCoolThreshold ()
  {
    return coolThreshold;
  }

  @ConfigurableValue(valueType = "Double",
      description = "temperature threshold for cooling")
  public MisoBuyer withCoolThreshold (double value)
  {
    coolThreshold = value;
    return this;
  }

  public double getCoolCoef ()
  {
    return coolCoef;
  }

  @ConfigurableValue(valueType = "Double",
      description = "Multiplier: cooling MWh / degree-hour")
  public MisoBuyer withCoolCoef (double value)
  {
    coolCoef = value;
    return this;
  }

  public double getHeatThreshold ()
  {
    return heatThreshold;
  }

  @ConfigurableValue(valueType = "Double",
      description = "temperature threshold for heating")
  public MisoBuyer withHeatThreshold (double value)
  {
    heatThreshold = value;
    return this;
  }

  public double getHeatCoef ()
  {
    return heatCoef;
  }

  @ConfigurableValue(valueType = "Double",
      description = "multiplier: heating MWh / degree-hour (negative for heating)")
  public MisoBuyer withHeatCoef (double value)
  {
    heatCoef = value;
    return this;
  }

  public double getTempAlpha ()
  {
    return tempAlpha;
  }

  @ConfigurableValue(valueType = "Double",
      description = "exponential smoothing parameter for temperature")
  public MisoBuyer withTempAlpha (double value)
  {
    tempAlpha = value;
    return this;
  }

  public double getScaleFactor ()
  {
    return scaleFactor;
  }

  @ConfigurableValue(valueType = "Double",
      description = "overall scale factor for demand profile")
  public MisoBuyer withScaleFactor (double value)
  {
    scaleFactor = value;
    return this;
  }

  ComposedTS getTimeseries ()
  {
    return timeseries;
  }

  // timeseries implementation
  class ComposedTS
  {
    private double lastWalk = 0.0;
    private double lastSmooth = 0.0;
    private double ring[] = null;
    private int lastTsGenerated = -1;


    ComposedTS ()
    {
      super();
    }

    // must be called with the first timeslot index to see the timeseries
    void initialize (int ts)
    {
      // set up the ring buffer
      ring = new double[timeslotsOpen];

      // set up the white noise generator
      lastWalk = tsSeed.nextGaussian() * walkSd;
      lastSmooth = lastWalk;
    }

    double generateValue (int ts)
    {
      // retrieve daily value, add daily noise
      double dv = getDailyValue(ts) + tsSeed.nextGaussian() * dailySd;
      // retrieve weekly value, add weekly noise
      double wv = getWeeklyValue(ts) + tsSeed.nextGaussian() * weeklySd;
      // run a step of the random walk, return sum
      lastWalk = (1.0 - walkz) * lastWalk + tsSeed.nextGaussian() * walkSd;
      lastSmooth = walkAlpha * lastWalk + (1.0 - walkAlpha) * lastSmooth;
      double result = mean + dv + wv + lastSmooth;
      log.debug("Demand ts {}: {}", ts, result);
      return result;
    }

    // returns the demand value for timeslot, which must be adjusted to
    // be zero at the start of the series.
    double getValue (int timeslot)
    {
      if (null == ring) // uninitialized
        log.error("Uninitialized");
      while (timeslot > lastTsGenerated) {
        lastTsGenerated += 1;
        ring[lastTsGenerated % ring.length] = generateValue(lastTsGenerated);
      }
      return (ring[timeslot % ring.length]);
    }
  }
}
