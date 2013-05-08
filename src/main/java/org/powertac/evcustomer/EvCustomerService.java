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
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ActivityDetail;
import org.powertac.evcustomer.beans.Car;
import org.powertac.evcustomer.beans.SocialGroup;
import org.powertac.evcustomer.customers.EvSocialClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.*;


/**
 * TODO
 *
 * @author Konstantina Valogianni, Govert Buijs
 * @version 0.2, Date: 2013.05.08
 */
@Service
public class EvCustomerService extends TimeslotPhaseProcessor
    implements InitializationService, NewTariffListener
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
  private RandomSeedRepo randomSeedRepo;

  /** Random Number Generator */
  private RandomSeed rs1;

  /** List of the SocialClass Customers in the competition */
  private ArrayList<EvSocialClass> evSocialClassList;

  // Shared by all customers
  private List<Car> cars;
  private List<SocialGroup> socialGroups;
  private Map<Integer, Activity> activities;
  private Map<Integer, Map<Integer, ActivityDetail>> allActivityDetails;

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

    tariffMarketService.registerNewTariffListener(this);
    rs1 = randomSeedRepo.getRandomSeed("EvCustomerService", 1,
                                       "EV Customer Models");
    super.init();

    int daysOfCompetition = Competition.currentCompetition()
        .getExpectedTimeslotCount() / EvSocialClass.HOURS_OF_DAY;
    if (daysOfCompetition == 0) {
      log.info("No Days Of Competition Taken");
      daysOfCompetition = 63;
    }
    EvSocialClass.DAYS_OF_COMPETITION = daysOfCompetition;

    loadCarTypes("EvCarTypes.xml");
    loadSocialGroups("EvSocialGroups.xml");
    activities = loadActivities("EvActivities.xml");
    allActivityDetails = loadActivityDetails("EvActivityDetails.xml");
    loadSocialClasses("EvSocialClasses.xml");

    return "EvCustomer";
  }

  private void loadCarTypes (String configResource)
  {
    cars = new ArrayList<Car>();

    log.info("Attempting to load CarTypes from : " + configResource);
    try {
      InputStream configStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream(configResource);

      DocumentBuilderFactory docBuilderFactory =
          DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(configStream);

      NodeList carNodes = doc.getElementsByTagName("car");
      log.info("Loading " + carNodes.getLength() + " CarTypes");

      for (int i = 0; i < carNodes.getLength(); i++) {
        Element element = (Element) carNodes.item(i);
        String name = element.getAttribute("name");
        double kwh = Double.parseDouble(element.getElementsByTagName("kwh")
            .item(0).getFirstChild().getNodeValue());
        double range = Double.parseDouble(element.getElementsByTagName("range")
            .item(0).getFirstChild().getNodeValue());
        double home = Double.parseDouble(element.getElementsByTagName("home_charging")
            .item(0).getFirstChild().getNodeValue());
        double away = Double.parseDouble(element.getElementsByTagName("away_charging")
            .item(0).getFirstChild().getNodeValue());

        cars.add(new Car(name, kwh, range, home, away));
      }

      log.info("Successfully loaded " + carNodes.getLength() + " CarTypes");
    }
    catch (Exception e) {
      log.error("Error loading CarTypes from : " + configResource);
      log.error(e.toString());
    }
  }

  private void loadSocialClasses (String configResource)
  {
    evSocialClassList = new ArrayList<EvSocialClass>();

    log.info("Attempting to load SocialGroups from : " + configResource);
    try {
      InputStream configStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream(configResource);

      DocumentBuilderFactory docBuilderFactory =
          DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(configStream);

      NodeList classNodes = doc.getElementsByTagName("class");
      log.info("Loading " + classNodes.getLength() + " SocialClass");

      for (int i = 0; i < classNodes.getLength(); i++) {
        Element classElement = (Element) classNodes.item(i);
        String className = classElement.getAttribute("name");
        // TODO Test for maxCount >= minCount
        int minCount = Integer.parseInt(classElement.getAttribute("minCount"));
        int maxCount = Integer.parseInt(classElement.getAttribute("maxCount"));
        int populationCount = minCount + rs1.nextInt(maxCount - minCount);

        NodeList groupNodes = classElement.getElementsByTagName("group");
        double[] groupProbabilities = new double[groupNodes.getLength()];
        double[] maleProbabilities = new double[groupNodes.getLength()];
        for (int j = 0; j < groupNodes.getLength(); j++) {
          Element element = (Element) groupNodes.item(j);
          int groupId = Integer.parseInt(element.getAttribute("id"));
          groupProbabilities[groupId-1] =
              Double.parseDouble(element.getAttribute("probability"));
          maleProbabilities[groupId-1] =
              Double.parseDouble(element.getAttribute("male_probability"));
        }

        String base = "EV SocialClass " + className;
        EvSocialClass evSocialClass = new EvSocialClass(base);
        evSocialClass.addCustomerInfo(
            new CustomerInfo(base + " CONSUMPTION", populationCount)
                .withPowerType(PowerType.CONSUMPTION));
        evSocialClass.addCustomerInfo(
            new CustomerInfo(base + " " + PowerType.ELECTRIC_VEHICLE.toString(), populationCount)
                .withPowerType(PowerType.ELECTRIC_VEHICLE));
        evSocialClass.initialize(socialGroups, activities, allActivityDetails,
            cars, populationCount, groupProbabilities, maleProbabilities, rs1);
        evSocialClassList.add(evSocialClass);
      }

      log.info("Successfully loaded " + classNodes.getLength() + " SocialClasses");
    }
    catch (Exception e) {
      log.error("Error loading SocialClasses from : " + configResource);
      log.error(e.toString());
    }
  }

  private void loadSocialGroups (String configResource)
  {
    socialGroups = new ArrayList<SocialGroup>();

    log.info("Attempting to load SocialGroups from : " + configResource);
    try {
      InputStream configStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream(configResource);

      DocumentBuilderFactory docBuilderFactory =
          DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(configStream);

      NodeList groupNodes = doc.getElementsByTagName("group");
      log.info("Loading " + groupNodes.getLength() + " SocialGroups");

      for (int i = 0; i < groupNodes.getLength(); i++) {
        Element groupElement = (Element) groupNodes.item(i);
        String groupName = groupElement.getAttribute("name");
        int id = Integer.parseInt(groupElement.getAttribute("id"));
        socialGroups.add(new SocialGroup(id, groupName));
      }

      log.info("Successfully loaded " + groupNodes.getLength() + " SocialGroups");
    }
    catch (Exception e) {
      log.error("Error loading SocialGroups from : " + configResource);
      log.error(e.toString());
    }
  }

  public static Map<Integer, Activity> loadActivities (String configResource)
  {
    Map<Integer, Activity> activities = new HashMap<Integer, Activity>();

    log.info("Attempting to load SocialGroups from : " + configResource);
    try {
      InputStream configStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream(configResource);

      DocumentBuilderFactory docBuilderFactory =
          DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(configStream);

      NodeList activityNodes = doc.getElementsByTagName("activity");
      log.info("Loading " + activityNodes.getLength() + " Activities");

      for (int i = 0; i < activityNodes.getLength(); i++) {
        Element element = (Element) activityNodes.item(i);
        String name = element.getAttribute("name");
        int id = Integer.parseInt(element.getAttribute("id"));
        double weekdayWeight = Double.parseDouble(
            element.getAttribute("weekday_weight"));
        double weekendWeight = Double.parseDouble(
            element.getAttribute("weekend_weight"));

        activities.put(id,
            new Activity(id, name, weekdayWeight, weekendWeight));
      }

      log.info("Successfully loaded " + activityNodes.getLength() + " Activities");
    }
    catch (Exception e) {
      log.error("Error loading Activities from : " + configResource);
      log.error(e.toString());
    }

    return activities;
  }

  public static Map<Integer, Map<Integer, ActivityDetail>> loadActivityDetails (
      String configResource)
  {
    Map<Integer, Map<Integer, ActivityDetail>> activityDetails =
        new HashMap<Integer, Map<Integer, ActivityDetail>>();

    log.info("Attempting to load Details from : " + configResource);
    try {
      InputStream configStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream(configResource);

      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(configStream);

      NodeList detailNodes = doc.getElementsByTagName("detail");
      log.info("Loading details for" + detailNodes.getLength() + " groups");

      for (int i = 0; i < detailNodes.getLength(); i++) {
        Element detailElement = (Element) detailNodes.item(i);
        int groupId = Integer.parseInt(detailElement.getAttribute("group_id"));

        Map<Integer, ActivityDetail> details =
            new HashMap<Integer, ActivityDetail>();

        NodeList activityNodes = detailElement.getElementsByTagName("activity");
        for (int j = 0; j < activityNodes.getLength(); j++) {
          Element element = (Element) activityNodes.item(j);
          int id = Integer.parseInt(element.getAttribute("id"));

          double maleKm = Double.parseDouble(
              element.getAttribute("male_daily_km"));
          double femaleKm = Double.parseDouble(
              element.getAttribute("female_daily_km"));
          double activityProbability = Double.parseDouble(
              element.getAttribute("prob"));

          // For now probabilities are equal for male and female
          details.put(id, new ActivityDetail(id, maleKm, femaleKm,
              activityProbability, activityProbability));
        }
        activityDetails.put(groupId, details);
      }

      log.info("Successfully loaded factored customer structures");
    }
    catch (Exception e) {
      log.error("Error loading SocialClasses from : " + configResource);
      log.error(e.toString());
    }

    return activityDetails;
  }

  /**
   * @Override @code{NewTariffListener}
   **/
  public void publishNewTariffs (List<Tariff> tariffs)
  {
    List<Tariff> publishedTariffs =
        tariffMarketService.getActiveTariffList(PowerType.ELECTRIC_VEHICLE);
    publishedTariffs.addAll(
       tariffMarketService.getActiveTariffList(PowerType.CONSUMPTION));

    for (EvSocialClass evSocialClass : evSocialClassList) {
      for (String type: evSocialClass.getEvSubscriptionMap().keySet()) {
        log.debug("Evaluation of social class " + evSocialClass.toString());
        evSocialClass.possibilityEvaluationNewTariffs(publishedTariffs, type);
      }
    }
  }

  /**
   * @Override @code{TimeslotPhaseProcessor}
   */
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
  public void setDefaults ()
  {

  }
}