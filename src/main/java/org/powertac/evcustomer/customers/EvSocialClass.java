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
import org.powertac.common.*;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.evcustomer.Config;
import org.powertac.evcustomer.beans.*;

import java.util.*;


/**
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.5, Date: 2013.11.25
 */
public class EvSocialClass extends AbstractCustomer
{
  private static Logger log = Logger.getLogger(EvSocialClass.class.getName());

  private TimeService timeService;

  protected RandomSeed generator;

  protected Map<CustomerInfo, TariffEvaluator> tariffEvaluators;

  protected Vector<EvCustomer> evCustomers;

  public EvSocialClass (String name, TimeService timeService)
  {
    super(name);

    this.timeService = timeService;

    Comparator<CustomerInfo> comp = new Comparator<CustomerInfo>()
    {
      public int compare (CustomerInfo customer1, CustomerInfo customer2)
      {
        return customer1.getName().compareToIgnoreCase(customer2.getName());
      }
    };
    tariffEvaluators = new TreeMap<CustomerInfo, TariffEvaluator>(comp);
  }

  public void initialize (Map<Integer, SocialGroup> groups,
                          Map<Integer, SocialGroupDetail> groupDetails,
                          Map<Integer, Activity> activities,
                          Map<Integer, Map<Integer, ActivityDetail>> allActivityDetails,
                          List<Car> cars,
                          int populationCount,
                          int seed)
  {
    this.generator = randomSeedRepo.
        getRandomSeed("EvSocialClass", seed, "initialize");

    evCustomers = new Vector<EvCustomer>();

    List<CustomerInfo> customerInfos1 = customerRepo.findByName(
        createInfoName(name, PowerType.CONSUMPTION));
    List<CustomerInfo> customerInfos2 = customerRepo.findByName(
        createInfoName(name, PowerType.ELECTRIC_VEHICLE));

    for (int i = 0; i < populationCount; i++) {
      int randomGroupId = getRandomGroupId(groupDetails, generator);

      SocialGroup group = groups.get(randomGroupId);
      SocialGroupDetail groupDetail = groupDetails.get(randomGroupId);
      Map<Integer, ActivityDetail> activityDetails =
          allActivityDetails.get(randomGroupId);

      String gender = "female";
      if (generator.nextDouble() < groupDetail.getMaleProbability()) {
        gender = "male";
      }

      // For now, all cars have equal probability
      int randomCar = generator.nextInt(cars.size());
      Car car = cars.get(randomCar);

      EvCustomer evCustomer = new EvCustomer();
      evCustomer.initialize(
          group, gender, activities, activityDetails, car, generator);
      evCustomers.add(evCustomer);

      if (customerInfos1.size() > 0) {
        CustomerInfo customerInfo = customerInfos1.get(0);
        double weight = generator.nextDouble() * Config.WEIGHT_INCONVENIENCE;
        double weeks = Config.MIN_DEFAULT_DURATION +
            generator.nextInt(Config.MAX_DEFAULT_DURATION - Config.MIN_DEFAULT_DURATION);
        tariffEvaluators.put(customerInfo,
            createTariffEvaluator(evCustomer, customerInfo, weight, weeks));
      }

      if (customerInfos2.size() > 0) {
        CustomerInfo customerInfo = customerInfos2.get(0);
        double weight = generator.nextDouble() * Config.WEIGHT_INCONVENIENCE;
        double weeks = Config.MIN_DEFAULT_DURATION +
            generator.nextInt(Config.DEFAULT_DURATION_WINDOW);
        tariffEvaluators.put(customerInfo,
            createTariffEvaluator(evCustomer, customerInfo, weight, weeks));
      }
    }
  }

  protected TariffEvaluator createTariffEvaluator (EvCustomer evCustomer,
                                                   CustomerInfo customerInfo,
                                                   double weight, double weeks)
  {
    TariffEvaluationWrapper wrapper =
        new TariffEvaluationWrapper(customerInfo, evCustomer, generator);
    TariffEvaluator te = new TariffEvaluator(wrapper);
    te.initializeInconvenienceFactors(Config.TOU_FACTOR,
        Config.TIERED_RATE_FACTOR,
        Config.VARIABLE_PRICING_FACTOR,
        Config.INTERRUPTIBILITY_FACTOR);
    te.withInconvenienceWeight(weight)
        .withInertia(Config.NSInertia)
        .withPreferredContractDuration(weeks * Config.DAYS_OF_WEEK)
        .withRationality(Config.RATIONALITY_FACTOR)
        .withTariffEvalDepth(Config.TARIFF_COUNT)
        .withTariffSwitchFactor(Config.BROKER_SWITCH_FACTOR);
    return te;
  }

  protected int getRandomGroupId (Map<Integer, SocialGroupDetail> groupDetails,
                                  Random gen)
  {
    double r = gen.nextDouble();
    for (Map.Entry entry : groupDetails.entrySet()) {
      r -= ((SocialGroupDetail) entry.getValue()).getProbability();
      if (r < 0) {
        return (Integer) entry.getKey();
      }
    }

    return 1;
  }

  // =====SUBSCRIPTION FUNCTIONS===== //

  @Override
  public void subscribeDefault ()
  {
    super.subscribeDefault();

    for (CustomerInfo customer : customerInfos) {
      if (customer.getPowerType() == PowerType.ELECTRIC_VEHICLE &&
          tariffMarketService
              .getDefaultTariff(PowerType.ELECTRIC_VEHICLE) == null) {

        log.debug("No Default Tariff for ELECTRIC_VEHICLE so the customer "
            + customer.toString()
            + " subscribe to CONSUMPTION Default Tariff instead");

        tariffMarketService.subscribeToTariff(
            tariffMarketService.getDefaultTariff(PowerType.CONSUMPTION),
            customer, customer.getPopulation());

        log.info("CustomerInfo of type ELECTRIC_VEHICLE of " + toString()
            + " was subscribed to the default CONSUMPTION tariff successfully.");
      }
    }
  }

  // =====CONSUMPTION FUNCTIONS===== //

  @Override
  public void consumePower ()
  {
    for (CustomerInfo customer : customerInfos) {
      List<TariffSubscription> subscriptions =
          tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(customer);

      double load = getConsumptionByTimeslot();

      log.debug("Consumption Load for Customer " + customer.toString() + ": "
          + load + " for subscriptions " + subscriptions.toString());

      boolean foundEvPower = false;
      for (TariffSubscription tariffSubscription : subscriptions) {
        PowerType powerType = tariffSubscription.getTariff().getPowerType();
        if (powerType == PowerType.ELECTRIC_VEHICLE) {
          tariffSubscription.usePower(load);
          foundEvPower = true;
          break;
        }
      }

      if (!foundEvPower && subscriptions.size() > 0) {
        subscriptions.get(0).usePower(load);
      }
    }
  }

  protected double getConsumptionByTimeslot ()
  {
    int hour = timeService.getHourOfDay();
    int dayOfWeek = timeService.getCurrentDateTime().getDayOfWeek();

    double totalConsumption = 0.0;
    for (EvCustomer evCustomer : evCustomers) {
      totalConsumption += evCustomer.charge(dayOfWeek, hour);
    }

    return totalConsumption;
  }

  // =====EVALUATION FUNCTIONS===== //

  /**
   * This is the basic evaluation function, taking into consideration the
   * minimum cost without shifting the appliances' load but the tariff chosen
   * is
   * picked up randomly by using a possibility pattern. The better tariffs
   * have
   * more chances to be chosen.
   */
  public void evaluateNewTariffs ()
  {
    for (CustomerInfo customer : customerInfos) {
      TariffEvaluator evaluator = tariffEvaluators.get(customer);
      evaluator.evaluateTariffs();
    }
  }

  // =====STEP FUNCTIONS===== //

  @Override
  public void step ()
  {
    doActivities();
    consumePower();
  }

  protected void doActivities ()
  {
    int hour = timeService.getHourOfDay();
    int day = timeService.getCurrentDateTime().getDayOfWeek();

    for (EvCustomer evCustomer : evCustomers) {
      evCustomer.doActivities(day, hour);
    }
  }

  @Override
  public String toString ()
  {
    return name;
  }

  public static String createInfoName (String base, PowerType type)
  {
    String s = type.toString().replace("_", " ");

    final StringBuilder result = new StringBuilder(base.length() + s.length() + 1);
    result.append(base).append(" ");

    String[] words = s.split("\\s");
    for (int i = 0, l = words.length; i < l; ++i) {
      if (i > 0) {
        result.append(" ");
      }
      result.append(Character.toUpperCase(words[i].charAt(0)))
          .append(words[i].substring(1).toLowerCase());
    }

    return result.toString();
  }
}

class TariffEvaluationWrapper implements CustomerModelAccessor
{
  private CustomerInfo customerInfo;
  private EvCustomer evCustomer;
  private Random generator;

  public TariffEvaluationWrapper (CustomerInfo customerInfo,
                                  EvCustomer evCustomer,
                                  Random generator)
  {
    this.customerInfo = customerInfo;
    this.evCustomer = evCustomer;
    this.generator = generator;
  }

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    return customerInfo;
  }

  @Override
  public double[] getCapacityProfile (Tariff tariff)
  {
    double[] result = new double[Config.HOURS_OF_DAY];

    for (int i = 0; i < Config.HOURS_OF_DAY; i++) {
      result[i] = evCustomer.getDominantLoad() / Config.HOURS_OF_DAY;
    }

    return result;
  }

  @Override
  public double getBrokerSwitchFactor (boolean isSuperseding)
  {
    double result = Config.BROKER_SWITCH_FACTOR;
    if (isSuperseding) {
      return result * 5.0;
    }
    return result;
  }

  @Override
  public double getTariffChoiceSample ()
  {
    return generator.nextDouble();
  }

  @Override
  public double getInertiaSample ()
  {
    return generator.nextDouble();
  }
}
