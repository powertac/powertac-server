/*
* Copyright 2011-2016 the original author or authors.
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
import org.powertac.common.TariffSubscription;
//import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.CapacityProfile.PermutationRule;
import org.powertac.factoredcustomer.ProfileRecommendation.Opinion;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;
import org.powertac.factoredcustomer.interfaces.CapacityOriginator;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.commons.math3.stat.descriptive.moment.Variance;
//import org.powertac.common.RandomSeed;

/**
 * Key class responsible for managing the tariff(s) for one customer across
 * multiple capacity bundles if necessary.
 *
 * @author Prashant Reddy
 */
//@Domain
class LearningUtilityOptimizer extends DefaultUtilityOptimizer
{
  private static Logger log = LogManager.getLogger(LearningUtilityOptimizer.class);

  public LearningUtilityOptimizer (CustomerStructure customerStructure,
                                   List<CapacityBundle> bundles)
  {
    super(customerStructure, bundles);
  }

  @Override
  public void initialize (FactoredCustomerService service)
  {
    super.initialize(service);
    //this.service = service;
    inertiaSampler =
        getRandomSeedRepo()
            .getRandomSeed("factoredcustomer.LearningUtilityOptimizer",
                SeedIdGenerator.getId(), "InertiaSampler");
    tariffSelector =
        getRandomSeedRepo()
            .getRandomSeed("factoredcustomer.LearningUtilityOptimizer",
                SeedIdGenerator.getId(), "TariffSelector");
  }

  @Override
  public void evaluateTariffs ()
  {
    super.evaluateTariffs();
    // moved recommendProfilesToBundles() to run after subscription
    // repo is updated
  }

  /**
   * calls recommendProfilesToBundles() which needs an updated
   * tariffSubscriptionRepo.
   */
  @Override
  public void updatedSubscriptionRepo ()
  {
    recommendProfilesToBundles();
  }

  private void recommendProfilesToBundles ()
  {
    // Ignore cross-bundle optimization for now. For example, we could
    // produce more locally when we have higher local demand, but we don't
    // currently support net-metering in PowerTAC, so it won't do anything.
    for (CapacityBundle bundle : capacityBundles) {
      List<TariffSubscription> subscriptions = getBundleSubscriptions(bundle);
      if (bundle.getOptimizerStructure().isReceiveRecommendations()) {
        recommendProfilesToBundle(bundle, subscriptions);
      }
    }
  }

  private void recommendProfilesToBundle (CapacityBundle bundle, List<TariffSubscription> subscriptions)
  {
    /**
     * find all subscriptions
     * for each subscription get forecast from each capacity originator in
     * bundle
     * for each forecast, get permutations
     * + use learned history to determine which permutations are more likely
     * + learn value for receptivityIndex and weights for recommendationFactors
     * + weight permutations by learned history
     * compute weight of each permutation as inverse of sum of normalized usage
     * charge and normalized distance from forecast
     * assign weights to each permutation for each CM to make a recommendation
     * + optimize weights of permutations over all capacity originators
     * (anti-herding)
     * make weighted recommendations to each capacity originator
     * + monitor deviation of actual from forecast and also from recommended
     * 
     * TODO - Iterating over CapacityOriginators is not correct for INDIVIDUAL
     * originators. See #956.
     **/

    Map<CapacityOriginator, Map<TariffSubscription, List<CapacityProfile>>> permsPerSub =
        new HashMap<>();
    Map<CapacityOriginator, Map<TariffSubscription, ProfileRecommendation>> recsPerSub =
        new HashMap<>();

    for (CapacityOriginator capacityOriginator : bundle.getCapacityOriginators()) {
      PermutationRule permutationRule =
          bundle.getOptimizerStructure().getPermutationRule();
      if (permutationRule == null) {
        permutationRule = PermutationRule.ALL_SHIFTS;
      }

      // new code - just for useCapacity - per-sub
      permsPerSub.put(capacityOriginator, new HashMap<TariffSubscription, List<CapacityProfile>>());
      for (TariffSubscription sub : subscriptions) {
        // create record per sub
        CapacityProfile forecastPerSub = capacityOriginator.getCurrentForecastPerSub(sub);
        double charge =
            computeProfileUsageChargePerSub(forecastPerSub, sub, capacityOriginator);
        ForecastRecord forecastRecordPerSub =
            new ForecastRecord(forecastPerSub, charge);
        permsPerSub.get(capacityOriginator).put(sub, forecastPerSub.getPermutations(permutationRule));
        insertToRecsMap(recsPerSub, capacityOriginator, sub,
            getProfileRecommendationPerSub(capacityOriginator, bundle,
                forecastRecordPerSub, permsPerSub, sub));
      }
    }

    for (CapacityOriginator capacityOriginator : bundle.getCapacityOriginators()) {
      if (capacityOriginator instanceof ProfileRecommendation.Listener) {
        for (TariffSubscription sub : subscriptions) {
          ProfileRecommendation rec = recsPerSub.get(capacityOriginator).get(sub);
          if (!rec.isEmpty()) {
            log.info(bundle.getName() + ": Submitting "
                + rec.getOpinions().size() + " profile suggestions to "
                + bundle.getCustomerInfo().getPowerType()
                + " capacity originator: "
                + capacityOriginator.getCapacityName());
            ((ProfileRecommendation.Listener) capacityOriginator)
                .handleProfileRecommendationPerSub(rec, sub, capacityOriginator.getCurrentForecast());
          }
          else {
            log.info(bundle.getName()
                + ": No beneficial profile permutations for "
                + bundle.getCustomerInfo().getPowerType()
                + " capacity originator: "
                + capacityOriginator.getCapacityName());
          }
        }
      }
    }
  }

  private void insertToRecsMap (
      Map<CapacityOriginator, Map<TariffSubscription, ProfileRecommendation>> recs,
      CapacityOriginator capacityOriginator, TariffSubscription sub,
      ProfileRecommendation profileRecommendation)
  {
    Map<TariffSubscription, ProfileRecommendation> sub2rec = recs.get(capacityOriginator);
    if (null == sub2rec) {
      sub2rec = new HashMap<>();
      recs.put(capacityOriginator, sub2rec);
    }
    sub2rec.put(sub, profileRecommendation);
  }

  /**
   * Predict how record will be shifted. Computed for one customer.
   * Sent to TariffEvaluationHelper as a part of evaluateTariffs().
   *
   * @param usageForecast
   * @return
   */
  @Override
  public double[] adjustForecastPerTariff (HashMap<CapacityOriginator, double[]> originator2usage, TariffSubscription dummySubscription, CapacityBundle bundle)
  {
    // create a dummy subscription for the 'tariff' parameter
    List<TariffSubscription> subscriptions = new ArrayList<>();
    subscriptions.add(dummySubscription);
    recommendProfilesToBundle(bundle, subscriptions);

    int nextTimeslot = service.getTimeslotRepo().currentSerialNumber() + 1;

    HashMap<CapacityOriginator, double[]> originator2shiftedUsage = new HashMap<>();
    for (CapacityOriginator capacityOriginator : bundle.getCapacityOriginators()) {
      if (capacityOriginator instanceof ProfileRecommendation.Listener) {

        // Daniel: here I changed the code that used to call handleProfileRecommendations
        for (TariffSubscription sub : subscriptions) { // THERE SHOULD ONLY BE 1 (dummy) subscription

          CapacityProfile chosenProfile = capacityOriginator.getForecastPerSubStartingAt(nextTimeslot, sub);

          // converting profile to array
          double[] usage = new double[CapacityProfile.NUM_TIMESLOTS];
          for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            usage[i] = chosenProfile.getCapacity(i);
          }

          originator2shiftedUsage.put(capacityOriginator, usage);
          //// add usage profile to total bundle usage
          //for (int i=0; i < result.length; ++i) {
          //  result[i] += chosenProfile.getCapacity(i);
          //}

          // RESTORE: MUST REMOVE TO AVOID MEMORY LEAK
          //forecastPerSub.remove(sub)
        }
      }
    }
    return super.adjustForecastPerTariff(originator2shiftedUsage, dummySubscription, bundle);
  }

  private List<TariffSubscription>
  getBundleSubscriptions (CapacityBundle bundle)
  {
    return getTariffSubscriptionRepo()
        .findSubscriptionsForCustomer(bundle.getCustomerInfo());
  }

  private ProfileRecommendation
  getProfileRecommendationPerSub (CapacityOriginator capacityOriginator,
                                  CapacityBundle bundle,
                                  ForecastRecord forecastRecord,
                                  Map<CapacityOriginator, Map<TariffSubscription, List<CapacityProfile>>> permsPerSub,
                                  TariffSubscription sub)
  {
    logRecommendationDetails("getProfileRecommendationPerSub(" + sub.getCustomer().getName() + ", " + sub.getTariff().getId() + ") Forecast " + forecastRecord.capacityProfile
        + " usage charge = " + forecastRecord.usageCharge);

    ProfileRecommendation rec = new ProfileRecommendation();
    for (CapacityProfile perm : permsPerSub.get(capacityOriginator).get(sub)) {
      double usageCharge =
          computeProfileUsageChargePerSub(perm, sub, capacityOriginator);
      if (isPermutationAcceptable(capacityOriginator,
          bundle.getOptimizerStructure(), usageCharge,
          forecastRecord.usageCharge)) {
        Opinion opinion = rec.new Opinion();
        // avoid duplication
        opinion.usageCharge = usageCharge;
        opinion.profileChange = forecastRecord.capacityProfile.distanceTo(perm);
        rec.setOpinion(perm, opinion);
      }
    }
    if (!rec.isEmpty()) {
      computeDerivedValues(rec, bundle.getOptimizerStructure());
    }
    return rec;
  }

  private void computeDerivedValues (ProfileRecommendation rec,
                                     ProfileOptimizerStructure optimizerStructure)
  {
    rec.normalizeOpinions();
    rec.computeScores(optimizerStructure.getProfileChangeWeight(),
        optimizerStructure.getBundleValueWeight());
    rec.computeUtilities();
    rec.computeProbabilities(optimizerStructure.getRationalityFactor());
  }

  private double computeProfileUsageChargePerSub (CapacityProfile profile,
                                                  TariffSubscription subscription,
                                                  CapacityOriginator capacityOriginator)
  {
    int timeslot = getTimeslotRepo().currentSerialNumber();
    double totalCharge = 0.0;
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      double totalTimeslotUsage = profile.getCapacity(i);
      double timeslotCharge = 0.0;
      double subTimeslotUsage =
          capacityOriginator.adjustCapacityForSubscription(timeslot,
              totalTimeslotUsage,
              subscription);
      timeslotCharge +=
          subscription.getTariff().getUsageCharge(getTimeslotRepo().getTimeForIndex(timeslot),
              subTimeslotUsage);
      totalCharge += timeslotCharge;
      timeslot += 1;
    }
    return totalCharge;
  }

  private boolean isPermutationAcceptable (CapacityOriginator capacityOriginator,
                                           ProfileOptimizerStructure optimizerStructure,
                                           double permCharge,
                                           double forecastCharge)
  {
    Double threshold = null;
    switch (optimizerStructure.getUsageChargeStance()) {
      case NEUTRAL:
        return true;
      case BENEFIT:
        if (capacityOriginator.getParentBundle().getCustomerInfo().getPowerType()
            .isConsumption()) {
          // less negative is better
          threshold =
              (1.0 - optimizerStructure.getUsageChargePercentBenefit()) * forecastCharge;
        }
        else {
          // PRODUCTION or STORAGE -- more positive is better
          threshold =
              (1.0 + optimizerStructure.getUsageChargePercentBenefit()) * forecastCharge;
        }
        // fall through
      case THRESHOLD:
        if (threshold == null) {
          threshold = optimizerStructure.getUsageChargeThreshold();
        }
        return permCharge > threshold;
      default:
        throw new Error("Unexpected case in usage charge stance: "
            + optimizerStructure.getUsageChargeStance());
    }
  }

  private void logRecommendationDetails (String msg)
  {
    log.debug(msg);
  }

  private class ForecastRecord
  {
    CapacityProfile capacityProfile;
    double usageCharge;

    ForecastRecord (CapacityProfile p, double c)
    {
      capacityProfile = p;
      usageCharge = c;
    }
  }
}


