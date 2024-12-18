/*
* Copyright 2011-2013 the original author or authors.
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
import java.time.ZonedDateTime;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.factoredcustomer.interfaces.StructureInstance;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Utility class that generates various time series patterns that can be
 * used as base capacity series by implementations of @code{CapacityOriginator}.
 *
 * @author Prashant Reddy, Govert Buijs
 */
public final class TimeseriesGenerator implements StructureInstance
{
  private static Logger log = LogManager.getLogger(TimeseriesGenerator.class);

  private TimeslotRepo timeslotRepo;

  private final int FORECAST_HORIZON = 2 * 24; // two days

  private String name;

  // These will come from the properties file
  @ConfigurableValue(valueType = "Double", dump = false)
  private double y0;
  @ConfigurableValue(valueType = "List", dump = false)
  private List<String> yh;
  @ConfigurableValue(valueType = "List", dump = false)
  private List<String> yd;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double phi1;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double Phi1;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double theta1;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double Theta1;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double sigma;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double lambda;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double gamma;
  @ConfigurableValue(valueType = "List", dump = false)
  private List<String> refSeries;

  private final Map<Integer, Double> genSeries = new HashMap<>();

  private Random arimaNoise;

  public TimeseriesGenerator (String name)
  {
    this.name = name;
  }

  public void initialize (FactoredCustomerService service)
  {
    timeslotRepo = service.getTimeslotRepo();

    arimaNoise = new Random(service.getRandomSeedRepo()
        .getRandomSeed("factoredcustomer.TimeseriesGenerator",
            SeedIdGenerator.getId(), "ArimaNoise").getValue());
  }

  @Override
  public String getName ()
  {
    return name;
  }

  public double generateNext (int timeslot)
  {
    if (genSeries.isEmpty()) {
      initArima101x101GenSeries(timeslot);
    }
    Double next = genSeries.get(timeslot);
    if (next == null) {
      next = generateNextArima101x101(timeslot);
      genSeries.put(timeslot, next);
    }
    return next;
  }

  private void initArima101x101GenSeries (int timeslot)
  {
    for (int i = 0; i < refSeries.size(); ++i) {
      genSeries.put(timeslot + i, Double.parseDouble(refSeries.get(i)));
    }
  }

  private double generateNextArima101x101 (int timeslot)
  {
    /** R code
     boostTimeSeries = function(Xt, lambda, t, N, Xht, Xdt, gamma) {
     return (Xt + (lambda * ((log(t-26))^2/(log(N-26))^2) * ((1 - gamma) * Xht + gamma * Xdt)))
     }
     for (t in compRange) {
     Zf[t] = Y0 + Yd[D[t]] + Yh[H[t]] + phi1 * Zf[t-1] + Phi1 * Zf[t-24] #+ rnorm(1, 0, sigma^2) +
     theta1 * (Zf[t-1] - Zf[t-2]) + Theta1 * (Zf[t-24] - Zf[t-25]) +
     theta1 * Theta1 * (Zf[t-25] - Zf[t-26])
     Zbf[t] = boostTimeSeries(Zf[t], lambda, t, N, Yh[H[t]], Yd[D[t]], gamma) #+ rnorm(1, 0, sigma^2)
     }
     **/

    ZonedDateTime now = timeslotRepo.getDateTimeForIndex(timeslot);
    int day = now.getDayOfWeek().getValue();   // 1=Monday, 7=Sunday
    int hour = now.getHour();  // 0-23

    double yh_hour = Double.parseDouble(yh.get(hour));
    double yd_day = Double.parseDouble(yd.get(day - 1));

    double logNext = y0 + yd_day + yh_hour
        + phi1 * getLog(timeslot - 1) + Phi1 * getLog(timeslot - 24)
        + theta1 * (getLog(timeslot - 1) - getLog(timeslot - 2))
        + Theta1 * (getLog(timeslot - 24) - getLog(timeslot - 25))
        + theta1 * Theta1 * (getLog(timeslot - 25) - getLog(timeslot - 26));
    double nom = Math.pow(Math.log(timeslot - 26), 2);
    double denom = Math.pow(Math.log(FORECAST_HORIZON - 26), 2);
    double fact = ((1 - gamma) * yh_hour + gamma * yd_day);
    logNext += lambda * (nom / denom) * fact;
    logNext += Math.pow(sigma, 2) * arimaNoise.nextGaussian();
    double next = Math.exp(logNext);
    if (Double.isNaN(next)) {
      throw new Error("Generated NaN as next time series element!");
    }
    return next;
  }

  private double getLog (int timeslot)
  {
    Double val = genSeries.get(timeslot);
    if (null == val) {
      log.error("Null value in genSeries for ts " + timeslot);
      return 1.0;
    }
    return Math.log(val);
  }
}
