/*
 * Copyright (c) 2014 by Konstantina Valogiani, Govert Buijs, and John Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.powertac.evcustomer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Singleton configuration class shared by all the ev-customer types
 * @author Konstantina Valogiani, Govert Buijs, and John Collins
 */
public final class Config
{
  static private Logger log =
      Logger.getLogger(Config.class.getName());

  // Singleton instance
  private static Config instance = null;

  // System configurator
  private ServerConfiguration serverConfiguration;

  // TODO These were originally copied from household-customer
  private double epsilon = 2.7;
  private double lambda = 20;
  //public double PERCENTAGE = 100;
  //public long MEAN_TARIFF_DURATION = 7;
  //public int HOURS_OF_DAY = 24;
  //public int DAYS_OF_WEEK = 7;
  //public int DAYS_OF_BOOTSTRAP = 14;

  @ConfigurableValue(valueType = "Double",
      description = "Aversion to TOU tariff")
  public double touFactor = 0.05;

  @ConfigurableValue(valueType = "Double",
      description = "Aversion to tariffs involving curtailment")
  public double interruptibilityFactor = 0.5;

  @ConfigurableValue(valueType = "Double",
      description = "Aversion to variable-price tariffs")
  public double variablePricingFactor = 0.7;

  @ConfigurableValue(valueType = "Double",
      description = "Aversion to tiered rates")
  public double tieredRateFactor = 0.1;
  //public int MIN_DEFAULT_DURATION = 1;
  //public int MAX_DEFAULT_DURATION = 3;
  //public int DEFAULT_DURATION_WINDOW = MAX_DEFAULT_DURATION -
  //    MIN_DEFAULT_DURATION;

  @ConfigurableValue(valueType = "Double",
      description = "Rationality of these customers")
  public double rationalityFactor = 0.9;
  //public int TARIFF_COUNT = 5;

  @ConfigurableValue(valueType = "Double",
      description = "Aversion to switching brokers")
  public double brokerSwitchFactor = 0.02;

  @ConfigurableValue(valueType = "Double",
      description = "Importance of inconvenience factors")
  public double weightInconvenience = 1.0;

  @ConfigurableValue(valueType = "Double",
      description = "Tariff evaluation inertia")
  public double nsInertia = 0.9;

  private static String[] beanTypes =
    {"SocialGroup", "CarType", "Activity", "ActivityDetail"};
//
//  @ConfigurableValue(valueType = "List",
//      description = "List of social groups")
//  public List<?> socialGroups;
//
//  @ConfigurableValue(valueType = "Configuration",
//      description = "Vehicle type information")
//  public Configuration carTypes;
//
//  @ConfigurableValue(valueType = "List",
//      description = "List of activity types")
//  public List<?> activities;
//
//  @ConfigurableValue(valueType = "Configuration",
//      description = "Activity details by group")
//  public Configuration activityDetails;

  private Map<String, Collection<?>> beans;

  // Singleton constructor
  private Config ()
  {
    super();
  }

  /**
   * Configures this singleton when it's needed. Not called during instance
   * creation to allow testing without a full Spring setup.
   */
  public void configure ()
  {
    if (null == serverConfiguration) {
      serverConfiguration = (ServerConfiguration)
          SpringApplicationContext.getBean("serverPropertiesService");
    }
    if (null == serverConfiguration) {
      // should not happen outside of testing
      log.warn("Cannot find serverPropertiesService");
    }
    else {
      serverConfiguration.configureMe(this);
    }
  }

  /**
   * Retrieves the list of configured beans
   */
  public Map<String, Collection<?>> getBeans ()
  {
    if (null != beans)
      // already configured
      return beans;
    configure();
    beans = new HashMap<String, Collection<?>>();
    for (String classname : beanTypes) {
      Class<?> clazz;
      try {
        clazz = Class.forName("org.powertac.evcustomer.beans." + classname);
        Collection<?> list = serverConfiguration.configureInstances(clazz);
        beans.put(classname, list);
      }
      catch (ClassNotFoundException e) {
        log.error("Cannot find class " + classname);
      } 
    }
    return beans;
  }

  /**
   * Singleton accessor
   */
  public synchronized static Config getInstance ()
  {
    
    if (null == instance)
    {
      instance = new Config();
      instance.configure();
    }
    return instance;
  }

  public synchronized static void recycle ()
  {
    instance = null;
  }
}