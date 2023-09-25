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
import org.joda.time.Instant;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.customer.AbstractCustomer;
//import org.powertac.common.state.Domain;
//import org.powertac.common.state.StateChange;
import org.powertac.factoredcustomer.interfaces.CapacityBundle;
import org.powertac.factoredcustomer.interfaces.CapacityOriginator;
import org.powertac.factoredcustomer.interfaces.UtilityOptimizer;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * Key class responsible for managing the tariff(s) for one customer across
 * multiple capacity bundles if necessary.
 *
 * @author Prashant Reddy, John Collins
 */
//@Domain
class DefaultUtilityOptimizer
extends AbstractCustomer
implements UtilityOptimizer
{
  private static Logger log =
      LogManager.getLogger(DefaultUtilityOptimizer.class.getName());

  //protected FactoredCustomerService service;

  private final CustomerStructure customerStructure;
  protected final List<CapacityBundle> capacityBundles;

  protected RandomSeed inertiaSampler;
  protected RandomSeed tariffSelector;

  private HashMap<CapacityBundle, TariffEvaluator> evaluatorMap;

  DefaultUtilityOptimizer (CustomerStructure customerStructure,
                           List<CapacityBundle> bundles)
  {
    this.customerStructure = customerStructure;
    this.capacityBundles = bundles;
    this.evaluatorMap = new HashMap<>();
  }

  @Override
  public void initialize (FactoredCustomerService service)
  {
    super.setServiceAccessor(service);
    this.service = service;
    super.initialize();

    // create evaluation wrappers and tariff evaluators for each bundle
    for (CapacityBundle bundle : capacityBundles) {
      TariffSubscriberStructure subStructure = bundle.getSubscriberStructure();
      TariffEvaluator evaluator =
          createTariffEvaluator(new TariffEvaluationWrapper(bundle))
              .withChunkSize(Math.max(1, bundle.getPopulation() / 1000))
                // should be a configurable value
              .withTariffSwitchFactor(subStructure.getTariffSwitchFactor())
              .withPreferredContractDuration(subStructure.getExpectedDuration())
              .withInconvenienceWeight(subStructure.getInconvenienceWeight())
              .withRationality(subStructure.getLogitChoiceRationality())
              .withEvaluateAllTariffs(true);
      evaluator.initializeCostFactors(subStructure.getExpMeanPriceWeight(),
          subStructure.getMaxValuePriceWeight(),
          subStructure.getRealizedPriceWeight(),
          subStructure.getTariffVolumeThreshold());
      evaluator.initializeInconvenienceFactors(subStructure.getTouFactor(),
          //subStructure.getTieredRateFactor(),
          subStructure.getVariablePricingFactor(),
          subStructure.getInterruptibilityFactor());
      evaluator.initializeRegulationFactors(subStructure.getExpUpRegulation(),
                                            0.0,
                                            subStructure.getExpDownRegulation());
      //log.info("evaluator set for bundle {}", bundle.getName());
      evaluatorMap.put(bundle, evaluator);
    }

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

  //@StateChange
  private void subscribe (Tariff tariff, CapacityBundle bundle,
                          int customerCount, boolean verbose)
  {
    getTariffMarket().subscribeToTariff(tariff, bundle.getCustomerInfo(),
        customerCount);
    if (verbose) {
      log.info(bundle.getName() + ": Subscribed " + customerCount
          + " customers to tariff " + tariff.getId() + " successfully");
    }
  }

  /**
   * @Override hook
   **/
  protected void subscribeDefault ()
  {
    for (CapacityBundle bundle : capacityBundles) {
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
    for (CapacityBundle bundle : capacityBundles) {
      TariffEvaluator evaluator = evaluatorMap.get(bundle);
      if (null == evaluator)
        log.error("null evaluator, bundle {}", bundle.getName());
      if (bundle.getSubscriberStructure().getInertiaDistribution() != null) {
        evaluator.withInertia(bundle.getSubscriberStructure()
            .getInertiaDistribution().drawSample());
      }
      else {
        log.warn("no inertia distro, using default value 0.7");
        evaluator.withInertia(0.7);
      }
      evaluator.evaluateTariffs();
    }
  }

  // /////////////// TIMESLOT ACTIVITY //////////////////////

  /**
   * used by LearningUtilityOptimizer to execute actions
   * that need updated repo (currently shifting computations)
   */
  @Override
  public void updatedSubscriptionRepo ()
  {

  }

  @Override
  public void step()
  {
    usePower();
  }

  // TODO - needs fix for #956
  private void usePower ()
  {
    for (CapacityBundle bundle : capacityBundles) {
      List<TariffSubscription> subscriptions =
          getTariffSubscriptionRepo()
          .findActiveSubscriptionsForCustomer(bundle.getCustomerInfo());
      double totalCapacity = 0.0;
      double totalUsageCharge = 0.0;
      // copy the list so we can pull the INDIVIDUAL originators apart
      ArrayList<CapacityOriginator> allOriginators =
              new ArrayList<>(bundle.getCapacityOriginators());
      int lastOriginator = 0;
      for (TariffSubscription subscription : subscriptions) {
        double usageSign = bundle.getPowerType().isConsumption() ? +1 : -1;
        List<CapacityOriginator> originators = allOriginators;
        // for INDIVIDUAL originators, we pull the list apart and
        // always pass the number equal to the subscribed population.
        if (bundle.isAllIndividual()) {
          originators =
                  allOriginators.subList(lastOriginator,
                                         lastOriginator + subscription.getCustomersCommitted());
          lastOriginator += subscription.getCustomersCommitted();
        }
        CapacityAccumulator ca = useCapacity(bundle, subscription, originators);
        double currCapacity = usageSign * ca.getCapacity();
        if (Config.getInstance().isUsageChargesLogging()) {
          double charge =
              subscription.getTariff().getUsageCharge(currCapacity,
//                  subscription.getTotalUsage(),
                  false);
          totalUsageCharge += charge;
        }
        subscription.usePower(currCapacity);
        subscription.setRegulationCapacity(new RegulationCapacity(subscription,
                                                                  ca.getUpRegulationCapacity(),
                                                                  ca.getDownRegulationCapacity()));
        totalCapacity += currCapacity;
      }
      log.info(bundle.getName() + ": Total " + bundle.getPowerType()
          + " capacity " + " = " + totalCapacity);
      logUsageCharges(bundle.getName() + ": Total " + bundle.getPowerType()
          + " usage charge " + " = " + totalUsageCharge);
    }
  }

  private CapacityAccumulator useCapacity (CapacityBundle bundle,
                                           TariffSubscription subscription,
                                           List<CapacityOriginator> originators)
  {
    CapacityAccumulator capacity = new CapacityAccumulator();
    for (CapacityOriginator capacityOriginator : originators) {
      capacity.add(capacityOriginator.useCapacity(subscription));
    }
    if (bundle.isAllIndividual()) {
      capacity.scale((double)subscription.getCustomersCommitted() /
                     (double)originators.size());
    }
    return capacity;
  }

  private String getCustomerName ()
  {
    return customerStructure.getName();
  }

  private void logUsageCharges (String msg)
  {
    if (Config.getInstance().isUsageChargesLogging()) {
      log.info(msg);
    }
  }

  @Override
  public String toString ()
  {
    return this.getClass().getCanonicalName() + ":" + getCustomerName();
  }

  private class TariffEvaluationWrapper implements CustomerModelAccessor
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
      double usageSign = bundle.getPowerType().isConsumption() ? +1 : -1;

      // New code: shifted prediction - correct for tariff-eval purposes
      // ===============================================================
      double[] newForecast;
      // new code
      HashMap<CapacityOriginator, double[]> originator2usage = new HashMap<>();
      for (CapacityOriginator capacityOriginator : bundle.getCapacityOriginators()) {
        double[] usageForecast = new double[CapacityProfile.NUM_TIMESLOTS];
        // BUG FIX: this function is called from forecastCost() and used
        // by TariffEvaluationHelper, which assumes the forcast starts
        // at the next timeslot
        CapacityProfile forecast = capacityOriginator.getForecastForNextTimeslot();
        for (int i = 0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
          double hourlyUsage = usageSign * forecast.getCapacity(i);
          usageForecast[i] = hourlyUsage;// don't divide yet / bundle.getPopulation();
        }
        originator2usage.put(capacityOriginator, usageForecast);
      }

      // Refactored the following code for LearningUtilityOptimizer - shift profile
      //
      // create dummy subscription for the above usage vector:
      // 1 population member under 'tariff', (consuming the sum
      // of the originators' usage)
      TariffSubscription dummySubscription =
          new DummyTariffSubscription(getCustomerInfo(), tariff);
      newForecast = adjustForecastPerTariff(
          originator2usage, dummySubscription, bundle);

      double[] result = newForecast;
      Instant start =
          service.getTimeService().getCurrentTime().plus(TimeService.HOUR);
      return new org.powertac.common.CapacityProfile(result, start);
    }

    @Override
    public double getBrokerSwitchFactor (boolean isSuperseding)
    {
      double result = subStructure.getBrokerSwitchFactor();
      if (isSuperseding) {
        return result * 5.0;
      }
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
     * 
     * TODO - this is not correct for INDIVIDUAL originators, since they
     * subscribe individually to tariffs. See #956.
     */
    @Override
    public double getShiftingInconvenienceFactor (Tariff tariff)
    {
      double inconv = 0;
      for (CapacityOriginator capacityOriginator : bundle.getCapacityOriginators()) {
        inconv += capacityOriginator.getShiftingInconvenienceFactor(tariff);
      }
      return inconv;
    }

    @Override
    public void notifyCustomer (TariffSubscription oldsub,
                                TariffSubscription newsub, int population)
    {
      //updateSubscriptionSoC(oldsub, newsub, population); 
    }
  }

  /**
   * HACK, accessed from inner class, overriden from derived class
   * <p>
   * sum all originators' usage arrays
   *
   * @param originator2usage
   * @param dummySubscription
   * @param bundle
   * @return
   */
  public double[] adjustForecastPerTariff (
      HashMap<CapacityOriginator, double[]> originator2usage,
      TariffSubscription dummySubscription,
      CapacityBundle bundle)
  {
    // sum all originators' usage arrays
    double[] result = new double[originator2usage.values().iterator().next().length];
    for (double[] usage : originator2usage.values()) {
      for (int i = 0; i < result.length; ++i) {
        result[i] += usage[i] / bundle.getPopulation();
      }
    }
    return result;
  }

  private class DummyTariffSubscription extends TariffSubscription
  {
    public DummyTariffSubscription (CustomerInfo customer, Tariff tariff)
    {
      super(customer, tariff);
    }

    @Override
    public int getCustomersCommitted ()
    {
      return 1;
    }
  }

  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    // TODO Auto-generated method stub
    
  }
}


