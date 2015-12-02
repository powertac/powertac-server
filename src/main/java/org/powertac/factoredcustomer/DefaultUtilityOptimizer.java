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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.factoredcustomer.interfaces.*;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

/**
 * Key class responsible for managing the tariff(s) for one customer across
 * multiple capacity bundles if necessary.
 * 
 * @author Prashant Reddy, John Collins
 */
@Domain
class DefaultUtilityOptimizer implements UtilityOptimizer
{
  private static Logger log =
          LogManager.getLogger(DefaultUtilityOptimizer.class.getName());

  protected FactoredCustomerService service;

  protected static final int NUM_HOURS_IN_DAY = 24;
  protected static final long MEAN_TARIFF_DURATION = 5; // number of days
  protected static int tariffEvalCount = 5; // # of tariffs/powerType to eval

  // Evaluation parameters

  protected final CustomerStructure customerStructure;
  protected final List<CapacityBundle> capacityBundles;

  //protected final List<Tariff> ignoredTariffs = new ArrayList<Tariff>();
  protected RandomSeed inertiaSampler;
  protected RandomSeed tariffSelector;

  //protected HashMap<Tariff, Integer> allocations;
  // tariff evaluators
  protected HashMap<CapacityBundle, TariffEvaluator> evaluatorMap;

  protected List<CapacityBundle> bundlesWithRevokedTariffs =
    new ArrayList<CapacityBundle>();

  DefaultUtilityOptimizer (CustomerStructure structure,
                           List<CapacityBundle> bundles)
  {
    customerStructure = structure;
    capacityBundles = bundles;
    evaluatorMap = new HashMap<CapacityBundle, TariffEvaluator>();

    // create evaluation wrappers and tariff evaluators for each bundle
    for (CapacityBundle bundle : bundles) {
      TariffSubscriberStructure subStructure = bundle.getSubscriberStructure();
      TariffEvaluator evaluator =
              new TariffEvaluator(new TariffEvaluationWrapper(bundle))
              .withChunkSize(Math.max(1, bundle.getPopulation()/1000))
              .withTariffSwitchFactor(subStructure.tariffSwitchFactor)
              .withPreferredContractDuration(subStructure.expectedDuration)
              .withInconvenienceWeight(subStructure.inconvenienceWeight)
              .withRationality(subStructure.logitChoiceRationality)
              .withEvaluateAllTariffs(true);
      evaluator.initializeCostFactors(subStructure.expMeanPriceWeight,
                                      subStructure.maxValuePriceWeight,
                                      subStructure.realizedPriceWeight,
                                      subStructure.tariffVolumeThreshold);
      evaluator.initializeInconvenienceFactors(subStructure.touFactor,
                                               subStructure.tieredRateFactor,
                                               subStructure.variablePricingFactor,
                                               subStructure.interruptibilityFactor);
      evaluatorMap.put(bundle, evaluator);

    }
  }

  @Override
  public void initialize (FactoredCustomerService service)
  {
    this.service = service;
    inertiaSampler =
      getRandomSeedRepo()
              .getRandomSeed("factoredcustomer.DefaultUtilityOptimizer",
                             SeedIdGenerator.getId(), "InertiaSampler");
    tariffSelector =
      getRandomSeedRepo()
              .getRandomSeed("factoredcustomer.DefaultUtilityOptimizer",
                             SeedIdGenerator.getId(), "TariffSelector");

    subscribeDefault();
  }
  
  // ----- Access components through service to support mocking ------

  protected RandomSeedRepo getRandomSeedRepo ()
  {
    return service.getRandomSeedRepo();
  }

  protected TariffMarket getTariffMarket ()
  {
    return service.getTariffMarket();
  }

  protected TariffSubscriptionRepo getTariffSubscriptionRepo ()
  {
    return service.getTariffSubscriptionRepo();
  }
  
  protected TariffRepo getTariffRepo ()
  {
    return service.getTariffRepo();
  }
  
  protected TimeslotRepo getTimeslotRepo ()
  {
    return service.getTimeslotRepo();
  }

  // /////////////// TARIFF SUBSCRIPTION //////////////////////

  @StateChange
  private void subscribe (Tariff tariff, CapacityBundle bundle,
                            int customerCount, boolean verbose)
  {
    getTariffMarket().subscribeToTariff(tariff, bundle.getCustomerInfo(),
                                          customerCount);
    if (verbose)
      log.info(bundle.getName() + ": Subscribed " + customerCount
               + " customers to tariff " + tariff.getId() + " successfully");
  }

//  @StateChange
//  protected void unsubscribe (TariffSubscription subscription,
//                              CapacityBundle bundle, int customerCount,
//                              boolean verbose)
//  {
//    subscription.unsubscribe(customerCount);
//    if (verbose)
//      log.info(bundle.getName() + ": Unsubscribed " + customerCount
//               + " customers from tariff " + subscription.getTariff().getId()
//               + " successfully");
//  }

  /** @Override hook **/
  protected void subscribeDefault ()
  {
    for (CapacityBundle bundle: capacityBundles) {
      PowerType powerType = bundle.getPowerType();
      if (getTariffMarket().getDefaultTariff(powerType) != null) {
        log.info(bundle.getName() + ": Subscribing " + bundle.getPopulation()
                 + " customers to default " + powerType + " tariff");
        subscribe(getTariffMarket().getDefaultTariff(powerType), bundle,
                  bundle.getPopulation(), false);
      }
      else {
        log.info(bundle.getName() + ": No default tariff for power type "
                 + powerType + "; trying generic type");
        PowerType genericType = powerType.getGenericType();
        if (getTariffMarket().getDefaultTariff(genericType) == null) {
          log.error(bundle.getName()
                    + ": No default tariff for generic power type "
                    + genericType + " either!");
        }
        else {
          log.info(bundle.getName() + ": Subscribing " + bundle.getPopulation()
                   + " customers to default " + genericType + " tariff");
          subscribe(getTariffMarket().getDefaultTariff(genericType), bundle,
                    bundle.getPopulation(), false);
        }
      }
    }
  }

  // Tariff Evaluation --------------------------------
  @Override
  public void evaluateTariffs ()
  {
    for (CapacityBundle bundle: capacityBundles) {
      TariffEvaluator evaluator = evaluatorMap.get(bundle); 
      if (bundle.getSubscriberStructure().inertiaDistribution != null) {
        evaluator.withInertia
          (bundle.getSubscriberStructure().inertiaDistribution.drawSample());
      }
      else {
        log.warn("no inertia distro, using default value 0.7");
        evaluator.withInertia(0.7);
      }
      evaluator.evaluateTariffs();
    }
    bundlesWithRevokedTariffs.clear();
  }


  // /////////////// TIMESLOT ACTIVITY //////////////////////

  /**
   * used by LearningUtilityOptimizer to execute actions
   * that need updated repo (currently shifting computations)
   */
  @Override
  public void updatedSubscriptionRepo() {
    
  }

  @Override
  public void handleNewTimeslot (Timeslot timeslot)
  {
    //log.info("handleNewTimeslot()");
    //checkRevokedSubscriptions();
    usePower(timeslot);
  }

  //private void checkRevokedSubscriptions ()
  //{
  //  for (CapacityBundle bundle: capacityBundles) {
  //    List<TariffSubscription> revoked =
  //      getTariffSubscriptionRepo().getRevokedSubscriptionList(bundle
  //              .getCustomerInfo());
  //    for (TariffSubscription revokedSubscription: revoked) {
  //      revokedSubscription.handleRevokedTariff();
  //      bundlesWithRevokedTariffs.add(bundle);
  //    }
  //  }
  //}

  private void usePower (Timeslot timeslot)
  {
    //log.info("usePower()");
    for (CapacityBundle bundle: capacityBundles) {
      List<TariffSubscription> subscriptions =
        getTariffSubscriptionRepo().findActiveSubscriptionsForCustomer(bundle
                .getCustomerInfo());
      double totalCapacity = 0.0;
      double totalUsageCharge = 0.0;
      for (TariffSubscription subscription: subscriptions) {
        double usageSign = bundle.getPowerType().isConsumption()? +1: -1;
        double currCapacity = usageSign * useCapacity(subscription, bundle);
        if (service.getUsageChargesLogging() == true) {
          double charge =
            subscription.getTariff().getUsageCharge(currCapacity,
                                                    subscription
                                                            .getTotalUsage(),
                                                    false);
          totalUsageCharge += charge;
        }
        subscription.usePower(currCapacity);
        //log.info("currCapaticy=" + currCapacity);
        totalCapacity += currCapacity;
      }
      log.info(bundle.getName() + ": Total " + bundle.getPowerType()
               + " capacity for timeslot " + timeslot.getSerialNumber() + " = "
               + totalCapacity);
      logUsageCharges(bundle.getName() + ": Total " + bundle.getPowerType()
                      + " usage charge for timeslot "
                      + timeslot.getSerialNumber() + " = " + totalUsageCharge);
    }
  }

  public double useCapacity (TariffSubscription subscription,
                             CapacityBundle bundle)
  {
    //log.info("useCapacity()");
    double capacity = 0;
    for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
      capacity += capacityOriginator.useCapacity(subscription);
      //log.info("updated capacity " + capacity );
    }
    return capacity;
  }

  protected String getCustomerName ()
  {
    return customerStructure.name;
  }

  private void logUsageCharges (String msg)
  {
    if (service.getUsageChargesLogging() == true) {
      log.info(msg);
    }
  }

  @Override
  public String toString ()
  {
    return this.getClass().getCanonicalName() + ":" + getCustomerName();
  }

  class TariffEvaluationWrapper implements CustomerModelAccessor
  {
    private CapacityBundle bundle;
    private TariffSubscriberStructure subStructure;

    TariffEvaluationWrapper (CapacityBundle bundle)
    {
      this.bundle = bundle;
      subStructure = bundle.getSubscriberStructure();
    }

    @Override
    public CustomerInfo getCustomerInfo ()
    {
      return bundle.getCustomerInfo();
    }

    @Override
    public org.powertac.common.CapacityProfile getCapacityProfile (Tariff tariff)
    {
      double usageSign = bundle.getPowerType().isConsumption()? +1: -1;
      
      //// Old code: non-shifted prediction - incorrect for tariff-eval purposes
      //// =======================================================================
      //double[] oldforecast;
      //if ( true ) {
      //  // original code (with a bug fix)
      //  double[] usageForecast = new double[CapacityProfile.NUM_TIMESLOTS];
      //  for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
      //    CapacityProfile forecast = capacityOriginator.getForecastForNextTimeslot();
      //    for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
      //      double hourlyUsage = usageSign * forecast.getCapacity(i);
      //      usageForecast[i] += hourlyUsage / bundle.getPopulation();
      //      //log.info("forecast.getCapacity(i)=" + forecast.getCapacity(i) + "hourlyUsage=" + hourlyUsage + "usageForecast[i]" + usageForecast[i]);
      //    }
      //    //log.info("oldway " + forecast.toString());
      //    
      //  }
      //  //log.info("oldway total" + Arrays.toString(usageForecast));
      //  
      //  oldforecast = usageForecast;
      //}
      
      // New code: shifted prediction - correct for tariff-eval purposes
      // ===============================================================
      double[] newForecast;
      if (true) { // else {
        
        // new code
        HashMap<CapacityOriginator, double[]> originator2usage = new HashMap<CapacityOriginator, double[]>();
        for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
          double[] usageForecast = new double[CapacityProfile.NUM_TIMESLOTS];
          // BUG FIX: this function is called from forecastCost() and used
          // by TariffEvaluationHelper, which assumes the forcast starts
          // at the next timeslot
          CapacityProfile forecast = capacityOriginator.getForecastForNextTimeslot();
          for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
            double hourlyUsage = usageSign * forecast.getCapacity(i);
            usageForecast[i] = hourlyUsage ;// don't divide yet / bundle.getPopulation();
            //log.info("forecast.getCapacity(i)=" + forecast.getCapacity(i) + "hourlyUsage=" + hourlyUsage + "usageForecast[i]" + usageForecast[i]);
          }
          //log.info("newway " + Arrays.toString(usageForecast));
          originator2usage .put(capacityOriginator, usageForecast);
        }

        // Refactored the following code for LearningUtilityOptimizer - shift profile
        //
        // create dummy subscription for the above usage vector: 
        // 1 population member under 'tariff', (consuming the sum 
        // of the originators' usage)
        TariffSubscription dummySubscription = new DummyTariffSubscription(getCustomerInfo(), tariff);
        newForecast = adjustForecastPerTariff(originator2usage, dummySubscription, bundle);
        //log.info("newway total" + Arrays.toString(newForecast));
        
        
      }
        
      //// Compare old forecast and new - they must be different for *shifting-customers* *under non-fixed-rate-tariffs*.
      // 
      //if (! Arrays.equals(newForecast, oldforecast) ) {
      //  log.info("DIFFERENTFORECASTS " + bundle.getCustomerInfo().getName() + " " + tariff.getId());
      //  log.info("old: " + Arrays.toString(oldforecast));
      //  log.info("new: " + Arrays.toString(newForecast));
      //}
      //else {
      //  log.info("SAMEFORECASTS" + bundle.getCustomerInfo().getName() + " " + tariff.getId());
      //  log.info("old/new: " + newForecast);
      //}

      double[] result = newForecast;
      //double[] result = oldforecast;
      //log.info("returning " + Arrays.toString(result));
      Instant start =
          service.getTimeService().getCurrentTime().plus(TimeService.HOUR);
      return new org.powertac.common.CapacityProfile(result, start); 
    }

    @Override
    public double getBrokerSwitchFactor (boolean isSuperseding)
    {
      double result = subStructure.brokerSwitchFactor; 
      if (isSuperseding)
        return result * 5.0;
      return result;
    }

    @Override
    public double getTariffChoiceSample ()
    {
      return tariffSelector.nextDouble();
    }

    @Override
    public double getInertiaSample ()
    {
      return inertiaSampler.nextDouble();
    }

    /**
     * Is it correct to sum inconveniences over originators? currently every
     * shifting customer has 1 originator so this doesn't matter, but it might
     * change in the future.
     */
    @Override
    public double getShiftingInconvenienceFactor(Tariff tariff) {
      double inconv = 0;
      for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
        inconv +=  capacityOriginator.getShiftingInconvenienceFactor(tariff);
      }
      return inconv;
    }
  }

  /**
   * 
   * HACK, accessed from inner class, overriden from derived class
   * 
   * sum all originators' usage arrays
   * @param originator2usage
   * @param dummySubscription 
   * @param bundle 
   * @return
   */
  public double[] adjustForecastPerTariff(HashMap<CapacityOriginator,double[]> originator2usage, TariffSubscription dummySubscription, CapacityBundle bundle) {
    //log.info("newway DefaultUtilityOptimizer.adjustForecastPerTariff()");
    
    // sum all originators' usage arrays
    double [] result = new double[originator2usage.values().iterator().next().length];
    for (double[] usage : originator2usage.values()) {
      for (int i = 0; i < result.length; ++i) {
        result[i] += usage[i] / bundle.getPopulation();
      }
    }
    return result;
  }

  class DummyTariffSubscription extends TariffSubscription {

    int count; 
    
    public DummyTariffSubscription(CustomerInfo customer, Tariff tariff) {
      super(customer, tariff);
    }
    
    @Override
    public int getCustomersCommitted () {
      return 1;
    }
    
  }

} // end class


