package org.powertac.customer.evcharger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.joda.time.Instant;
import org.powertac.common.CapacityProfile;
import org.powertac.common.Competition;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;

class TariffInfo
{
  private final EvCharger evCharger;
  private Tariff tariff;
  private int profileSize;
  private CapacityProfile capacityProfile;

  // tariff cost values are negative
  private double[] tariffCost;
  private double meanTariffCost;
  private double maxTariffCost = 0.0;
  private double minTariffCost = -Double.MAX_VALUE;

  // regulation premium is an array, the length of the profile
  // values are positive if regulation is advantageous
  private double[] upRegulationPremium;
  private double[] downRegulationPremium;

  // regulation bias adjust consumption in case regulation
  // has value
  private double regulationBias = 0.1; // 10%

  TariffInfo (EvCharger evCharger, Tariff tariff)
  {
    super();
    this.evCharger = evCharger;
    this.tariff = tariff;
    if (tariff.isWeekly()) {
      // use a weekly profile
      profileSize = 
              (int) (TimeService.WEEK
                      / Competition.currentCompetition().getTimeslotDuration()); 
    }
    else {
      profileSize = 
              (int) (TimeService.DAY
                      / Competition.currentCompetition().getTimeslotDuration()); 
    }
    upRegulationPremium = new double[profileSize];
    downRegulationPremium = new double[profileSize];
  }

  Tariff getTariff ()
  {
    return tariff;
  }

  void setCapacityProfile (CapacityProfile profile)
  {
    capacityProfile = profile;      
  }

  CapacityProfile getCapacityProfile ()
  {
    if (null == capacityProfile) {
      // need to create the profile
      if ((!isTOU()) && (!isVariableRate())) {
        // Simplest case
        meanTariffCost = tariff.getUsageCharge(1.0, 0.0, false);
        minTariffCost = meanTariffCost;
        maxTariffCost = meanTariffCost;
        setCapacityProfile(evCharger.getDefaultCapacityProfile());
      }
      else if (isTOU()) {
        generateTouProfile();          
      }
      else {
        // TODO - we should set up a TariffEvaluationHelper to deal
        // with variable-rate tariffs. For now we'll just use
        // the default profile
        setCapacityProfile(evCharger.getDefaultCapacityProfile());
      }
    }
    return capacityProfile;
  }

  // For a TOU tariff, we have to create a StorageState
  // and run a full day of demand, then run the
  // prototype DemandInfo sequence and collect
  // data until our DemandProfile is complete.
  void generateTouProfile ()
  {
    // we first need to understand the value of flexibility
    computeRegulationPremium();
    Instant lastSunday = evCharger.lastSunday();
    Instant evalTime = lastSunday;
    tariffCost = new double[profileSize];
    double costSum = 0.0;
    long increment = Competition.currentCompetition().getTimeslotDuration(); // millis
    // collect per-kWh cost for each ts in the profile
    for (int index = 0; index < profileSize; index++) {
      tariffCost[index] = getTariff().getUsageCharge(evalTime, 1.0, 0.0); // neg value
      maxTariffCost = Math.min(maxTariffCost, tariffCost[index]);
      minTariffCost = Math.max(minTariffCost, tariffCost[index]);
      costSum += tariffCost[index];
      evalTime = evalTime.plus(increment);
    }
    meanTariffCost = costSum / profileSize;

    // next we set up a StorageState and run the typical DemandInfo
    // sequence against it, adjusting demand within the min-max range
    // to take advantage of consumption and flexibility prices.
    StorageState ss = new StorageState(null, evCharger.getChargerCapacity(),
                                       evCharger.getMaxDemandHorizon())
            .withUnitCapacity(evCharger.getChargerCapacity());
    List<ArrayList<DemandElement>> demandInfo = evCharger.getDemandInfoMean();
    evalTime = lastSunday;
    // we first run a full day to seed the SS
    for (int hour = 0; hour < demandInfo.size(); hour++) {
      List<DemandElement> demand = demandInfo.get(hour);
      ss.distributeDemand(hour, demand, 1.0);
      double[] limits = ss.getMinMax(hour);
      // determine usage, but don't record
      double usage = determineUsage(hour, limits);
      ss.distributeUsage(hour, usage);
      // normally these steps happen after distributing regulation
      ss.collapseElements(hour + 1);
      ss.rebalance(hour + 1);
      evalTime = evalTime.plus(increment);        
    }
    // now do the same thing to fill up the profile array
    // handle the case where the profile array is larger than the demandInfo map
    int repeat = 1;
    if (profileSize > demandInfo.size())
      repeat = (int) Math.ceil(((double) profileSize)
                               / ((double) demandInfo.size()));
    double[] profileData = new double[demandInfo.size() * repeat];
    evalTime = lastSunday;
    for (int i = 0; i < repeat; i++) {
      // add a demandInfo.size block to the profile
      for (int hour = 0; hour < demandInfo.size(); hour++) {
        List<DemandElement> demand = demandInfo.get(hour);
        ss.distributeDemand(hour, demand, 1.0);
        double[] limits = ss.getMinMax(hour);
        double usage = determineUsage(hour, limits);
        // record usage
        profileData[i * demandInfo.size() + hour] = usage;
        ss.distributeUsage(hour, usage);
        // clean up for next timeslot
        ss.collapseElements(hour + 1);
        ss.rebalance(hour + 1);
        evalTime = evalTime.plus(increment);
      }
    }
    capacityProfile = new CapacityProfile(profileData, lastSunday);
  }

  // Given [min, max, mid] limits, determine preferred usage
  double determineUsage (int hour, double[] limits)
  {
    if (!isTOU())
      // flat-rate tariff
      return limits[2];
    // otherwise, determine usage based on tariff and reg prices
    int index = hour % profileSize;
    double result = limits[2];
    if (tariffCost[index] >= meanTariffCost) {
      // tariff cost values are negative
      // in this case, we have cheap energy, use more
      result = limits[1];
      if (downRegulationPremium[hour] > 0.0) {
        result -= (limits[2] - limits[0]) * regulationBias; 
      }
    }
    else {
      // expensive energy
      result = limits[0];
      if (upRegulationPremium[hour] > 0.0) {
        result += (limits[1] - limits[2]) * regulationBias; 
      }
    }
    return result;
  }

  // true just in case the tariff is a TOU tariff. If it's not, then
  // the price array and capacityProfile will be empty (null).
  boolean isTOU ()
  {
    return tariff.isTimeOfUse();
  }

  boolean isVariableRate ()
  {
    return tariff.isVariableRate();
  }

  // Heuristic decision on demand bias, depending on current vs near-future prices
  // and on value of demand flexibility
  double getDemandBias ()
  {
    double result = evCharger.getNominalDemandBias();
    // do something here
    return result;
  }

  void computeRegulationPremium ()
  {
    if (tariff.hasRegulationRate()) {
      // We expect the upregPayment to be positive (customer gets paid).
      double upregPayment = getTariff().getRegulationCharge(-1.0, 0.0, false); // up-reg +
      double downregCost = getTariff().getRegulationCharge(1.0,  0.0, false); // down-reg -
      Instant evalTime = evCharger.lastSunday();
      long increment = Competition.currentCompetition().getTimeslotDuration(); // millis
      for (int index = 0; index < profileSize; index++) {
        double cost = getTariff().getUsageCharge(evalTime, 1.0, 0.0); // neg value
        // Premiums < 0.0 mean we lose money on regulation
        upRegulationPremium[index] = upregPayment + cost;
        downRegulationPremium[index] = downregCost - cost;
        evalTime = evalTime.plus(increment);
      }
    }
    else {
      // fill with zeros
      Arrays.fill(upRegulationPremium, 0.0);
      Arrays.fill(downRegulationPremium, 0.0);
    }
  }

  // Creates and returns a tariff-specific capacity profile
  // to support tariff eval.
  // For now, we want a 24h profile. To avoid startup effects, we will
  // create a 48h profile and just use the last 24h.
  // This requires
  // - setting up a StorageState
  // - processing the median DemandElements
  // - using the normal heuristics to adjust demand to prices,
  //   while maintaining flexibility in case the tariff has
  //   a regulation rate

  // expected cost 00:00 Monday through 23:00 Sunday
  //    double[] getCost ()
  //    {
  //      if (null != this.costs)
  //        return costs;
  //      costs = new double[profileSize];
  //      double cumulativeUsage = 0.0;
  //      Instant start =
  //          service.getTimeslotRepo().currentTimeslot().getStartInstant();
  //      for (int i = 0; i < profileSize; i++) {
  //        Instant when = start.plus(i * TimeService.HOUR);
  //        if (when.get(DateTimeFieldType.hourOfDay()) == 0) {
  //          cumulativeUsage = 0.0;
  //        }
  //        costs[i] =
  //            tariff.getUsageCharge(when, cumulativeUsage) / nhc;
  //        cumulativeUsage += nhc;
  //      }
  //      return costs;
  //    }
  }