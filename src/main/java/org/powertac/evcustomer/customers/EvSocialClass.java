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

import org.powertac.common.*;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.state.Domain;
import org.powertac.customer.AbstractCustomer;
import org.powertac.evcustomer.Config;
import org.powertac.evcustomer.beans.*;

import java.util.*;

/**
 * Instances represent identifiable "classes" of EV customers. 
 * Each EvSocialClass aggregates a collection if EvCustomer instances, as
 * determined by configuration. However, this class and not the EvCustomer
 * class implements the AbstractCustomer interface, which means that
 * each instance of EvSocialClass is specified in the configuration.
 * This scheme allows dynamic configuration of the EvCustomer instances,
 * each of which has a specific gender, social group, and car type, and
 * the probabilities and other attributes of the various activities are
 * a function of social class, as specified in the configuration.
 * 
 * The configuration setup is basically a set of tables: social class,
 * social group, activity, and car type. Join tables add attributes
 * between classes and groups, between classes and car types, and
 * between groups and activities (for
 * example, we expect the wealthier classes to be more likely to own a
 * Tesla-80, and full-time workers are more likely to commute regularly).
 * 
 * Because of the dynamic configuration, it is necessary to preserve
 * the configuration between a boot session and a sim session. This is done
 * by recording the attributes of individual customers created by the initial
 * configuration at the start of a boot session, recording those attributes
 * in the boot record, and re-loading the configuration from the boot
 * record at the start of a sim session. In other words, the dynamic
 * configuration happens only at the start of a boot session.
 * 
 * @author Konstantina Valogianni, Govert Buijs
 */
@Domain
@ConfigurableInstance
public class EvSocialClass extends AbstractCustomer
{
  //private static Logger log = Logger.getLogger(EvSocialClass.class.getName());

  private RandomSeed generator;

  private Config config;

  @ConfigurableValue(valueType = "Integer", description = "minimum population")
  private int minCount;

  @ConfigurableValue(valueType = "Integer", description = "maximum population")
  private int maxCount;

  private ArrayList<EvCustomer> evCustomers;

  // indexed bean lists
  private Map<Integer, SocialGroup> groups;
  private Map<String, CarType> carTypes;
  private Map<Integer, Activity> activities;
  private Map<Integer, ClassGroup> classGroups;
  private Map<String, ClassCar> classCars;

  // bootstrap state
  private int population;

  @ConfigurableValue(valueType = "List",
      bootstrapState = true,
      description = "List of customer attributes")
  private ArrayList<String> customerAttributeList = null;
  // true just in case we are restoring from a boot record
  private boolean restoring = false;

  /**
   * Default constructor, requires manual setting of name
   */
  public EvSocialClass ()
  {
    super();
  }

  public EvSocialClass (String name)
  {
    super(name);
  }

  /**
   * Hooks up data structures, creates CustomerInfo instances
   */
  @Override
  public void initialize()
  {
    super.initialize();
    log.info("Initialize " + name);
    Map<String, Collection<?>> beans; //= new HashMap<String, Collection<?>>();
    this.generator = service.getRandomSeedRepo().
        getRandomSeed("EvSocialClass-" + name, 1, "initialize");
    config = Config.getInstance();

    beans = config.getBeans();
    unpackBeans(beans);

    // Create and set up the customer instances
    evCustomers = new ArrayList<EvCustomer>();
    if (null == customerAttributeList) {
      configureForBoot(beans);
    }
    else {
      configureForSim(beans);
    }
  }

  private void configureForBoot (Map<String, Collection<?>> beans)
  {
    // setup at beginning of boot session
    customerAttributeList = new ArrayList<String>();
    population = minCount + generator.nextInt(Math.abs(maxCount - minCount));
    log.info("Configuring " + population + " customers for class "
        + this.getName());

    // Prepare for the joins
    ArrayList<SocialGroup> groupList =
        new ArrayList<SocialGroup>(groups.values());
    double cgProbability = 0.0;
    for (SocialGroup group : groupList) {
      cgProbability += classGroups.get(group.getId()).getProbability();
    }
    ArrayList<CarType> carList = new ArrayList<CarType>(carTypes.values());
    double ccProbability = 0.0;
    for (CarType car : carList) {
      ccProbability += classCars.get(car.getName()).getProbability();
    }

    for (int i = 0; i < population; i++) {
      // pick a random social group
      SocialGroup thisGroup = pickGroup(groupList, cgProbability);
      ClassGroup groupDetails = classGroups.get(thisGroup.getId());
      // pick a gender
      String gender = "female";
      if (generator.nextDouble() < groupDetails.getMaleProbability()) {
        gender = "male";
      }
      // pick a random car
      CarType car = pickCar(carList, ccProbability);
      // name format is class.groupId.gender.carName.index
      String customerName =
          this.name + "." + thisGroup.getId() + "." + gender
          + "." + car.getName() + "." + i;
      customerAttributeList.add(customerName);
      instantiateCustomer(beans, thisGroup, gender, car, customerName);
    }
  }

  private void configureForSim (Map<String, Collection<?>> beans)
  {
    population = customerAttributeList.size();
    for (String description : customerAttributeList) {
      String[] attributes = description.split("\\.");
      SocialGroup thisGroup = groups.get(Integer.parseInt(attributes[1]));
      String gender = attributes[2];
      CarType car = carTypes.get(attributes[3]);
      instantiateCustomer(beans, thisGroup, gender, car, description);
    }
  }

  private void instantiateCustomer (Map<String, Collection<?>> beans,
                                    SocialGroup thisGroup, String gender,
                                    CarType car, String customerName)
  {
    EvCustomer customer = new EvCustomer(customerName);
    log.info("Adding EvCustomer " + customerName);
    evCustomers.add(customer);
    CustomerInfo info =
        customer.initialize(thisGroup, gender, activities,
                            getGroupActivities(beans, thisGroup),
                            car, service);
    addCustomerInfo(info);
    service.getCustomerRepo().add(info);
  }

  private SocialGroup pickGroup (ArrayList<SocialGroup> groupList,
                                 double scale)
  {
    double picker = generator.nextDouble() * scale;
    for (SocialGroup group : groupList) {
      picker -= classGroups.get(group.getId()).getProbability();
      if (picker <= 0.0) return group;
    }
    return groupList.get(groupList.size() - 1);
  }

  private CarType pickCar (ArrayList<CarType> carList, double scale)
  {
    double picker = generator.nextDouble() * scale;
    for (CarType car : carList) {
      picker -= classCars.get(car.getName()).getProbability();
      if (picker <= 0.0) return car;
    }
    return carList.get(carList.size() - 1);

  }

  // creates indexed lists of the various bean types
  void unpackBeans (Map<String, Collection<?>> beans)
  {
    // groups
    groups = new HashMap<Integer, SocialGroup>();
    for (Object thing : beans.get("SocialGroup")) {
      SocialGroup group = (SocialGroup) thing;
      groups.put(group.getId(), group);
    }

    // car types
    carTypes = new HashMap<String, CarType>();
    for (Object thing : beans.get("CarType")) {
      CarType car = (CarType) thing;
      carTypes.put(car.getName(), car);
    }

    // activities
    activities = new HashMap<Integer, Activity>();
    for (Object thing : beans.get("Activity")) {
      Activity activity = (Activity) thing;
      activities.put(activity.getId(), activity);
    }

    // classGroups, indexed by group ID
    classGroups = new HashMap<Integer, ClassGroup>();
    for (Object thing : beans.get("ClassGroup")) {
      ClassGroup classGroup = (ClassGroup) thing;
      if (classGroup.getSocialClassName().equals(getName())) {
        // one of ours
        classGroups.put(classGroup.getGroupId(), classGroup);
      }
    }

    // classCars, indexed by CarType name
    classCars = new HashMap<String, ClassCar>();
    for (Object thing : beans.get("ClassCar")) {
      ClassCar classCar = (ClassCar) thing;
      if (classCar.getSocialClassName().equals(getName())) {
        // one of ours
        classCars.put(classCar.getCarName(), classCar);
      }
    }
  }

  /**
   * Extracts the GroupActivity instances associated with the given group
   */
  private HashMap<Integer, GroupActivity>
  getGroupActivities (Map<String, Collection<?>> beans, SocialGroup group)
  {
    HashMap<Integer, GroupActivity> result =
        new HashMap<Integer, GroupActivity>();
    for (Object thing: beans.get("GroupActivity")) {
      GroupActivity ga = (GroupActivity) thing;
      if (ga.getGroupId() == group.getId()) {
        // one of ours
        result.put(ga.getActivityId(), ga);
      }
    }
    if (result.size() != activities.size())
       log.error("found " + result.size()
                + " group-activities for group " + group.getId()
                + ", should be " + activities.size());
    return result;
  }

  // =====EVALUATION FUNCTIONS===== //

  /**
   * This is the basic evaluation function, taking into consideration the
   * minimum cost without shifting the appliances' load but the tariff chosen
   * is picked up randomly by using a possibility pattern. The better tariffs
   * have more chances to be chosen.
   */
  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    for (EvCustomer customer : evCustomers) {
      customer.evaluateTariffs(tariffs);
    }
  }

  // =====STEP FUNCTIONS===== //

  @Override
  public void step ()
  {
    for (EvCustomer customer : evCustomers) {
      customer.step(service.getTimeslotRepo().currentTimeslot());
    }
  }
//
//  protected void getLoads (int day, int hour)
//  {
//    consumptionLoad = 0.0;
//    evLoad = 0.0;
//    upRegulation = 0.0;
//    downRegulation = 0.0;
//
//    for (EvCustomer evCustomer : evCustomers) {
//      double[] loads = evCustomer.getLoads(day, hour);
//
//      consumptionLoad += loads[0];
//      evLoad += loads[1];
//      upRegulation += loads[2];
//      downRegulation += loads[3];
//    }
//
//    log.info(String.format("%s : consumption = % 7.2f ; electric vehicule = " +
//            "% 7.2f ; up regulation = % 7.2f ; down regulation = % 7.2f",
//        name, consumptionLoad, evLoad, upRegulation, downRegulation
//    ));
//  }

  @Override
  public String toString ()
  {
    return name;
  }

  // ===== USED FOR TESTING ===== //

  ArrayList<EvCustomer> getEvCustomers ()
  {
    return evCustomers;
  }

  int getPopulation ()
  {
    return population;
  }

  List<String> getCustomerAttributeList ()
  {
    return customerAttributeList;
  }

  Random getGenerator ()
  {
    return generator;
  }

  void setMinCount (int count)
  {
    minCount = count;
  }
  
  int getMinCount ()
  {
    return minCount;
  }

  void setMaxCount (int count)
  {
    maxCount = count;
  }

  int getMaxCount ()
  {
    return maxCount;
  }

  Map<Integer, SocialGroup> getGroups ()
  {
    return groups;
  }

  Map<String, CarType> getCarTypes ()
  {
    return carTypes;
  }

  Map<Integer, Activity> getActivities ()
  {
    return activities;
  }

  Map<Integer, ClassGroup> getClassGroups ()
  {
    return classGroups;
  }

  Map<String, ClassCar> getClassCars ()
  {
    return classCars;
  }
}
