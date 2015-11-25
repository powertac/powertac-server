/*
* Copyright 2011-2014 the original author or authors.
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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.log4j.Logger;
import org.powertac.common.RandomSeed;
import org.powertac.common.TariffSubscription;
import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.CapacityProfile.PermutationRule;
import org.powertac.factoredcustomer.ProfileRecommendation.Opinion;
import org.powertac.factoredcustomer.interfaces.*;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

/**
 * Key class responsible for managing the tariff(s) for one customer across
 * multiple capacity bundles if necessary.
 * 
 * @author Prashant Reddy
 */
@Domain
class LearningUtilityOptimizer extends DefaultUtilityOptimizer
{
  private static final double NUM_SAMPLING_ITERATIONS = 30;

  private RandomSeed recommendationMaker;

  LearningUtilityOptimizer (CustomerStructure structure,
                            List<CapacityBundle> bundles)
  {
    super(structure, bundles);
    log = Logger.getLogger(LearningUtilityOptimizer.class.getName());
  }

  @Override
  public void initialize (FactoredCustomerService service)
  {
    this.service = service;
    inertiaSampler =
      getRandomSeedRepo()
              .getRandomSeed("factoredcustomer.LearningUtilityOptimizer",
                             SeedIdGenerator.getId(), "InertiaSampler");
    tariffSelector =
      getRandomSeedRepo()
              .getRandomSeed("factoredcustomer.LearningUtilityOptimizer",
                             SeedIdGenerator.getId(), "TariffSelector");
    recommendationMaker =
      getRandomSeedRepo()
              .getRandomSeed("factoredcustomer.LearningUtilityOptimizer",
                             SeedIdGenerator.getId(), "RecommendationMaker");

    subscribeDefault();
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
  public void updatedSubscriptionRepo() {
    recommendProfilesToBundles();  
  }

  private void recommendProfilesToBundles ()
  {
    // Ignore cross-bundle optimization for now. For example, we could
    // produce more locally when we have higher local demand, but we don't
    // currently support net-metering in PowerTAC, so it won't do anything.
    for (CapacityBundle bundle: capacityBundles) {
    	 //log.info("Daniel receiveRecommendations " + bundle.getOptimizerStructure().receiveRecommendations );
      List<TariffSubscription> subscriptions = getBundleSubscriptions(bundle);
      if (bundle.getOptimizerStructure().receiveRecommendations == true) {
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
     **/

    //List<TariffSubscription> subscriptions = getBundleSubscriptions(bundle);

    Map<CapacityOriginator, ForecastRecord> forecasts =
      new HashMap<CapacityOriginator, ForecastRecord>();
    Map<CapacityOriginator, List<CapacityProfile>> perms =
      new HashMap<CapacityOriginator, List<CapacityProfile>>();
    Map<CapacityOriginator, Map<TariffSubscription, List<CapacityProfile>>> permsPerSub =
      new HashMap<CapacityOriginator, Map<TariffSubscription, List<CapacityProfile>>>();
    Map<CapacityOriginator, ProfileRecommendation> recs =
      new HashMap<CapacityOriginator, ProfileRecommendation>();
    Map<CapacityOriginator, Map<TariffSubscription, ProfileRecommendation>> recsPerSub =
      new HashMap<CapacityOriginator, Map<TariffSubscription, ProfileRecommendation>>();

    for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
      //CapacityProfile forecast = capacityOriginator.getCurrentForecast();
      //double charge =
      //  computeProfileUsageCharge(forecast, subscriptions, capacityOriginator);
      //ForecastRecord forecastRecord = new ForecastRecord(forecast, charge);
      //forecasts.put(capacityOriginator, forecastRecord);
      PermutationRule permutationRule =
        bundle.getOptimizerStructure().permutationRule;
      if (permutationRule == null)
        permutationRule = PermutationRule.ALL_SHIFTS;
      //perms.put(capacityOriginator, forecast.getPermutations(permutationRule));
      //log.info(bundle.getName() + ": Evaluating "
      //         + perms.get(capacityOriginator).size()
      //         + " profile permutations for "
      //         + bundle.getCustomerInfo().getPowerType()
      //         + " capacity originator: "
      //         + capacityOriginator.getCapacityName());
      // old code - avg over all subs
      //recs.put(capacityOriginator,
      //         getProfileRecommendation(capacityOriginator, bundle,
      //                                  forecastRecord, perms, subscriptions));
      
      // new code - just for useCapacity - per-sub
      permsPerSub.put(capacityOriginator, new HashMap<TariffSubscription, List<CapacityProfile>>());
      for (TariffSubscription sub : subscriptions) {
        // create record per sub
        CapacityProfile forecastPerSub = capacityOriginator.getCurrentForecastPerSub(sub);
        //log.info("srv basis for perms: " + sub.getCustomer().getName() + " " +  sub.getTariff().getId() + " " + forecastPerSub.toString());
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
//    if (bundle.getOptimizerStructure().raconcileRecommendations == true) {
//      reconcileRecommendations(subscriptions, forecasts, perms, recs);
//    }
    for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
      if (capacityOriginator instanceof ProfileRecommendation.Listener) {
        ProfileRecommendation rec = recs.get(capacityOriginator);
//        if (!rec.isEmpty()) {
//          log.info(bundle.getName() + ": Submitting "
//                   + rec.getOpinions().size() + " profile suggestions to "
//                   + bundle.getCustomerInfo().getPowerType()
//                   + " capacity originator: "
//                   + capacityOriginator.getCapacityName());
          // Daniel: do not overwrite forecastCapacities - leave as default
//          ((ProfileRecommendation.Listener) capacityOriginator)
//                  .handleProfileRecommendation(rec);
//        }
//        else {
//          log.info(bundle.getName()
//                   + ": No beneficial profile permutations for "
//                   + bundle.getCustomerInfo().getPowerType()
//                   + " capacity originator: "
//                   + capacityOriginator.getCapacityName());
//        }
        for (TariffSubscription sub : subscriptions) {
          rec = recsPerSub.get(capacityOriginator).get(sub);
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

  private void insertToRecsMap(
      Map<CapacityOriginator, Map<TariffSubscription, ProfileRecommendation>> recs,
      CapacityOriginator capacityOriginator, TariffSubscription sub,
      ProfileRecommendation profileRecommendation) {
    Map<TariffSubscription, ProfileRecommendation> sub2rec = recs.get(capacityOriginator);
    if (null == sub2rec) {
      sub2rec = new HashMap<TariffSubscription, ProfileRecommendation>();
      recs.put(capacityOriginator, sub2rec);
    }
    sub2rec.put(sub, profileRecommendation);
  }

  /**
   * Predict how record will be shifted. Computed for one customer. 
   * Sent to TariffEvaluationHelper as a part of evaluateTariffs().
   * @param usageForecast
   * @return
   */
  @Override
  public double[] adjustForecastPerTariff(HashMap<CapacityOriginator,double[]> originator2usage, TariffSubscription dummySubscription, CapacityBundle bundle) {
    //log.info("Daniel newway LearningUtilityOptimizer.adjustForecastPerTariff()");
    // create a dummy subscription for the 'tariff' parameter
    List<TariffSubscription> subscriptions = new ArrayList<TariffSubscription>();
    subscriptions.add(dummySubscription);
    recommendProfilesToBundle(bundle, subscriptions);

    
    int nextTimeslot = service.getTimeslotRepo().currentSerialNumber() + 1;

    
    HashMap<CapacityOriginator, double[]> originator2shiftedUsage = new HashMap<CapacityOriginator, double[]>();
    //double[] result = new double[originator2usage.values().iterator().next().length];
    for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
      if (capacityOriginator instanceof ProfileRecommendation.Listener) {
        
        // Daniel: here I changed the code that used to call handleProfileRecommendations
        for (TariffSubscription sub : subscriptions) { // THERE SHOULD ONLY BE 1 (dummy) subscription
          
          CapacityProfile chosenProfile = capacityOriginator.getForecastPerSubStartingAt(nextTimeslot, sub);
          
          // converting profile to array
          double[] usage = new double[CapacityProfile.NUM_TIMESLOTS];
          for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
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

  private
    ProfileRecommendation
    getProfileRecommendation (CapacityOriginator capacityOriginator,
                              CapacityBundle bundle,
                              ForecastRecord forecastRecord,
                              Map<CapacityOriginator, List<CapacityProfile>> perms,
                              List<TariffSubscription> subscriptions)
  {
    //logRecommendationDetails("getProfileRecommendation() Forecast " + forecastRecord.capacityProfile
    //                         + " usage charge = " + forecastRecord.usageCharge);

    ProfileRecommendation rec = new ProfileRecommendation();
    for (CapacityProfile perm: perms.get(capacityOriginator)) {
      double usageCharge =
        computeProfileUsageCharge(perm, subscriptions, capacityOriginator);
      //logRecommendationDetails("getProfileRecommendation() Permutation " + perm + " usage charge = "
      //                         + usageCharge);
      if (isPermutationAcceptable(capacityOriginator,
                                  bundle.getOptimizerStructure(), usageCharge,
                                  forecastRecord.usageCharge)) {
        Opinion opinion = rec.new Opinion();
        opinion.usageCharge =
          computeProfileUsageCharge(perm, subscriptions, capacityOriginator);
        opinion.profileChange = forecastRecord.capacityProfile.distanceTo(perm);
        //logRecommendationDetails("profile distance: " + opinion.profileChange);
        rec.setOpinion(perm, opinion);
      }
    }
    if (!rec.isEmpty())
      computeDerivedValues(rec, bundle.getOptimizerStructure());
    return rec;
  }

  private
    ProfileRecommendation
    getProfileRecommendationPerSub (CapacityOriginator capacityOriginator,
                              CapacityBundle bundle,
                              ForecastRecord forecastRecord,
                              Map<CapacityOriginator, Map<TariffSubscription, List<CapacityProfile>>> permsPerSub,
                              //List<TariffSubscription> subscriptions)
                              TariffSubscription sub)
  {
    logRecommendationDetails("getProfileRecommendationPerSub(" + sub.getCustomer().getName() + ", " + sub.getTariff().getId() + ") Forecast " + forecastRecord.capacityProfile
                             + " usage charge = " + forecastRecord.usageCharge);

    ProfileRecommendation rec = new ProfileRecommendation();
    for (CapacityProfile perm: permsPerSub.get(capacityOriginator).get(sub)) {
      double usageCharge =
        computeProfileUsageChargePerSub(perm, sub, capacityOriginator);
      //logRecommendationDetails("getProfileRecommendationPerSub(" + sub.getTariff.getId() + ") Permutation " + perm + " usage charge = " + usageCharge);
      if (isPermutationAcceptable(capacityOriginator,
                                  bundle.getOptimizerStructure(), usageCharge,
                                  forecastRecord.usageCharge)) {
        Opinion opinion = rec.new Opinion();
        opinion.usageCharge =
          // avoid duplication
          usageCharge; 
          //computeProfileUsageChargePerSub(perm, sub, capacityOriginator);
        opinion.profileChange = forecastRecord.capacityProfile.distanceTo(perm);
        //logRecommendationDetails("getProfileRecommendationPerSub(" + sub.getCustomer().getName() + ", " +
        //  sub.getTariff().getId() + ") Permutation " + perm + " usage charge = " +
        //  usageCharge + " distanceTo=" + opinion.profileChange);
        rec.setOpinion(perm, opinion);
      }
    }
    if (!rec.isEmpty())
      computeDerivedValues(rec, bundle.getOptimizerStructure());
    return rec;
  }

  private void
    computeDerivedValues (ProfileRecommendation rec,
                          ProfileOptimizerStructure optimizerStructure)
  {
    rec.normalizeOpinions();
    rec.computeScores(optimizerStructure.profileChangeWeight,
                      optimizerStructure.bundleValueWeight);
    rec.computeUtilities();
    rec.computeProbabilities(optimizerStructure.rationalityFactor);
  }

  private double
    computeProfileUsageCharge (CapacityProfile profile,
                               List<TariffSubscription> subscriptions,
                               CapacityOriginator capacityOriginator)
  {
    int timeslot = getTimeslotRepo().currentSerialNumber();
    double totalCharge = 0.0;
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      double totalTimeslotUsage = profile.getCapacity(i);
      // System.out.println("timeslot usage total = " + totalTimeslotUsage);
      double timeslotCharge = 0.0;
      for (TariffSubscription subscription: subscriptions) {
        double subTimeslotUsage =
          capacityOriginator.adjustCapacityForSubscription(timeslot,
                                                           totalTimeslotUsage,
                                                           subscription);
        // System.out.println("timeslot usage for subscription = " +
        // totalTimeslotUsage);
        timeslotCharge +=
          subscription.getTariff().getUsageCharge(getTimeslotRepo()
                                                  .getTimeForIndex(timeslot),
                                                  subTimeslotUsage, 0.0);
        // System.out.println("timeslot charge = " + timeslotCharge);
      }
      totalCharge += timeslotCharge;
      timeslot += 1;
    }
    return totalCharge;
  }

  private double
    computeProfileUsageChargePerSub (CapacityProfile profile,
                               TariffSubscription subscription,
                               CapacityOriginator capacityOriginator)
  {
    int timeslot = getTimeslotRepo().currentSerialNumber();
    double totalCharge = 0.0;
    //logRecommendationDetails("computeProfileUsageCharge(), CapacityProfile.NUM_TIMESLOTS=" + CapacityProfile.NUM_TIMESLOTS);
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      double totalTimeslotUsage = profile.getCapacity(i);
      //logRecommendationDetails("totalTimeslotUsage=" + totalTimeslotUsage);
      // System.out.println("timeslot usage total = " + totalTimeslotUsage);
      double timeslotCharge = 0.0;
      //for (TariffSubscription subscription: subscriptions) {
        double subTimeslotUsage =
          capacityOriginator.adjustCapacityForSubscription(timeslot,
                                                           totalTimeslotUsage,
                                                           subscription);
        //logRecommendationDetails("subTimeslotUsage=" + subTimeslotUsage);
        // System.out.println("timeslot usage for subscription = " +
        // totalTimeslotUsage);
        timeslotCharge +=
          subscription.getTariff().getUsageCharge(getTimeslotRepo().getTimeForIndex(timeslot),
                                                  subTimeslotUsage, 0.0); // TODO: why cumulative usage is 0?
        //logRecommendationDetails("timeslotCharge=" + timeslotCharge);
        // System.out.println("timeslot charge = " + timeslotCharge);
      //}
      totalCharge += timeslotCharge;
      timeslot += 1;
    }
    return totalCharge;
  }

  private boolean
    isPermutationAcceptable (CapacityOriginator capacityOriginator,
                             ProfileOptimizerStructure optimizerStructure,
                             double permCharge, double forecastCharge)
  {
    Double threshold = null;
    switch (optimizerStructure.usageChargeStance) {
    case NEUTRAL:
      return true;
    case BENEFIT:
      if (capacityOriginator.getParentBundle().getCustomerInfo().getPowerType()
              .isConsumption()) {
        // less negative is better
        threshold =
          (1.0 - optimizerStructure.usageChargePercentBenefit) * forecastCharge;
      }
      else {
        // PRODUCTION or STORAGE -- more positive is better
        threshold =
          (1.0 + optimizerStructure.usageChargePercentBenefit) * forecastCharge;
      }
      // fall through
    case THRESHOLD:
      if (threshold == null) {
        threshold = optimizerStructure.usageChargeThreshold;
      }
      return permCharge > threshold;
    default:
      throw new Error("Unexpected case in usage charge stance: "
                      + optimizerStructure.usageChargeStance);
    }
  }

  private
    void
    reconcileRecommendations (List<TariffSubscription> subscriptions,
                              Map<CapacityOriginator, ForecastRecord> forecasts,
                              Map<CapacityOriginator, List<CapacityProfile>> perms,
                              Map<CapacityOriginator, ProfileRecommendation> recs)
  {
    // TODO: adjust for accumulation towards tiered rates across capacity
    // originators

    for (AbstractMap.Entry<CapacityOriginator, ProfileRecommendation> targetEntry: recs
            .entrySet()) {
      CapacityOriginator targetOriginator = targetEntry.getKey();
      ProfileRecommendation targetRec = targetEntry.getValue();
      if (targetRec.isEmpty()
          || targetOriginator.getParentBundle().getCapacityOriginators().size() == 1) {
        continue;
      }
      double[] othersCapacities = new double[CapacityProfile.NUM_TIMESLOTS];
      for (int s = 0; s < NUM_SAMPLING_ITERATIONS; ++s) {
        for (AbstractMap.Entry<CapacityOriginator, ProfileRecommendation> otherEntry: recs
                .entrySet()) {
          ProfileRecommendation otherRec = otherEntry.getValue();
          CapacityProfile otherProfile;
          if (otherRec.isEmpty()) {
            otherProfile = forecasts.get(otherEntry.getKey()).capacityProfile;
          }
          else {
            otherProfile = drawProfileFromRecommendation(otherRec);
          }
          for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            othersCapacities[i] += otherProfile.getCapacity(i);
            if (s == NUM_SAMPLING_ITERATIONS) {
              othersCapacities[i] =
                othersCapacities[i] / (double) NUM_SAMPLING_ITERATIONS;
            }
          }
        }
      }
      CapacityProfile forecastProfile =
        forecasts.get(targetOriginator).capacityProfile;
      double forecastVariance =
        computeAggregateVariance(forecastProfile, othersCapacities);
      for (AbstractMap.Entry<CapacityProfile, Opinion> opinionEntry: targetRec
              .getOpinions().entrySet()) {
        CapacityProfile targetProfile = opinionEntry.getKey();
        double targetVariance =
          computeAggregateVariance(targetProfile, othersCapacities);
        double bundleValue = forecastVariance / targetVariance;
        opinionEntry.getValue().bundleValue = bundleValue;
      }
      computeDerivedValues(targetRec, targetOriginator.getParentBundle()
              .getOptimizerStructure()); // TODO use local opt-structure
    }
  }

  private double computeAggregateVariance (CapacityProfile profile,
                                           double[] otherCapacities)
  {
    double[] aggCapacities = new double[CapacityProfile.NUM_TIMESLOTS];
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      aggCapacities[i] = profile.getCapacity(i) + otherCapacities[i];
    }
    return new Variance().evaluate(aggCapacities);
  }

  private CapacityProfile
    drawProfileFromRecommendation (ProfileRecommendation rec)
  {
    double draw = recommendationMaker.nextFloat();
    double sumProb = 0.0;
    for (AbstractMap.Entry<CapacityProfile, Double> entry: rec
            .getProbabilities().entrySet()) {
      sumProb += entry.getValue();
      if (draw < sumProb) {
        return entry.getKey();
      }
    }
    throw new Error("Drawing from recommendation resulted in a null profile!");
  }

  private void logRecommendationDetails (String msg)
  {
    // log.info(msg);
    log.debug(msg);
  }

  // INNER CLASS

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

} // end class


