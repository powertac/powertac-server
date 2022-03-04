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
class StorageCapacityOriginator extends DefaultCapacityOriginator
{
  protected static Logger log = LogManager.getLogger(DefaultCapacityOriginator.class);

  public StorageCapacityOriginator (FactoredCustomerService service,
                                    CapacityStructure capacityStructure,
                                    CapacityBundle bundle)
  {
    super(service, capacityStructure, bundle);
  }

  /**
   * Here we deal with the charge envelope for storage types
   */
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
    if (parentBundle.getPowerType().isInterruptible()
            || parentBundle.getPowerType().isStorage()) {
      // compute regulation capacity before handling regulation shifts
      // TODO - use current SoC values rather than static values from the capacityStructure
      double soc = (Double) subscription.getCustomerDecorator(CapacityStructure.getStateOfChargeLabel());
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
}



