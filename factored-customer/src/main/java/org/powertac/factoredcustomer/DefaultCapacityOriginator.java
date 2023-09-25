/*
* Copyright 2011-2018 the original author or authors.
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
import org.joda.time.DateTime;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherForecastRepo;
import org.powertac.common.repo.WeatherReportRepo;
//import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.CapacityStructure.BaseCapacityType;
import org.powertac.factoredcustomer.CapacityStructure.InfluenceKind;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;
import org.powertac.factoredcustomer.interfaces.CapacityOriginator;
import org.powertac.factoredcustomer.interfaces.StructureInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Key class responsible for drawing from a base capacity and adjusting that
 * capacity in response to various static and dynamic factors for each timeslot.
 *
 * @author Prashant Reddy, John Collins
 */
//@Domain
class DefaultCapacityOriginator implements CapacityOriginator
{
  private static Logger log = LogManager.getLogger(DefaultCapacityOriginator.class);

  private TimeService timeService;
  private TimeslotRepo timeslotRepo;
  private WeatherReportRepo weatherReportRepo;
  private WeatherForecastRepo weatherForecastRepo;

  private final double SMOOTHING_WEIGHT = 0.4; // 0.0 => ignore previous value

  private final TimeseriesGenerator tsGenerator;

  private final CapacityStructure capacityStructure;
  private final CapacityBundle parentBundle;

  protected final String logIdentifier;
  protected final Map<Integer, Double> baseCapacities = new HashMap<>();
  protected final Map<Integer, Double> forecastCapacities = new HashMap<>();
  protected final Map<Integer, Double> actualCapacities = new HashMap<>();
  protected final Map<Integer, Double> curtailedCapacities = new HashMap<>();
  protected final Map<Integer, Double> shiftedCurtailments = new HashMap<>();
  protected RegulationCapacity currentRegCapacity = null;

  public DefaultCapacityOriginator (FactoredCustomerService service,
                                    CapacityStructure capacityStructure,
                                    CapacityBundle bundle)
  {
    this.timeService = service.getTimeService();
    this.timeslotRepo = service.getTimeslotRepo();
    this.weatherReportRepo = service.getWeatherReportRepo();
    this.weatherForecastRepo = service.getWeatherForecastRepo();
    this.capacityStructure = capacityStructure;
    this.parentBundle = bundle;

    logIdentifier = this.capacityStructure.getName().isEmpty()
        ? bundle.getName()
        : bundle.getName() + "#" + this.capacityStructure.getName();

    if (capacityStructure.getBaseCapacityType() == BaseCapacityType.TIMESERIES) {
      Map<String, StructureInstance> map =
          Config.getInstance().getStructures().get("TimeseriesGenerator");
      tsGenerator = (TimeseriesGenerator)
          map.get(capacityStructure.getName() + "Population");
      if (tsGenerator != null) {
        tsGenerator.initialize(service);
      }
    }
    else {
      tsGenerator = null;
    }
  }

  @Override
  public CapacityProfile getCurrentForecast ()
  {
    int timeslot = timeslotRepo.currentSerialNumber();
    return getForecastForTimeslot(timeslot);
  }

  @Override
  public CapacityProfile getForecastForNextTimeslot ()
  {
    int timeslot = timeslotRepo.currentSerialNumber();
    return getForecastForTimeslot(timeslot + 1);
  }

  private CapacityProfile getForecastForTimeslot (int timeslot)
  {
    List<Double> values = new ArrayList<>();
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      Double forecastCapacity = forecastCapacities.get(timeslot);
      if (forecastCapacity != null) {
        values.add(forecastCapacity);
      }
      else {
        values.add(getForecastCapacity(timeslot));
      }
      timeslot += 1;
    }
    return new CapacityProfile(values);
  }

  @Override
  public CapacityProfile getCurrentForecastPerSub (TariffSubscription sub)
  {
    // DefaultCapacityOriginator doesn't track subscriptions, so:
    return getCurrentForecast();
  }

  @Override
  public CapacityProfile getForecastPerSubStartingAt (
      int startingTimeslot, TariffSubscription subscription)
  {
    return getForecastForTimeslot(startingTimeslot);
  }

  protected double getForecastCapacity (int timeslot)
  {
    Double ret = forecastCapacities.get(timeslot);
    if (ret == null) {
      ret = computeForecastCapacity(timeslot);
    }
    return ret;
  }

  private double computeForecastCapacity (int future)
  {
    int now = timeslotRepo.currentSerialNumber();
    int timeToFuture = future - now;
    Weather weather = null;
    if (timeToFuture == 0) {
      weather =
          new Weather(weatherReportRepo.currentWeatherReport());
    }
    else {
      WeatherForecast forecast = weatherForecastRepo.currentWeatherForecast();
      List<WeatherForecastPrediction> predictions = forecast.getPredictions();
      for (WeatherForecastPrediction prediction : predictions) {
        if (prediction.getForecastTime() == timeToFuture) {
          weather = new Weather(prediction);
        }
      }
    }
    if (weather == null) {
      throw new Error("Could not find weather forecast for timeslot " + future);
    }

    double baseCapacity = getBaseCapacity(future);
    if (Double.isNaN(baseCapacity)) {
      throw new Error("Base capacity is NaN!");
    }

    // Compute for full population ignoring current tariff rates
    double forecastCapacity = baseCapacity;
    forecastCapacity =
        adjustCapacityForPeriodicSkew(forecastCapacity,
            timeslotRepo.getDateTimeForIndex(future),
            false);
    forecastCapacity =
        adjustCapacityForWeather(forecastCapacity, weather, false);
    if (Double.isNaN(forecastCapacity)) {
      throw new Error("Adjusted capacity is NaN for base capacity = "
          + baseCapacity);
    }

    forecastCapacity = truncateTo2Decimals(forecastCapacity);
    forecastCapacities.put(future, forecastCapacity);
    log.debug(logIdentifier + ": Daniel Forecast capacity for timeslot " + future
        + " = " + forecastCapacity);
    return forecastCapacity;
  }

  private double getBaseCapacity (int future)
  {
    Double ret = baseCapacities.get(future);
    if (ret == null) {
      ret = drawBaseCapacitySample(future);
    }
    return ret;
  }

  private double drawBaseCapacitySample (int timeslot)
  {
    double baseCapacity = 0.0;
    switch (capacityStructure.getBaseCapacityType()) {
      case POPULATION:
        baseCapacity = capacityStructure.getBasePopulationCapacity().drawSample();
        break;
      case INDIVIDUAL:
        // #956
        //for (int i = 0; i < parentBundle.getPopulation(); ++i) {
          baseCapacity +=
              capacityStructure.getBaseIndividualCapacity().drawSample();
        //}
        break;
      case TIMESERIES:
        baseCapacity = getBaseCapacityFromTimeseries(timeslot);
        break;
      default:
        throw new Error(logIdentifier + ": Unexpected base capacity type: "
            + capacityStructure.getBaseCapacityType());
    }

    Double prevCapacity = baseCapacities.get(timeslot - 1);
    if (prevCapacity != null) {
      baseCapacity =
          SMOOTHING_WEIGHT * prevCapacity + (1 - SMOOTHING_WEIGHT) * baseCapacity;
    }
    baseCapacity = truncateTo2Decimals(baseCapacity);
    baseCapacities.put(timeslot, baseCapacity);
    return baseCapacity;
  }

  private double getBaseCapacityFromTimeseries (int timeslot)
  {
    try {
      return tsGenerator.generateNext(timeslot);
    }
    catch (ArrayIndexOutOfBoundsException e) {
      log.error(logIdentifier
          + ": Tried to get base capacity from time series at index beyond maximum!");
      throw e;
    }
  }

  @Override
  public double getShiftingInconvenienceFactor (Tariff tariff)
  {
    return 0; // not shifting should take place in DefaultCapacityOriginator
  }

  @Override
  public CapacityAccumulator useCapacity (TariffSubscription subscription)
  {
    int timeslot = timeslotRepo.currentSerialNumber();

    double baseCapacity = getBaseCapacity(timeslot);
    if (Double.isNaN(baseCapacity)) {
      throw new Error("Base capacity is NaN!");
    }
    //else if (parentBundle.getPowerType().isProduction()) {
    //  // correct sign before going further
    //  baseCapacity *= -1.0;
    //}
    logCapacityDetails(logIdentifier + ": Base capacity for timeslot "
        + timeslot + " = " + baseCapacity);

    // total adjusted capacity
    double adjustedCapacity = baseCapacity;
    adjustedCapacity =
        adjustCapacityForPeriodicSkew(adjustedCapacity,
            timeService.getCurrentDateTime(), true);
    adjustedCapacity = adjustCapacityForCurrentWeather(adjustedCapacity, true);

    // adjust for subscribed population
    if (!parentBundle.isAllIndividual()) {
      adjustedCapacity =
              adjustCapacityForSubscription(timeslot, adjustedCapacity, subscription);
    }

    CapacityAccumulator result =
        addRegCapacityMaybe(subscription, timeslot, adjustedCapacity);

    actualCapacities.put(timeslot, result.getCapacity());
    log.info(logIdentifier + ": Adjusted capacity for tariff "
        + subscription.getTariff().getId() + " = " + result.getCapacity());
    return result;
  }

  protected CapacityAccumulator
  addRegCapacityMaybe (TariffSubscription subscription,
                       int timeslot,
                       double adjustedCapacity)
  {
    // Deal with regulation
    double upReg = 0.0;
    double downReg = 0.0;
    if (parentBundle.getPowerType().isInterruptible()) {
      // compute regulation capacity before handling regulation shifts
      upReg = Math.max(0.0, (adjustedCapacity -
          capacityStructure.getUpRegulationLimit()));
      downReg = Math.min(0.0, (adjustedCapacity -
          capacityStructure.getDownRegulationLimit()));
      log.info("{} regulation = {}:{}", parentBundle.getName(), upReg, downReg);
      adjustedCapacity =
          adjustCapacityForCurtailments(timeslot, adjustedCapacity, subscription);
    }
    return new CapacityAccumulator(truncateTo2Decimals(adjustedCapacity),
                                   upReg, downReg);
  }

  private double adjustCapacityForCurtailments (int timeslot, double capacity,
                                                TariffSubscription subscription)
  {
    double lastCurtailment = subscription.getCurtailment();
    if (Math.abs(lastCurtailment) > 0.01) { // != 0
      curtailedCapacities.put(timeslot - 1, lastCurtailment);
      List<String> shifts = capacityStructure.getCurtailmentShifts();
      for (int i = 0; i < shifts.size(); ++i) {
        double shiftingFactor = Double.parseDouble(shifts.get(i));
        double shiftedCapacity = lastCurtailment * shiftingFactor;
        Double previousShifts = shiftedCurtailments.get(timeslot + i);
        shiftedCapacity += (previousShifts != null) ? previousShifts : 0.0;
        shiftedCurtailments.put(timeslot + i, shiftedCapacity);
      }
    }
    Double currentShift = shiftedCurtailments.get(timeslot);
    return (currentShift == null) ? capacity : capacity + currentShift;
  }

  private double adjustCapacityForPeriodicSkew (double capacity, DateTime when,
                                                boolean verbose)
  {
    int day = when.getDayOfWeek(); // 1=Monday, 7=Sunday
    int hour = when.getHourOfDay(); // 0-23

    double periodicSkew = capacityStructure.getPeriodicSkew(day, hour);
    if (verbose) {
      logCapacityDetails(logIdentifier + ": periodic skew = " + periodicSkew);
    }
    return capacity * periodicSkew;
  }

  private double adjustCapacityForCurrentWeather (double capacity,
                                                  boolean verbose)
  {
    WeatherReport weatherReport = weatherReportRepo.currentWeatherReport();
    return adjustCapacityForWeather(capacity, new Weather(weatherReport),
        verbose);
  }

  private double adjustCapacityForWeather (double capacity, Weather weather,
                                           boolean verbose)
  {
    if (verbose) {
      logCapacityDetails(logIdentifier + ": weather = ("
          + weather.getTemperature() + ", "
          + weather.getWindSpeed() + ", "
          + weather.getWindDirection() + ", "
          + weather.getCloudCover() + ")");
    }

    double weatherFactor = 1.0;
    if (capacityStructure.getTemperatureInfluence() == InfluenceKind.DIRECT) {
      int temperature = (int) Math.round(weather.getTemperature());
      weatherFactor =
          weatherFactor * capacityStructure.getTemperatureFactor(temperature);
    }
    else if (capacityStructure.getTemperatureInfluence() == InfluenceKind.DEVIATION) {
      int curr = (int) Math.round(weather.getTemperature());
      int ref = (int) Math.round(capacityStructure.getTemperatureReference());
      double deviationFactor = 1.0;
      if (curr > ref) {
        for (int t = ref + 1; t <= curr; ++t) {
          deviationFactor += capacityStructure.getTemperatureFactor(t);
        }
      }
      else if (curr < ref) {
        for (int t = curr; t < ref; ++t) {
          deviationFactor += capacityStructure.getTemperatureFactor(t);
        }
      }
      weatherFactor = weatherFactor * deviationFactor;
    }
    if (capacityStructure.getWindSpeedInfluence() == InfluenceKind.DIRECT) {
      int windSpeed = (int) Math.round(weather.getWindSpeed());
      weatherFactor = weatherFactor *
                      capacityStructure.getWindspeedFactor(windSpeed);
      if (windSpeed > 0.0
          && capacityStructure.getWindDirectionInfluence() == InfluenceKind.DIRECT) {
        int windDirection = (int) Math.round(weather.getWindDirection());
        weatherFactor = weatherFactor *
                        capacityStructure.getWindDirectionFactor(windDirection);
      }
    }
    if (capacityStructure.getCloudCoverInfluence() == InfluenceKind.DIRECT) {
      int cloudCover = (int) Math.round(100 * weather.getCloudCover()); // [0,1]
      // to
      // ##%
      weatherFactor =
          weatherFactor * capacityStructure.getCloudCoverFactor(cloudCover);
    }
    if (verbose) {
      logCapacityDetails(logIdentifier + ": weather factor = " + weatherFactor);
    }
    return capacity * weatherFactor;
  }

  @Override
  public double adjustCapacityForSubscription (int timeslot,
                                               double totalCapacity,
                                               TariffSubscription subscription)
  {
    double subCapacity =
        adjustCapacityForPopulationRatio(totalCapacity, subscription);
    return adjustCapacityForTariffRates(timeslot, subCapacity, subscription);
  }

  private double adjustCapacityForPopulationRatio (
      double capacity, TariffSubscription subscription)
  {
    double popRatio =
        (double) subscription.getCustomersCommitted() / (double) parentBundle.getPopulation();
    logCapacityDetails(logIdentifier + ": population ratio = " + popRatio);
    return capacity * popRatio;
  }

  // TODO -- seems gratuitous
  //private double getPopulationRatio (int customerCount, int population)
  //{
  //  return ((double) customerCount) / ((double) population);
  //}

  private double adjustCapacityForTariffRates (
      int timeslot, double baseCapacity, TariffSubscription subscription)
  {
    if ((baseCapacity - 0.0) < 0.01) {
      return baseCapacity;
    }

    double chargeForBase =
        subscription.getTariff().getUsageCharge(
            timeslotRepo.getTimeForIndex(timeslot),
            baseCapacity); //,
//            subscription.getTotalUsage());
    double rateForBase = chargeForBase / baseCapacity;

    double benchmarkRate =
        capacityStructure.getBenchmarkRate(timeService.getHourOfDay());
    double rateRatio = rateForBase / benchmarkRate;

    double tariffRatesFactor = determineTariffRatesFactor(rateRatio);
    logCapacityDetails(logIdentifier + ": tariff rates factor = "
        + tariffRatesFactor);
    return baseCapacity * tariffRatesFactor;
  }

  private double determineTariffRatesFactor (double rateRatio)
  {
    switch (capacityStructure.getElasticityModelType()) {
      case CONTINUOUS:
        return capacityStructure.determineContinuousElasticityFactor(rateRatio);
      case STEPWISE:
        return determineStepwiseElasticityFactor(rateRatio);
      default:
        throw new Error("Unexpected elasticity model type: "
            + capacityStructure.getElasticityModelType());
    }
  }

  private double determineStepwiseElasticityFactor (double rateRatio)
  {
    double[][] elasticity = capacityStructure.getElasticity();
    if (Math.abs(rateRatio - 1) < 0.01 || elasticity.length == 0) {
      return 1.0;
    }
    PowerType powerType = parentBundle.getPowerType();
    if (powerType.isConsumption() && rateRatio < 1.0) {
      return 1.0;
    }
    if (powerType.isProduction() && rateRatio > 1.0) {
      return 1.0;
    }

    final int RATE_RATIO_INDEX = 0;
    final int CAPACITY_FACTOR_INDEX = 1;
    double rateLowerBound = Double.NEGATIVE_INFINITY;
    double rateUpperBound = Double.POSITIVE_INFINITY;
    double lowerBoundCapacityFactor = 1.0;
    double upperBoundCapacityFactor = 1.0;
    for (double[] anElasticity : elasticity) {
      double r = anElasticity[RATE_RATIO_INDEX];
      if (r <= rateRatio && r > rateLowerBound) {
        rateLowerBound = r;
        lowerBoundCapacityFactor = anElasticity[CAPACITY_FACTOR_INDEX];
      }
      if (r >= rateRatio && r < rateUpperBound) {
        rateUpperBound = r;
        upperBoundCapacityFactor = anElasticity[CAPACITY_FACTOR_INDEX];
      }
    }
    return (rateRatio < 1) ? upperBoundCapacityFactor : lowerBoundCapacityFactor;
  }

  @Override
  public String getCapacityName ()
  {
    return capacityStructure.getName();
  }

  @Override
  public CapacityBundle getParentBundle ()
  {
    return parentBundle;
  }

  protected double truncateTo2Decimals (double x)
  {
    double fract, whole;
    if (x > 0) {
      whole = Math.floor(x);
      fract = Math.floor((x - whole) * 100) / 100;
    }
    else {
      whole = Math.ceil(x);
      fract = Math.ceil((x - whole) * 100) / 100;
    }
    return whole + fract;
  }

  private void logCapacityDetails (String msg)
  {
    if (Config.getInstance().isCapacityDetailsLogging()) {
      log.info(msg);
    }
  }

  @Override
  public boolean isIndividual ()
  {
    return (capacityStructure.isIndividual());
  }

  @Override
  public String toString ()
  {
    return this.getClass().getCanonicalName() + ":" + logIdentifier;
  }

  // Convenience class to unify the interface to
  // WeatherReport and WeatherForecastPrediction.
  private class Weather
  {
    final double temperature;
    final double windSpeed;
    final double windDirection;
    final double cloudCover;

    Weather (WeatherReport report)
    {
      temperature = report.getTemperature();
      windSpeed = report.getWindSpeed();
      windDirection = report.getWindDirection();
      cloudCover = report.getCloudCover();
    }

    Weather (WeatherForecastPrediction prediction)
    {
      temperature = prediction.getTemperature();
      windSpeed = prediction.getWindSpeed();
      windDirection = prediction.getWindDirection();
      cloudCover = prediction.getCloudCover();
    }

    double getTemperature ()
    {
      return temperature;
    }

    double getWindSpeed ()
    {
      return windSpeed;
    }

    double getWindDirection ()
    {
      return windDirection;
    }

    double getCloudCover ()
    {
      return cloudCover;
    }
  }
}



