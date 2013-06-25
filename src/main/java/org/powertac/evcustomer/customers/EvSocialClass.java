/*
 * Copyright 2013 the original author or authors.
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

package org.powertac.evcustomer.customers;

import org.apache.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.powertac.common.*;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ActivityDetail;
import org.powertac.evcustomer.beans.Car;
import org.powertac.evcustomer.beans.SocialGroup;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;


/**
 * TODO
 *
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.2, Date: 2013.05.08
 */
public class EvSocialClass extends AbstractCustomer
{

  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  private static Logger log = Logger.getLogger(EvSocialClass.class.getName());

  @Autowired
  private TimeService timeService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  public static double EPSILON = 2.7;
  public static double LAMDA = 20;
  public static double PERCENTAGE = 100;
  public static long MEAN_TARIFF_DURATION = 7;
  public static int HOURS_OF_DAY = 24;
  public static int DAYS_OF_BOOTSTRAP = 14;
  public static int DAYS_OF_COMPETITION = 0;

  private final TariffEvaluationHelper tariffEvalHelper =
      new TariffEvaluationHelper();

  private HashMap<String, TariffSubscription> subscriptionMap =
      new HashMap<String, TariffSubscription>();
  private HashMap<String, TariffSubscription> evSubscriptionMap =
      new HashMap<String, TariffSubscription>();

  private Vector<EvCustomer> evCustomers;
  
  protected final List<CapacityBundle> capacityBundles;
  protected List<CapacityBundle> bundlesWithRevokedTariffs = new ArrayList<CapacityBundle>();

  public EvSocialClass (String name) {
    super(name);

    timeslotRepo =
        (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    timeService = (TimeService) SpringApplicationContext.getBean("timeService");

    String[] typeList = new String[] { "NS" };
    for (String type: typeList) {
      subscriptionMap.put(type, null);
      evSubscriptionMap.put(type, null);
    }
  }

  /**
   * TODO
   */
  public void initialize (List<SocialGroup> groups,
                          Map<Integer, Activity> activities,
                          Map<Integer, Map<Integer, ActivityDetail>> allActivityDetails,
                          List<Car> cars,
                          int count,
                          double[] groupProbilities,
                          double[] maleProbilities,
                          Random generator)
  {
    evCustomers = new Vector<EvCustomer>();

    for (int i = 0; i < count; i++) {
      int randomGroupId = getRandomGroupId(groupProbilities, generator);
      SocialGroup group = groups.get(randomGroupId);
      Map<Integer, ActivityDetail> details = allActivityDetails.get(group.getId());
      String gender = "female";
      if (generator.nextDouble() <  maleProbilities[randomGroupId]) {
        gender = "male";
      }
      // For now, all cars have equal probability
      Car car = cars.get(generator.nextInt(cars.size()));

      EvCustomer evCustomer = new EvCustomer();
      evCustomer.initialize(group, gender, activities, details, car, generator);
      evCustomers.add(evCustomer);
    }
  }

  private int getRandomGroupId (double[] probabilities, Random gen)
  {
    int[] newProbs = new int[probabilities.length];
    int sum = 0;
    for (int i = 0; i < probabilities.length; i++) {
      newProbs[i] = (int) (1000 * probabilities[i]);
      sum += newProbs[i];
    }

    int tmp = gen.nextInt(sum);

    int j = -1;
    while (tmp >= 0) {
      tmp -= newProbs[++j];
    }
    return j;
  }

  // =====EVALUATION FUNCTIONS===== //

  /**
   * The better tariffs have more chance to be chosen.
   * Run every 6 timeslots == 6 hours
   */
  public void possibilityEvaluationNewTariffs (List<Tariff> newTariffs,
          String type)
  {
	  double wtExpected = 0.6;
	  double wtMax = 0.4;
	  double wtRealized = 0.8;
	  double soldThreshold = 10000.0;
  
	  TariffEvaluationHelper.init();
	  TariffEvaluationHelper.initializeCostFactors(double wtExpected, double wtMax,
              double wtRealized, double soldThreshold));
              
              if (newTariffs.size() <= 1) {
                  return;
                }
              
              for (Tariff tariff: newTariffs) {
            	  estimateCost (Tariff tariff, double[] usage) // usage must be changed to the getDominantLoad() or corresponding function that gives the consumption
            	  estimateCostArray (Tariff tariff, double[] usage)

              }
             double preferredTariff= Collections.max(Arrays.asList(estimateCostArray); //Gives the tarrif with the lowest cost. Now the customer must subscribe to that
             
             //TODO: Subscribe to the cheapest tarrif (i.e preferreddTariff)
             
  }
 /* public void possibilityEvaluationNewTariffs (List<Tariff> newTariffs,
                                               String type)
  {
    // TODO Implement changes in household-customer.Village

    for (CustomerInfo customer: customerInfos) {
      List<TariffSubscription> subscriptions =
          tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer);

      if (subscriptions == null || subscriptions.size() == 0) {
        subscribeDefault();
        return;
      }

      log.debug("Estimation size for " + this.toString() + " = "
          + newTariffs.size());

      if (newTariffs.size() <= 1) {
        return;
      }

      Vector<Double> estimation = new Vector<Double>();
      for (Tariff tariff: newTariffs) {
        log.debug("Tariff : " + tariff.toString() + " Tariff Type : "
            + tariff.getTariffSpecification().getPowerType()
            + " Broker: " + tariff.getBroker().toString());

//        boolean case1 = customer.getPowerType() ==
//            tariff.getTariffSpecification().getPowerType();
//        boolean case2 = (
//            customer.getPowerType() == PowerType.ELECTRIC_VEHICLE &&
//            tariff.getTariffSpecification().getPowerType().isConsumption());

        //if (!tariff.isExpired() && (case1 || case2) ) {
        estimation.add(-costEstimation(tariff, type));
        //}
        //else {
        //  estimation.add(Double.NEGATIVE_INFINITY);
        //}
      }

      int minIndex = logitPossibilityEstimation(estimation);

      TariffSubscription sub = subscriptionMap.get(type);
      if (customer.getPowerType() == PowerType.ELECTRIC_VEHICLE) {
        sub = evSubscriptionMap.get(type);
      }

      log.debug("Equality: "
          + sub.getTariff().getTariffSpec().toString() + " = "
          + newTariffs.get(minIndex).getTariffSpec().toString());

      if (sub.getTariff().getTariffSpec() !=
          newTariffs.get(minIndex).getTariffSpec()) {
        log.debug("Changing From " + sub.toString() + " For PowerType "
            + customer.getPowerType() + " After Evaluation");

        changeSubscription(
            sub.getTariff(), newTariffs.get(minIndex), type, customer);
      }
    }
  }
*/
  @StateChange
  protected void subscribe(Tariff tariff, CapacityBundle bundle, int customerCount, boolean verbose)
  {
    tariffMarketService.subscribeToTariff(tariff, bundle.getCustomerInfo(), customerCount);
    if (verbose) log.info(bundle.getName() + ": Subscribed " + customerCount + " customers to tariff " + tariff.getId() + " successfully");
  }

  @StateChange
  protected void unsubscribe(TariffSubscription subscription, CapacityBundle bundle, int customerCount, boolean verbose)
  {
    subscription.unsubscribe(customerCount);
    if (verbose) log.info(bundle.getName() + ": Unsubscribed " + customerCount + " customers from tariff " + subscription.getTariff().getId() + " successfully");
  }
 /* private void changeSubscription (Tariff tariff, Tariff newTariff, String type,
                                  CustomerInfo customer)
  {
    TariffSubscription ts =
        tariffSubscriptionRepo.getSubscription(customer, tariff);
    int populationCount = ts.getCustomersCommitted();
    unsubscribe(ts, populationCount);
    subscribe(newTariff, populationCount, customer);

    updateSubscriptions(tariff, newTariff, type, customer);
  }*/

  private void updateSubscriptions (Tariff tariff, Tariff newTariff,
                                    String type, CustomerInfo customer)
  {
    if (type.equals("NS")) {
      return;
    }

    TariffSubscription ts =
        tariffSubscriptionRepo.getSubscription(customer, tariff);
    TariffSubscription newTs =
        tariffSubscriptionRepo.getSubscription(customer, newTariff);

    log.debug(this.toString() + " Changing Only " + type);
    log.debug("Old:" + ts.toString() + "  New:" + newTs.toString());

    if (customer.getPowerType() == PowerType.ELECTRIC_VEHICLE) {
      log.debug("For " + customer.getPowerType().toString());
      log.debug("Controllable Subscription Map: " + evSubscriptionMap.toString());
      evSubscriptionMap.put("NS", newTs);
    }
    else {
      log.debug("Subscription Map: " + subscriptionMap.toString());
      subscriptionMap.put("NS", newTs);
    }
  }

  /**
   * This function estimates the overall cost, taking into consideration the
   * fixed payments as well as the variable that are dependent on the tariff
   * rates
   */
  private double costEstimation (Tariff tariff, String type)
  {
    // all the customers with EVs are non shifting with respect to EV charging.
    // They charge whenever they are available and need charged battery

    double costVariable = estimateVariableTariffPayment(tariff, type);
    double costFixed = estimateFixedTariffPayments(tariff) * evCustomers.size();

    log.debug("Simple Evaluation for " + type);
    log.debug("Cost Variable: " + costVariable + " Cost Fixed: " + costFixed);

    return (costVariable + costFixed) / 1000000;
  }

  /**
   * This function estimates the variable cost, depending only to the load
   * quantity you consume
   */
  private double estimateVariableTariffPayment (Tariff tariff, String type)
  {
    // EVs are non shifting
    double[] dominantUsage = new double[HOURS_OF_DAY];
    if (type.equals("NS")) {
      for (EvCustomer evCustomer: evCustomers) {
        for (int i=0; i<dominantUsage.length; i++) {
          dominantUsage[i] += evCustomer.getDominantLoad() / HOURS_OF_DAY;
        }
      }
    }

    // TODO dominantUsage should be for one customer only
    for (int i=0; i<dominantUsage.length; i++) {
      dominantUsage[i] /= evCustomers.size();
    }

    double dominantCostSummary = tariffEvalHelper.estimateCost(tariff, dominantUsage);
    log.debug("Dominant Cost Summary: " + dominantCostSummary);

    return -dominantCostSummary;
  }

  /**
   * This function estimates the fixed cost, comprised by fees, bonuses and
   * penalties that are the same no matter how much you consume
   */
  private double estimateFixedTariffPayments (Tariff tariff)
  {
    double lifecyclePayment =
        -tariff.getEarlyWithdrawPayment() - tariff.getSignupPayment();

    // When there is not a Minimum Duration of the contract, you cannot divide
    // with the duration because you don't know it.
    double minDuration;
    if (tariff.getMinDuration() == 0) {
      minDuration = MEAN_TARIFF_DURATION;
    }
    else {
      minDuration = (double) tariff.getMinDuration() / TimeService.DAY;
    }

    log.debug("Minimum Duration: " + minDuration);
    return lifecyclePayment / minDuration;
  }

  /**
   * This is the function that realizes the mathematical possibility formula
   * for the choice of tariff.
   */
  private int logitPossibilityEstimation (Vector<Double> estimation)
  {
    double summedEstimations = 0;
    Vector<Integer> randomizer = new Vector<Integer>();
    Vector<Integer> possibilities = new Vector<Integer>();

    for (int i = 0; i < estimation.size(); i++) {
      log.debug("Estimation for " + i + ": " + estimation.get(i));
      summedEstimations += Math.pow(EPSILON, LAMDA * estimation.get(i));
      log.debug("Cost variable: " + 50 * estimation.get(i));
      log.debug("Summary of Estimation: " + summedEstimations);
    }

    for (int i = 0; i < estimation.size(); i++) {
      possibilities
          .add((int) (PERCENTAGE * (Math
              .pow(EPSILON, LAMDA * estimation.get(i)) / summedEstimations)));
      for (int j = 0; j < possibilities.get(i); j++) {
        randomizer.add(i);
      }
    }

    int index = randomizer.get((int) (randomizer.size() * rs1.nextDouble()));
    log.debug("Randomizer Vector: " + randomizer);
    log.debug("Possibility Vector: " + possibilities.toString());
    log.debug("Resulting Index = " + index);
    return index;
  }

  /*@Override
  public void subscribeDefault ()
  {
    super.subscribeDefault();

    for (CustomerInfo customer: customerInfos) {
      if (customer.getPowerType() == PowerType.ELECTRIC_VEHICLE &&
          tariffMarketService
              .getDefaultTariff(PowerType.ELECTRIC_VEHICLE) == null) {

        log.debug("No Default Tariff for ELECTRIC_VEHICLE so the customer "
            + customer.toString()
            + " subscribe to CONSUMPTION Default Tariff instead");
        tariffMarketService.subscribeToTariff(tariffMarketService
            .getDefaultTariff(PowerType.CONSUMPTION), customer, customer
            .getPopulation());
        log.info("CustomerInfo of type ELECTRIC_VEHICLE of " + toString()
            + " was subscribed to the default CONSUMPTION tariff successfully.");
      }

      List<TariffSubscription> subscriptions =
          tariffSubscriptionRepo.findSubscriptionsForCustomer(customer);

      if (subscriptions.size() > 0) {
        log.info(subscriptions.toString());

        for (String type: subscriptionMap.keySet()) {
          if (customer.getPowerType() == PowerType.CONSUMPTION) {
            subscriptionMap.put(type, subscriptions.get(0));
          }
          if (customer.getPowerType() == PowerType.ELECTRIC_VEHICLE) {
            evSubscriptionMap.put(type, subscriptions.get(0));
          }
        }
      }
    }

    log.info("Consume Subscriptions:" + subscriptionMap.toString());
    log.info("Storage Subscriptions:" + evSubscriptionMap.toString());
  }
*/
  
  /** @Override hook **/
  protected void subscribeDefault()
  {
      for (CapacityBundle bundle: capacityBundles) {
          PowerType powerType = bundle.getPowerType();
          if (tariffMarketService.getDefaultTariff(powerType) != null) {
              log.info(bundle.getName() + ": Subscribing " + bundle.getPopulation() + " customers to default " + powerType + " tariff");
              subscribe(tariffMarketService.getDefaultTariff(powerType), bundle, bundle.getPopulation(), false);
          } else {
              log.info(bundle.getName() + ": No default tariff for power type " + powerType + "; trying generic type");
              PowerType genericType = powerType.getGenericType();
              if (tariffMarketService.getDefaultTariff(genericType) == null) {
                  log.error(bundle.getName() + ": No default tariff for generic power type " + genericType + " either!");
              } else {
                  log.info(bundle.getName() + ": Subscribing " + bundle.getPopulation() + " customers to default " + genericType + " tariff");
                  subscribe(tariffMarketService.getDefaultTariff(genericType), bundle, bundle.getPopulation(), false);
              }
          }
      }
  }
  
  @Override
  public void handleNewTariffs (List<Tariff> newTariffs)
  {
      ++tariffEvaluationCounter;
      for (Tariff tariff: newTariffs) {
          allTariffs.add(tariff);
      }
      for (CapacityBundle bundle: capacityBundles) {
          evaluateTariffs(bundle, newTariffs);
      }
      bundlesWithRevokedTariffs.clear();
  }
  
  private boolean isTariffApplicable(Tariff tariff, CapacityBundle bundle)
  {
      PowerType bundlePowerType = bundle.getCustomerInfo().getPowerType();
      if (tariff.getPowerType() == bundlePowerType ||
          tariff.getPowerType() == bundlePowerType.getGenericType()) {
          return true;
      }
      return false;
  }
  
  
  
  
  private void evaluateTariffs(CapacityBundle bundle, List<Tariff> newTariffs)
  {
      if ((tariffEvaluationCounter % bundle.getSubscriberStructure().reconsiderationPeriod) == 0
           || bundlesWithRevokedTariffs.contains(bundle)) {
          reevaluateAllTariffs(bundle);
      }
      else if (! ignoredTariffs.isEmpty()) {
          evaluateCurrentTariffs(bundle, newTariffs);
      }
      else if (! newTariffs.isEmpty()) {
          boolean ignoringNewTariffs = true;
          for (Tariff tariff: newTariffs) {
              if (isTariffApplicable(tariff, bundle)) {
                  ignoringNewTariffs = false;
                  evaluateCurrentTariffs(bundle, newTariffs);
                  break;
              }
          }
          if (ignoringNewTariffs) log.info(bundle.getName() + ": New tariffs are not applicable; skipping evaluation");
      }
  }
  
  private void reevaluateAllTariffs(CapacityBundle bundle)
  {
      log.info(bundle.getName() + ": Reevaluating all tariffs for " + bundle.getPowerType() + " subscriptions");
      
      List<Tariff> evalTariffs = new ArrayList<Tariff>();
      for (Tariff tariff: allTariffs) {
          if (! tariff.isRevoked() && ! tariff.isExpired() && isTariffApplicable(tariff, bundle)) {
              evalTariffs.add(tariff);
          }
      }
      assertNotEmpty(bundle, evalTariffs);
      ignoredTariffs.clear();
      manageSubscriptions(bundle, evalTariffs);
  }
  
  
  private void evaluateCurrentTariffs(CapacityBundle bundle, List<Tariff> newTariffs)
  {
      if (bundle.getSubscriberStructure().inertiaDistribution != null) {
          double inertia = bundle.getSubscriberStructure().inertiaDistribution.drawSample();
          if (inertiaSampler.nextDouble() < inertia) {
              log.info(bundle.getName() + ": Skipping " + bundle.getCustomerInfo().getPowerType() + " tariff reevaluation due to inertia");
              for (Tariff newTariff: newTariffs) {
                  ignoredTariffs.add(newTariff);
              }
              return;
          }
      }
      // Include previously ignored tariffs, currently subscribed tariffs, and default
      // tariff in evaluation. Use map instead of list to eliminate duplicate tariffs.
      Map<Long, Tariff> currTariffs = new HashMap<Long, Tariff>();
for (Tariff ignoredTariff: ignoredTariffs) {
currTariffs.put(ignoredTariff.getId(), ignoredTariff);
}
ignoredTariffs.clear();	
List<TariffSubscription> subscriptions = tariffSubscriptionRepo.findSubscriptionsForCustomer(bundle.getCustomerInfo());
      for (TariffSubscription subscription: subscriptions) {
          Tariff subTariff = subscription.getTariff();
          if (!(subTariff.isExpired() || subTariff.isExpired())) {
              currTariffs.put(subTariff.getId(), subTariff);
          }
      }
      for (Tariff newTariff: newTariffs) {
          currTariffs.put(newTariff.getId(), newTariff);
      }
      Tariff defaultTariff = getDefaultTariff(bundle);
      if (! currTariffs.containsKey(defaultTariff.getId())) {
          currTariffs.put(defaultTariff.getId(), defaultTariff);
      }

      List<Tariff> evalTariffs = new ArrayList<Tariff>();
      for (Tariff tariff: currTariffs.values()) {
          if (isTariffApplicable(tariff, bundle)) {
              evalTariffs.add(tariff);
          }
      }
      assertNotEmpty(bundle, evalTariffs);
      manageSubscriptions(bundle, evalTariffs);	
  }
  
  private void assertNotEmpty(CapacityBundle bundle, List<Tariff> evalTariffs)
  {
      if (evalTariffs.isEmpty()) {
          throw new Error(bundle.getName() + ": The evaluation tariffs list is unexpectedly empty!");
      }
  }
  
  private void manageSubscriptions(CapacityBundle bundle, List<Tariff> evalTariffs)
  {
      Collections.shuffle(evalTariffs);
      
      PowerType powerType = bundle.getCustomerInfo().getPowerType();
      List<Long> tariffIds = new ArrayList<Long>(evalTariffs.size());
      for (Tariff tariff: evalTariffs) tariffIds.add(tariff.getId());
      logAllocationDetails(bundle.getName() + ": " + powerType + " tariffs for evaluation: " + tariffIds);

List<Double> estimatedPayments = estimatePayments(bundle, evalTariffs);
logAllocationDetails(bundle.getName() + ": Estimated payments for tariffs: " + estimatedPayments);

List<Integer> allocations = determineAllocations(bundle, evalTariffs, estimatedPayments);
logAllocationDetails(bundle.getName() + ": Allocations for tariffs: " + allocations);

int overAllocations = 0;
for (int i=0; i < evalTariffs.size(); ++i) {
Tariff evalTariff = evalTariffs.get(i);
int allocation = allocations.get(i);
TariffSubscription subscription = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(evalTariff, bundle.getCustomerInfo()); // could be null
int currentCommitted = (subscription != null) ? subscription.getCustomersCommitted() : 0;
int numChange = allocation - currentCommitted;

log.debug(bundle.getName() + ": evalTariff = " + evalTariff.getId() + ", numChange = " + numChange +
", currentCommitted = " + currentCommitted + ", allocation = " + allocation);

if (numChange == 0) {
if (currentCommitted > 0) {
log.info(bundle.getName() + ": Maintaining " + currentCommitted + " " + powerType + " customers in tariff " + evalTariff.getId());
} else {
log.info(bundle.getName() + ": Not allocating any " + powerType + " customers to tariff " + evalTariff.getId());
}
} else if (numChange > 0) {
if (evalTariff.isExpired()) {
overAllocations += numChange;
if (currentCommitted > 0) {
log.info(bundle.getName() + ": Maintaining " + currentCommitted + " " + powerType + " customers in expired tariff " + evalTariff.getId());
}
log.info(bundle.getName() + ": Reallocating " + numChange + " " + powerType + " customers from expired tariff " + evalTariff.getId() + " to other tariffs");
} else {
log.info(bundle.getName() + ": Subscribing " + numChange + " " + powerType + " customers to tariff " + evalTariff.getId());
subscribe(evalTariff, bundle, numChange, false);
}
} else if (numChange < 0) {
log.info(bundle.getName() + ": Unsubscribing " + -numChange + " " + powerType + " customers from tariff " + evalTariff.getId());
unsubscribe(subscription, bundle, -numChange, false);
}
}
if (overAllocations > 0) {
int minIndex = 0;
double minEstimate = Double.POSITIVE_INFINITY;
for (int i=0; i < estimatedPayments.size(); ++i) {
if (estimatedPayments.get(i) < minEstimate && ! evalTariffs.get(i).isExpired()) {
minIndex = i;
minEstimate = estimatedPayments.get(i);
}
}
log.info(bundle.getName() + ": Subscribing " + overAllocations + " over-allocated customers to tariff " + evalTariffs.get(minIndex).getId());
subscribe(evalTariffs.get(minIndex), bundle, overAllocations, false);
}
  }
  
  private List<Double> estimatePayments(CapacityBundle bundle, List<Tariff> evalTariffs)
  {
      List<Double> estimatedPayments = new ArrayList<Double>(evalTariffs.size());
      for (int i=0; i < evalTariffs.size(); ++i) {
          Tariff tariff = evalTariffs.get(i);
          double fixedPayments = estimateFixedTariffPayments(bundle, tariff);
          double variablePayment = forecastDailyUsageCharge(bundle, tariff);
          double totalPayment = truncateTo2Decimals(fixedPayments + variablePayment);
          double adjustedPayment = adjustForInterruptibility(bundle, tariff, totalPayment);
          estimatedPayments.add(adjustedPayment);
      }
      return estimatedPayments;
  }
  
  private double estimateFixedTariffPayments(CapacityBundle bundle, Tariff tariff)
  {
      double lifecyclePayment = tariff.getEarlyWithdrawPayment() + tariff.getSignupPayment();
      double minDuration;
      if (tariff.getMinDuration() == 0l) minDuration = (double)MEAN_TARIFF_DURATION;
      else minDuration = (double)tariff.getMinDuration() / (double)TimeService.DAY;
      double dailyLifecyclePayment = lifecyclePayment / minDuration;
      //double dailyPeriodicPayment = tariff.getPeriodicPayment() * NUM_HOURS_IN_DAY;
      //return bundle.getPopulation() * (dailyLifecyclePayment + dailyPeriodicPayment);
      return bundle.getPopulation() * (dailyLifecyclePayment);
         }

  private double forecastDailyUsageCharge(CapacityBundle bundle, Tariff tariff)
  {
      double usageSign = bundle.getPowerType().isConsumption() ? +1 : -1;
      double[] usageForecast = new double[CapacityProfile.NUM_TIMESLOTS];
      for (CapacityOriginator capacityOriginator: bundle.getCapacityOriginators()) {
          CapacityProfile forecast = capacityOriginator.getCurrentForecast();
          for (int i=0; i < CapacityProfile.NUM_TIMESLOTS; ++i) {
              double hourlyUsage = usageSign * forecast.getCapacity(i);
              usageForecast[i] += hourlyUsage;
          }
      }
      TariffSubscriberStructure subStructure = bundle.getSubscriberStructure();
      tariffEvalHelper.init(subStructure.expMeanPriceWeight, subStructure.maxValuePriceWeight,
       subStructure.realizedPriceWeight, tariffEvalHelper.getSoldThreshold());
      return tariffEvalHelper.estimateCost(tariff, usageForecast);
  }
  
  private double adjustForInterruptibility(CapacityBundle bundle, Tariff tariff, double totalPayment)
  {
      double interruptibilityDiscount = bundle.getSubscriberStructure().interruptibilityDiscount;
      if (interruptibilityDiscount > 0.0 && tariff.getPowerType().isInterruptible()) {
          double effectSign = tariff.getPowerType().isConsumption() ? -1 : +1;
          double adjustedPayment = (1.0 + effectSign * interruptibilityDiscount) * totalPayment;
          return adjustedPayment;
      } else {
          return totalPayment;
      }
  }

  private List<Integer> determineAllocations(CapacityBundle bundle, List<Tariff> evalTariffs,
                                             List<Double> estimatedPayments)
  {
      List<Integer> allocations = new ArrayList<Integer>();
      if (evalTariffs.size() == 1) {
          allocations.add(bundle.getPopulation());
          return allocations;
      }
      List<Boolean> validTariffsIndex = enforceTariffConstraints(bundle, evalTariffs, estimatedPayments);
      List<Tariff> checkedTariffs = new ArrayList<Tariff>();
      List<Double> checkedPayments = new ArrayList<Double>();
      for (int i=0; i < evalTariffs.size(); ++i) {
          if (validTariffsIndex.get(i)) {
              checkedTariffs.add(evalTariffs.get(i));
              checkedPayments.add(estimatedPayments.get(i));
          }
      }
      List<Integer> checkedAllocs = bundle.getSubscriberStructure().allocationMethod == AllocationMethod.TOTAL_ORDER ?
          determineTotalOrderAllocations(bundle, checkedTariffs, checkedPayments) :
          determineLogitChoiceAllocations(bundle, checkedTariffs, checkedPayments);
      int checkedCounter = 0;
      for (int i=0; i < evalTariffs.size(); ++i) {
          allocations.add(validTariffsIndex.get(i) ? checkedAllocs.get(checkedCounter++) : 0);
      }
      return allocations;
  }
  
  private List<Boolean> enforceTariffConstraints(CapacityBundle bundle, List<Tariff> evalTariffs,
                                                 List<Double> estimatedPayments)
 {
      List<Boolean> validityIndex = new ArrayList<Boolean>();
      
      Tariff defaultTariff = getDefaultTariff(bundle);
      if (defaultTariff == null) throw new Error("Default tariff not found amongst eval tariffs!");
      double defaultPayment = estimatedPayments.get(evalTariffs.indexOf(defaultTariff));
      
      boolean benchmarkRiskEnabled = bundle.getSubscriberStructure().benchmarkRiskEnabled;
      boolean tariffThrottlingEnabled = bundle.getSubscriberStructure().tariffThrottlingEnabled;
      
      Map<Long, Integer> brokerBests = new HashMap<Long, Integer>(); // brokerId -> tariffIndex
      
      for (int i=0; i < evalTariffs.size(); ++i) {
          Tariff evalTariff = evalTariffs.get(i);
          // #1: Default tariff is always valid.
          if (evalTariff == defaultTariff) {
              validityIndex.add(true); continue;
          }
          // #2: If tariff is expired, don't consider it.
          if (evalTariff.isExpired() || evalTariff.isRevoked()) {
              log.info("Tariff " + evalTariff.getId() + " has expired or been revoked; being ignored.");
              validityIndex.add(false); continue;
          }
          // #3: Tariff payments cannot be too much worse than those of default tariff.
          if (benchmarkRiskEnabled) {
              double evalRatio = estimatedPayments.get(i) / defaultPayment;
              if ((bundle.getPowerType().isConsumption() && evalRatio > bundle.getSubscriberStructure().benchmarkRiskRatio) ||
                  (bundle.getPowerType().isProduction() && evalRatio < bundle.getSubscriberStructure().benchmarkRiskRatio)) {
                  logAllocationDetails(bundle.getName() + ": Tariff " + evalTariff.getId() + " has a worse than constrained benchmark risk at: " + evalRatio);
                  validityIndex.add(false); continue;
              }
          }
          // #4: Only include best N tariffs per broker.
          if (tariffThrottlingEnabled) {
              Long brokerId = evalTariff.getBroker().getId();
              if (! brokerBests.containsKey(brokerId)) {
                  brokerBests.put(brokerId, i);
              } else {
                  TariffSubscription evalSub = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(evalTariff, bundle.getCustomerInfo());
                  if (evalSub == null || evalSub.getCustomersCommitted() == 0) {
                      double evalPayment = estimatedPayments.get(i);
                      double bestPayment = estimatedPayments.get(brokerBests.get(brokerId));
                      if ((bundle.getPowerType().isConsumption() && evalPayment <= bestPayment) ||
                          (bundle.getPowerType().isProduction() && evalPayment >= bestPayment)) {
                          logAllocationDetails(bundle.getName() + ": Tariff " + evalTariff.getId() + " is no better than "
                                  + evalTariffs.get(brokerBests.get(brokerId)).getId());
                          validityIndex.add(false); continue;
                      } else {
                          // reevaluate previous brokerBest
                          TariffSubscription sub = tariffSubscriptionRepo.findSubscriptionForTariffAndCustomer(evalTariff, bundle.getCustomerInfo());
                          if (sub == null || sub.getCustomersCommitted() == 0) {
                              validityIndex.set(brokerBests.get(brokerId), false);
                              brokerBests.put(brokerId, i);
                          }
                      }
                  }
              }
          }
          validityIndex.add(true);
      }
      logAllocationDetails(bundle.getName() + ": Tariff constraint validation index: " + validityIndex);
      return validityIndex;
 }
  
  private List<Integer> determineTotalOrderAllocations(CapacityBundle bundle, List<Tariff> checkedTariffs,
                                                       List<Double> estimatedPayments)
  {
      int numTariffs = checkedTariffs.size();
      List<Double> allocationRule;
      if (bundle.getSubscriberStructure().totalOrderRules.isEmpty()) {
          allocationRule = new ArrayList<Double>(numTariffs);
          allocationRule.add(1.0);
          for (int i=1; i < numTariffs; ++i) {
              allocationRule.add(0.0);
          }
      } else if (numTariffs <= bundle.getSubscriberStructure().totalOrderRules.size()) {
          allocationRule = bundle.getSubscriberStructure().totalOrderRules.get(numTariffs - 1);
      } else {
          allocationRule = new ArrayList<Double>(numTariffs);
          List<Double> largestRule = bundle.getSubscriberStructure().totalOrderRules.get(bundle.getSubscriberStructure().totalOrderRules.size() - 1);
          for (int i=0; i < numTariffs; ++i) {
              if (i < largestRule.size()) {
                  allocationRule.add(largestRule.get(i));
              } else {
                  allocationRule.add(0.0);
              }
          }
      }
      // payments are positive for production, so sorting is still valid
      List<Double> sortedPayments = new ArrayList<Double>(numTariffs);
      for (double estimatedPayment: estimatedPayments) {
          sortedPayments.add(estimatedPayment);
      }
      Collections.sort(sortedPayments, Collections.reverseOrder()); // we want descending order

      List<Integer> allocations = new ArrayList<Integer>(numTariffs);
      for (int i=0; i < numTariffs; ++i) {
          if (allocationRule.get(i) > 0) {
              double nextBest = sortedPayments.get(i);
              for (int j=0; j < numTariffs; ++j) {
                  if (estimatedPayments.get(j) == nextBest) {
                      allocations.add((int) Math.round(bundle.getCustomerInfo().getPopulation() * allocationRule.get(i)));
                  }
              }
          }	
          else allocations.add(0);
      }
      return allocations;
  }
  
  private List<Integer> determineLogitChoiceAllocations(CapacityBundle bundle, List<Tariff> checkedTariffs,
                                                        List<Double> estimatedPayments)
  {
      // logit choice model: p_i = e^(lambda * utility_i) / sum_i(e^(lambda * utility_i))

      int numTariffs = checkedTariffs.size();
      double bestPayment = Collections.max(estimatedPayments);
      double worstPayment = Collections.min(estimatedPayments);
      
      List<Double> probabilities = new ArrayList<Double>(numTariffs);
      if (bestPayment - worstPayment < 0.01) { // i.e., approximately zero
          for (int i=0; i < numTariffs; ++i) {
              probabilities.add(1.0 / numTariffs);
          }
      } else {
          double midPayment = (worstPayment + bestPayment) / 2.0;
          double basis = Math.max((bestPayment - midPayment), (midPayment - worstPayment));

          double absMin = Double.MAX_VALUE;
          double absMax = Double.MIN_VALUE;
          for (int i=0; i < numTariffs; ++i) {
              double absPayment = Math.abs(estimatedPayments.get(i));
              absMin = Math.min(absPayment, absMin);
              absMax = Math.max(absPayment, absMax);
          }
          double kappa = Math.min(10.0, Math.max(1.0, absMax / absMin)); // utility curve shape factor
          double lambda = bundle.getSubscriberStructure().logitChoiceRationality; // [0.0 = irrational, 1.0 = perfectly rational]

          List<Double> numerators = new ArrayList<Double>(numTariffs);
          double denominator = 0.0;
          for (int i=0; i < numTariffs; ++i) {
              double utility = ((estimatedPayments.get(i) - midPayment) / basis) * kappa; // [-kappa, +kappa]
              //System.out.println("***** utility for tariff[" + i + "] = " + utility);
              double numerator = Math.exp(lambda * utility);
              if (Double.isNaN(numerator)) numerator = 0.0;
              numerators.add(numerator);
              denominator += numerator;
          }
          for (int i=0; i < numTariffs; ++i) {
              probabilities.add(numerators.get(i) / denominator);
          }
          //System.out.println("***** allocation probabilities: " + probabilities);
      }
      // Now determine allocations based on above probabilities
      List<Integer> allocations = new ArrayList<Integer>(numTariffs);
      int population = bundle.getPopulation();
      if (bundle.getCustomerInfo().isMultiContracting())
      {
          int sumAllocations = 0;
          for (int i=0; i < numTariffs; ++i) {
              int allocation;
              if (sumAllocations == population) {
                  allocation = 0;
              } else if (i < (numTariffs - 1)) {
                  allocation = (int) Math.round(population * probabilities.get(i));
                  if ((sumAllocations + allocation) > population) {
                      allocation = population - sumAllocations;
                  }
                  sumAllocations += allocation;
              } else {
                  allocation = population - sumAllocations;
              }
              allocations.add(allocation);
          }
      } else {
          double r = ((double) tariffSelector.nextInt(100) / 100.0); // [0.0, 1.0]
          double cumProbability = 0.0;
          for (int i=0; i < numTariffs; ++i) {
              cumProbability += probabilities.get(i);
              if (r <= cumProbability) {
                  allocations.add(population);
                  for (int j=i+1; j < numTariffs; ++j) {
                      allocations.add(0);
                  }
                  break;
              } else {
                  allocations.add(0);
              }
          }
      }
      return allocations;
  }

  private Tariff getDefaultTariff(CapacityBundle bundle)
  {
      Tariff defaultTariff;
      defaultTariff = tariffMarketService.getDefaultTariff(bundle.getPowerType());
      if (defaultTariff == null) {
          defaultTariff = tariffMarketService.getDefaultTariff(bundle.getPowerType().getGenericType());
      }
      if (defaultTariff == null) {
          throw new Error(bundle.getName() + ": There is no default tariff for bundle type or it's generic type!");
      }
      return defaultTariff;
  
  
  
  
  
  
  
  
  @Override
  public void step ()
  {
    long millis = timeService.getCurrentTime().getMillis();
    int hour = timeService.getHourOfDay();
    int dayOfWeek = new Instant(millis).get(DateTimeFieldType.dayOfWeek());

    // TODO Replace with household-customer or factored-customer version?
    checkRevokedSubscriptions();

    doActivities(dayOfWeek, hour);

    consumePower();
  }

  @Override
  public void consumePower ()
  {
    int serial;
    Timeslot ts = timeslotRepo.currentTimeslot();
    if (ts == null) {
      log.debug("Current timeslot is null");
      serial = (int)
          ((timeService.getCurrentTime().getMillis() - timeService.getBase())
              / TimeService.HOUR);
    }
    else {
      log.debug("Timeslot Serial: " + ts.getSerialNumber());
      serial = ts.getSerialNumber();
    }

    HashMap<TariffSubscription, Double> subs =
        new HashMap<TariffSubscription, Double>();
    for (TariffSubscription sub: subscriptionMap.values()) {
      subs.put(sub, 0.0);
    }

    for (TariffSubscription sub: evSubscriptionMap.values()) {
      subs.put(sub, getConsumptionByTimeslot(serial));
    }

    for (TariffSubscription sub: subs.keySet()) {
      if (sub == null) {
        continue;
      }

      double usage = subs.get(sub);

      log.debug("Consumption Load for Customer " + sub.getCustomer().toString()
          + ": \n" + subs.get(sub));

      if (sub.getCustomersCommitted() > 0) {
        sub.usePower(usage);
      }
    }
  }

  /*
   * TODO
   */
  private double getConsumptionByTimeslot (int serial)
  {
    int hour = serial % HOURS_OF_DAY;
    long millis = timeService.getCurrentTime().getMillis();
    int dayOfWeek = new Instant(millis).get(DateTimeFieldType.dayOfWeek());

    double totalConsumption = 0.0;
    for (EvCustomer evCustomer: evCustomers) {
      totalConsumption += evCustomer.charge(dayOfWeek, hour);
    }

    return totalConsumption;
  }

  /*
   * TODO
   */
  private void doActivities (int day, int hour)
  {
    for (EvCustomer evCustomer: evCustomers) {
      evCustomer.doActivities(day, hour);
    }
  }

  @Override
  public String toString ()
  {
    return name;
  }

  public HashMap<String, TariffSubscription> getEvSubscriptionMap ()
  {
    return evSubscriptionMap;
  }

  /* Used for testing */
  public HashMap<String, TariffSubscription> getSubscriptionMap ()
  {
    return subscriptionMap;
  }

  public Vector<EvCustomer> getEvCustomers ()
  {
    return evCustomers;
  }
}
