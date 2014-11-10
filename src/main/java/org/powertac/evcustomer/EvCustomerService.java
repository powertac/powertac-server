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

package org.powertac.evcustomer;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.*;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.*;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.evcustomer.beans.*;
import org.powertac.evcustomer.customers.EvSocialClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.5, Date: 2013.11.25
 */
@Service
public class EvCustomerService extends TimeslotPhaseProcessor
    implements InitializationService, NewTariffListener, CustomerServiceAccessor
{
  /**
   * logger for trace logging -- use log.info(), log.warn(), and log.error()
   * appropriately. Use log.debug() for output you want to see in testing or
   * debugging.
   */
  static private Logger log = Logger.getLogger(EvCustomerService.class
      .getName());

  @Autowired
  private TariffMarket tariffMarketService;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private CustomerRepo customerRepo;

  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @Autowired
  private ServerConfiguration serverPropertiesService;

  protected List<EvSocialClass> evSocialClassList;
  protected List<CarType> carTypes;
  protected Map<Integer, SocialGroup> socialGroups;
  protected Map<Integer, Activity> activities;
  protected Map<Integer, Map<Integer, ActivityDetail>> allActivityDetails;
  protected Map<String, SocialClassDetail> socialClassDetails;

  protected static String carTypesXml = "EvCarTypes.xml";
  protected static String socialGroupsXml = "EvSocialGroups.xml";
  protected static String activitiesXml = "EvActivities.xml";
  protected static String activityDetailsXml = "EvActivityDetails.xml";
  protected static String socialClassesXml = "EvSocialClasses.xml";

  private static int seedId = 1;

  /**
   * This is called once at the beginning of each game.
   */
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    if (!completedInits.contains("DefaultBroker") ||
        !completedInits.contains("TariffMarket")) {
      log.debug("Waiting for DefaultBroker and TariffMarket to initialize");
      return null;
    }

    serverPropertiesService.configureMe(this);
    tariffMarketService.registerNewTariffListener(this);
    super.init();
    serverPropertiesService.publishConfiguration(this);

    // Shared by all customers
    //carTypes = loadCarTypes();
    socialGroups = loadSocialGroups(); // map <groupId, group>
    activities = loadActivities(); //map <activity-id, activity>
    allActivityDetails = loadActivityDetails(); // map<groupId, map<detailId, detail>>
    socialClassDetails = loadSocialClassesDetails();
    evSocialClassList = initializeSocialClasses(seedId++);

    return "EvCustomer";
  }

  private List<EvSocialClass> initializeSocialClasses (int seed)
  {
    RandomSeed rs1 = randomSeedRepo.
        getRandomSeed("EvCustomerService", seed++, "initializeSocialClasses");

    List<EvSocialClass> evSocialClassList = new ArrayList<EvSocialClass>();

    for (SocialClassDetail classDetail : socialClassDetails.values()) {
      int populationCount = classDetail.getMinCount() +
          rs1.nextInt(classDetail.getMaxCount() - classDetail.getMinCount());

      String name = "EV " + classDetail.getName();
      EvSocialClass socialClass = new EvSocialClass(name);
      socialClass.setServiceAccessor(this);
      // TODO - why do we need the CONSUMPTION info?
      //socialClass.addCustomer(populationCount, carTypes,
      //                        PowerType.CONSUMPTION);
      socialClass.addCustomer(populationCount, carTypes,
                              PowerType.ELECTRIC_VEHICLE);

      socialClass.initialize(socialGroups, classDetail.getSocialGroupDetails(),
          activities, allActivityDetails, carTypes, populationCount, seed++);
      evSocialClassList.add(socialClass);
      socialClass.subscribeDefault(tariffMarketService);
    }

    return evSocialClassList;
  }

  /**
   * @Override @code{NewTariffListener}
   */
  @Override
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    log.info("PublishNewTariffs");

    for (EvSocialClass evSocialClass : evSocialClassList) {
      evSocialClass.evaluateTariffs(tariffs);
    }
  }

  /**
   * @Override @code{TimeslotPhaseProcessor}
   */
  @Override
  public void activate (Instant time, int phaseNumber)
  {
    log.info("Activate");

    for (EvSocialClass evSocialClass : evSocialClassList) {
      evSocialClass.step();
    }
  }

  /**
   * @Override @code{InitializationService}
   */
  @Override
  public void setDefaults ()
  {
  }

////  public static List<CarType> loadCarTypes ()
////  {
////    List<CarType> carTypes = new ArrayList<CarType>();
////
////    log.info("Attempting to load CarTypes from : " + carTypesXml);
////    NodeList carNodes = getNodeList(carTypesXml, "car");
////
////    if (carNodes != null && carNodes.getLength() > 0) {
////      log.info("Loading " + carNodes.getLength() + " CarTypes");
////
////      for (int i = 0; i < carNodes.getLength(); i++) {
////        Element element = (Element) carNodes.item(i);
////        String name = element.getAttribute("name");
////        double kwh = getElementDouble(element, "kwh");
////        double range = getElementDouble(element, "range");
////        double home = getElementDouble(element, "home_charging");
////        double away = getElementDouble(element, "away_charging");
////        carTypes.add(new CarType(name, kwh, range, home, away));
////      }
////
////      log.info("Successfully loaded " + carNodes.getLength() + " CarTypes");
////    }
//
//    return carTypes;
//  }

  /**
   * Populates the socialClassDetails map with info about the class, and a
   * SocialGroupDetail instance for each group specified in the xml
   * structure.
   * Result: map<class-name, >
   */
  public static Map<String, SocialClassDetail> loadSocialClassesDetails ()
  {
    Map<String, SocialClassDetail> socialClassDetails =
        new HashMap<String, SocialClassDetail>();

    log.info("Attempting to load SocialGroups from : " + socialClassesXml);
    NodeList classNodes = getNodeList(socialClassesXml, "class");

    if (classNodes != null && classNodes.getLength() > 0) {
      log.info("Loading " + classNodes.getLength() + " SocialClass");

      for (int i = 0; i < classNodes.getLength(); i++) {
        Element classElement = (Element) classNodes.item(i);
        String className = classElement.getAttribute("name");
        int temp1 = getElementInt(classElement, "minCount");
        int temp2 = getElementInt(classElement, "maxCount");
        int minCount = Math.min(temp1, temp2);
        int maxCount = Math.max(temp1, temp2);

        Map<Integer, SocialGroupDetail> groupDetails =
            new HashMap<Integer, SocialGroupDetail>();
        NodeList groupNodes = classElement.getElementsByTagName("group");
        for (int j = 0; j < groupNodes.getLength(); j++) {
          Element element = (Element) groupNodes.item(j);
          int groupId = getElementInt(element, "id");
          double probability = getElementDoubleSimple(element, "prob");
          double maleProbability = getElementDoubleSimple(element, "male_prob");
          groupDetails.put(groupId,
              new SocialGroupDetail(groupId, probability, maleProbability));
        }

        socialClassDetails.put(className,
            new SocialClassDetail(className, minCount, maxCount, groupDetails));
      }

      log.info("Successfully loaded "+ classNodes.getLength() +" SocialClasses");
    }

    return socialClassDetails;
  }

  public static Map<Integer, SocialGroup> loadSocialGroups ()
  {
    Map<Integer, SocialGroup> socialGroups = new HashMap<Integer, SocialGroup>();

    log.info("Attempting to load SocialGroups from : " + socialGroupsXml);
    NodeList groupNodes = getNodeList(socialGroupsXml, "group");

    if (groupNodes != null && groupNodes.getLength() > 0) {
      log.info("Loading " + groupNodes.getLength() + " SocialGroups");

      for (int i = 0; i < groupNodes.getLength(); i++) {
        Element groupElement = (Element) groupNodes.item(i);
        String groupName = groupElement.getAttribute("name");
        int id = getElementInt(groupElement, "id");
        socialGroups.put(id, new SocialGroup(id, groupName));
      }

      log.info("Successfully loaded "+ groupNodes.getLength() +" SocialGroups");
    }

    return socialGroups;
  }

  // Result is map <activity-id, activity>
  public static Map<Integer, Activity> loadActivities ()
  {
    Map<Integer, Activity> activities = new HashMap<Integer, Activity>();

    log.info("Attempting to load Activities from : " + activitiesXml);
    NodeList activityNodes = getNodeList(activitiesXml, "activity");

    if (activityNodes != null && activityNodes.getLength() > 0) {
      log.info("Loading " + activityNodes.getLength() + " Activities");

      for (int i = 0; i < activityNodes.getLength(); i++) {
        Element element = (Element) activityNodes.item(i);
        String name = element.getAttribute("name");
        int id = getElementInt(element, "id");
        double weekday_weight =
                getElementDoubleSimple(element, "weekday_weight");
        double weekend_weight =
                getElementDoubleSimple(element, "weekend_weight");

        activities.put(id, new Activity(id, name,
                                        weekday_weight, weekend_weight));
      }

      log.info("Successfully loaded " + activityNodes.getLength() + " Activities");
    }

    return activities;
  }

  // Result: map<groupId, map<detailId, detail>>
  public static Map<Integer, Map<Integer, ActivityDetail>> loadActivityDetails ()
  {
    Map<Integer, Map<Integer, ActivityDetail>> activityDetails =
        new HashMap<Integer, Map<Integer, ActivityDetail>>();

    log.info("Attempting to load Details from : " + activityDetailsXml);
    NodeList detailNodes = getNodeList(activityDetailsXml, "detail");

    if (detailNodes != null && detailNodes.getLength() > 0) {
      log.info("Loading details for" + detailNodes.getLength() + " groups");

      for (int i = 0; i < detailNodes.getLength(); i++) {
        Element detailElement = (Element) detailNodes.item(i);
        int groupId = Integer.parseInt(detailElement.getAttribute("group_id"));

        Map<Integer, ActivityDetail> details =
            new HashMap<Integer, ActivityDetail>();

        NodeList activityNodes = detailElement.getElementsByTagName("activity");
        for (int j = 0; j < activityNodes.getLength(); j++) {
          Element element = (Element) activityNodes.item(j);
          int id = getElementInt(element, "id");
          double maleKm = getElementDoubleSimple(element, "male_daily_km");
          double femaleKm = getElementDoubleSimple(element, "female_daily_km");
          double maleProb = getElementDoubleSimple(element, "male_prob");
          double femaleProb = getElementDoubleSimple(element, "female_prob");

          details.put(id,
              new ActivityDetail(id, maleKm, femaleKm, maleProb, femaleProb));
        }
        activityDetails.put(groupId, details);
      }

      log.info("Successfully loaded Activity Details");
    }

    return activityDetails;
  }

  private static NodeList getNodeList (String fileName, String nodeName)
  {
    try {
      InputStream configStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream(fileName);

      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(configStream);

      return doc.getElementsByTagName(nodeName);
    }
    catch (Exception e) {
      log.error("Error loading from : " + fileName);
      log.error(e.toString());
      return null;
    }
  }

  private static int getElementInt (Element element, String tagName)
  {
    return Integer.parseInt(element.getAttribute(tagName));
  }

  private static double getElementDoubleSimple (Element element, String tagName)
  {
    return Double.parseDouble(element.getAttribute(tagName));
  }

  private static double getElementDouble (Element element, String tagName)
  {
    return Double.parseDouble(element.getElementsByTagName(tagName)
        .item(0).getFirstChild().getNodeValue());
  }

  // ===================== CustomerModelAccessor API =========================
  @Override
  public CustomerRepo getCustomerRepo ()
  {
    return customerRepo;
  }

  @Override
  public RandomSeedRepo getRandomSeedRepo ()
  {
    return randomSeedRepo;
  }

  @Override
  public TariffRepo getTariffRepo ()
  {
    // not used
    return null;
  }

  @Override
  public TariffSubscriptionRepo getTariffSubscriptionRepo ()
  {
    // not used
    return tariffSubscriptionRepo;
  }

  @Override
  public TimeslotRepo getTimeslotRepo ()
  {
    return timeslotRepo;
  }

  @Override
  public WeatherReportRepo getWeatherReportRepo ()
  {
    // not used
    return null;
  }
}