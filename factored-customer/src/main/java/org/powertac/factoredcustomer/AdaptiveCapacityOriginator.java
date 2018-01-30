/*
* Copyright 2011-2018 the original authors.
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
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
//import org.powertac.common.state.Domain;
import org.powertac.factoredcustomer.ProfileOptimizerStructure.ProfileSelectionMethod;
import org.powertac.factoredcustomer.ProfileRecommendation.Opinion;
import org.powertac.factoredcustomer.ProfileRecommendation.ScoringFactor;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Extends @code{DefaultCapacityOriginator} to adapt to the learning behavior
 * of @code{LearningUtilityOptimizer}.
 *
 * @author Prashant Reddy and John Collins
 */
//@Domain
final class AdaptiveCapacityOriginator extends DefaultCapacityOriginator
    implements ProfileRecommendation.Listener
{
  private static Logger log = LogManager.getLogger(AdaptiveCapacityOriginator.class);

  private final ProfileOptimizerStructure optimizerStructure;
  private final Random recommendationHandler;
  private Map<TariffSubscription, Map<Integer, Double>> forecastCapacitiesPerSub;
  private Map<Tariff, Double> tariff2inconv;
  private TimeslotRepo timeslotRepo;

  public AdaptiveCapacityOriginator (FactoredCustomerService service,
                                     CapacityStructure capacityStructure,
                                     DefaultCapacityBundle bundle)
  {
    super(service, capacityStructure, bundle);

    optimizerStructure = getParentBundle().getOptimizerStructure();

    timeslotRepo = service.getTimeslotRepo();
    RandomSeedRepo randomSeedRepo = service.getRandomSeedRepo();
    recommendationHandler =
        new Random(randomSeedRepo
            .getRandomSeed("factoredcustomer.AdaptiveCapacityOriginator",
                SeedIdGenerator.getId(),
                "RecommendationHandler")
            .getValue());

    forecastCapacitiesPerSub = new HashMap<>();
    tariff2inconv = new HashMap<>();
  }

  @Override
  /** @code{ProfileRecommendation.Listener} **/
  public void handleProfileRecommendation (ProfileRecommendation globalRec)
  {
    double draw1 = recommendationHandler.nextFloat();
    if (draw1 > optimizerStructure.getReactivityFactor()) {
      log.info(logIdentifier + ": Ignoring received profile recommendation");
      return;
    }

    ProfileRecommendation localRec;
    double draw2 = recommendationHandler.nextFloat();
    if (draw2 < optimizerStructure.getReceptivityFactor()) {
      log.info(logIdentifier + ": Adopting profile recommendation as received");
      localRec = globalRec;
    }
    else {
      localRec = new ProfileRecommendation(globalRec.getOpinions());

      Map<ScoringFactor, Double> weights = new HashMap<>();
      weights.put(ScoringFactor.PROFILE_CHANGE, optimizerStructure.getProfileChangeWeight());
      weights.put(ScoringFactor.BUNDLE_VALUE, optimizerStructure.getBundleValueWeight());
      localRec.computeScores(weights);

      localRec.computeUtilities();
      localRec.computeProbabilities(optimizerStructure.getRationalityFactor());
    }
    CapacityProfile chosenProfile;
    if (optimizerStructure.getProfileSelectionMethod() == ProfileSelectionMethod.BEST_UTILITY) {
      chosenProfile = selectBestProfileInRecommendation(localRec);
    }
    else { // LOGIT_CHOICE
      chosenProfile = drawProfileFromRecommendation(localRec);
    }
    overwriteForecastCapacities(timeslotRepo.currentTimeslot(), chosenProfile);
  }

  @Override
  /** @code{ProfileRecommendation.Listener} **/
  public void handleProfileRecommendationPerSub (ProfileRecommendation globalRec,
                                                 TariffSubscription sub,
                                                 CapacityProfile capacityProfile)
  {
    double draw1 = recommendationHandler.nextFloat();
    if (draw1 > optimizerStructure.getReactivityFactor()) {
      log.info(logIdentifier + ": Ignoring received profile recommendation");
      return;
    }

    ProfileRecommendation localRec;
    double draw2 = recommendationHandler.nextFloat();
    if (draw2 < optimizerStructure.getReceptivityFactor()) {
      log.info(logIdentifier + ": Adopting profile recommendation as received");
      localRec = globalRec;
    }
    else {
      localRec = new ProfileRecommendation(globalRec.getOpinions());

      Map<ScoringFactor, Double> weights = new HashMap<>();
      weights.put(ScoringFactor.PROFILE_CHANGE, optimizerStructure.getProfileChangeWeight());
      weights.put(ScoringFactor.BUNDLE_VALUE, optimizerStructure.getBundleValueWeight());
      localRec.computeScores(weights);

      localRec.computeUtilities();
      localRec.computeProbabilities(optimizerStructure.getRationalityFactor());
    }
    CapacityProfile chosenProfile;
    if (optimizerStructure.getProfileSelectionMethod() == ProfileSelectionMethod.BEST_UTILITY) {
      chosenProfile = selectBestProfileInRecommendation(localRec);
    }
    else { // LOGIT_CHOICE
      chosenProfile = drawProfileFromRecommendation(localRec);
    }
    overwriteForecastCapacitiesPerSub(timeslotRepo.currentTimeslot(),
        chosenProfile, sub);
    // record inconv
    // (non-scaled) score = (charge / a) + w x d(e,e') / b
    // so a x score is supposed to be comparable to profile charge,
    // taking inconv into account
    Opinion opinionOnChosenProfile = localRec.getOpinions().get(chosenProfile);
    double originalScore = localRec.getNonScaledScore(chosenProfile);
    // a = charge / normalized-charge
    double costNormalizationConst = (opinionOnChosenProfile.normUsageCharge != 0) ? opinionOnChosenProfile.usageCharge / opinionOnChosenProfile.normUsageCharge : 0;
    // scaled-inconv-factor = |a| x score - charge  = w|a|/b x d(e,e')
    double inconvenienceFactor = Math.abs(costNormalizationConst) * originalScore - opinionOnChosenProfile.usageCharge;
    tariff2inconv.put(sub.getTariff(), inconvenienceFactor);
  }

  private CapacityProfile selectBestProfileInRecommendation (ProfileRecommendation rec)
  {
    double bestUtility = Double.MIN_VALUE;
    CapacityProfile bestProfile = null;
    for (AbstractMap.Entry<CapacityProfile, Double> entry : rec.getUtilities().entrySet()) {
      if (entry.getValue() > bestUtility) {
        bestUtility = entry.getValue();
        bestProfile = entry.getKey();
      }
    }
    if (bestProfile == null) {
      throw new Error("Best profile in recommendation is null!");
    }
    return bestProfile;
  }

  private CapacityProfile drawProfileFromRecommendation (ProfileRecommendation rec)
  {
    double draw = recommendationHandler.nextFloat();
    // sort map entries, for reproducability
    ArrayList<Map.Entry<CapacityProfile, Double>> l =
        new ArrayList<>(rec.getProbabilities().entrySet());
    // TODO Refactor once the new visualizer can handle lambdas
    //Collections.sort(l, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));
    Collections.sort(l, new Comparator<Map.Entry<CapacityProfile, Double>>(){
      @Override
      public int compare(Map.Entry<CapacityProfile, Double> o1, Map.Entry<CapacityProfile, Double> o2) {
        return o1.getValue().compareTo(o2.getValue());
      }});
    // use the sorted map and the draw to sample an entry
    double sumProb = 0.0;
    for (AbstractMap.Entry<CapacityProfile, Double> entry : l) {
      sumProb += entry.getValue();
      if (draw < sumProb) {
        return entry.getKey();
      }
    }
    throw new Error("Drawing from recommendation resulted in a null profile!");
  }

  private void overwriteForecastCapacities (Timeslot timeslot, CapacityProfile profile)
  {
    Timeslot slider = timeslot;
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      forecastCapacities.put(slider.getSerialNumber(), profile.getCapacity(i));
      slider = timeslotRepo.getNext(slider);
    }
  }

  private void overwriteForecastCapacitiesPerSub (Timeslot timeslot, CapacityProfile profile, TariffSubscription sub)
  {
    Timeslot slider = timeslot;
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      int futureTimeslot = slider.getSerialNumber();
      double futureCapacity = profile.getCapacity(i);
      insertIntoForecastCapacitiesPerSub(sub, futureTimeslot, futureCapacity);
      slider = timeslotRepo.getNext(slider);
    }
  }

  private void insertIntoForecastCapacitiesPerSub (TariffSubscription sub,
                                                   int futureTimeslot,
                                                   double futureCapacity)
  {
    Map<Integer, Double> ts2capacity = forecastCapacitiesPerSub.get(sub);
    if (null == ts2capacity) {
      ts2capacity = new HashMap<>();
      forecastCapacitiesPerSub.put(sub, ts2capacity);
    }
    ts2capacity.put(futureTimeslot, futureCapacity);
  }

  @Override
  public double getShiftingInconvenienceFactor (Tariff tariff)
  {
    Double inconv = tariff2inconv.get(tariff);
    // shouldn't happen that it is null..
    if (inconv != null) {
      return inconv;
    }
    log.error("How come inconvenience is null?");
    return 0;
  }

  @Override
  public CapacityAccumulator useCapacity (TariffSubscription subscription)
  {
    int timeslot = timeslotRepo.currentSerialNumber();

    // we don't re-adjust for current weather here;
    // would not be accurate for wind/solar production
    //
    // Daniel: try to get per sub first, if doesn't work get the
    // old, averaged one
    double forecastCapacity = getForecastCapacityPerSub(timeslot, subscription);
    double adjustedCapacity = forecastCapacity;
    adjustedCapacity = adjustCapacityForSubscription(
        timeslot, adjustedCapacity, subscription);
    if (Double.isNaN(adjustedCapacity)) {
      throw new Error("Adjusted capacity is NaN for forecast capacity = "
          + forecastCapacity);
    }

    CapacityAccumulator result =
        addRegCapacityMaybe(subscription, timeslot, adjustedCapacity);
    actualCapacities.put(timeslot, result.getCapacity());
    log.info(logIdentifier + ": Adjusted capacity for tariff "
        + subscription.getTariff().getId() + " = " + result.getCapacity());
    return result;
  }

  // Daniel: some needed additions
  @Override
  public CapacityProfile getCurrentForecastPerSub (TariffSubscription sub)
  {
    int timeslot = timeslotRepo.currentSerialNumber();
    return getForecastPerSubStartingAt(timeslot, sub);
  }

  @Override
  public CapacityProfile getForecastPerSubStartingAt (int startingTimeslot,
                                                      TariffSubscription subscription)
  {
    int timeslot = startingTimeslot;
    List<Double> values = new ArrayList<>();
    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      values.add(getForecastCapacityPerSub(timeslot, subscription));
      timeslot += 1;
    }
    return new CapacityProfile(values);
  }

  private Double getForecastCapacityPerSub (int timeslot,
                                            TariffSubscription subscription)
  {
    Map<Integer, Double> ts2capacity = forecastCapacitiesPerSub.get(subscription);

    if (null == ts2capacity || null == ts2capacity.get(timeslot)) {
      return getForecastCapacity(timeslot);
    }
    else {
      return ts2capacity.get(timeslot);
    }
  }
}


