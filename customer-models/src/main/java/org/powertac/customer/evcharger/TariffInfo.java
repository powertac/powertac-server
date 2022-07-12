package org.powertac.customer.evcharger;

import java.util.Arrays;

import org.joda.time.Instant;
import org.powertac.common.CapacityProfile;
import org.powertac.common.Competition;
import org.powertac.common.Tariff;
import org.powertac.common.TimeService;

class TariffInfo
  {
    /**
     * 
     */
    private final EvCharger evCharger;
    private Tariff tariff;
    private int profileSize;
    private CapacityProfile capacityProfile;

    // regulation premium is an array, the length of the profile
    private double[] upRegulationPremium;
    private double[] downRegulationPremium;
    private double[] tariffCost;
    private double meanTariffCost;
    
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

    // For TOU tariffs, we need to simulate operation using the
    // "typical" demandInfo sequence available in demandInfoMean    
    void generateTouProfile ()
    {
      // we first need to understand the value of flexibility
      computeRegulationPremium();
      Instant evalTime = evCharger.lastSunday();
      tariffCost = new double[profileSize];
      long increment = Competition.currentCompetition().getTimeslotDuration(); // millis
      for (int index = 0; index < profileSize; index++) {
        tariffCost[index] = getTariff().getUsageCharge(evalTime, 1.0, 0.0); // neg value
        evalTime = evalTime.plus(increment);
      }
      // next we set up a StorageState and run the typical DemandInfo
      // sequence against it, adjusting demand within the min-max range
      // to take advantage of consumption and flexibility prices. 
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

    // For a TOU
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

    // tariff.getRegulationCharge(kwh, 0, false); neg for up-reg. pos for down-reg
    // tariff.getUsageCharge(when, kwh, 0.0, false, TEH); 

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