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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Singleton configuration class shared by all the ev-customer types
 * @author Konstantina Valogiani, Govert Buijs, and John Collins
 */
public final class Config
{
  static private Logger log = LogManager.getLogger(Config.class.getName());

  // Singleton instance
  private static Config instance = null;

  // System configurator
  private ServerConfiguration serverConfiguration;

  // TODO These were originally copied from household-customer
  private double epsilon = 2.7;
  private double lambda = 20;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Aversion to TOU tariff")
  private double touFactor = 0.05;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Aversion to tariffs involving curtailment")
  private double interruptibilityFactor = 0.1;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Aversion to variable-price tariffs")
  private double variablePricingFactor = 0.1;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Aversion to tiered rates")
  private double tieredRateFactor = 0.1;

  @ConfigurableValue(valueType = "Integer", dump = false,
      description = "Minimum expected duration of subscription in days")
  private int minDefaultDuration = 7;

  @ConfigurableValue(valueType = "Integer", dump = false,
      description = "Maximum expected duration of subscription in days")
  private int maxDefaultDuration = 21;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Rationality of these customers")
  private double rationalityFactor = 0.9;

  @ConfigurableValue(valueType = "Integer", dump = false,
          description = "Number of tariffs/broker to consider")
  private int tariffCount = 5;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Aversion to switching brokers")
  private double brokerSwitchFactor = 0.02;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Importance of inconvenience factors")
  private double weightInconvenience = 0.2;

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Tariff evaluation inertia")
  private double nsInertia = 0.8;

  @ConfigurableValue(valueType = "Integer", dump = false,
          description = "Length in timeslots of evaluation profile")
  private int profileLength = 48;

  @ConfigurableValue(valueType = "List", dump = false,
      description = "classnames of bean types to be configured")
  private List<String> beanTypes;

  private Map<String, Collection<?>> beans;

  // Singleton constructor
  private Config ()
  {
    super();
  }

  // =================== Accessors ====================
  public double getEpsilon ()
  {
    return epsilon;
  }
  
  public double getLambda ()
  {
    return lambda;
  }
  
  public double getTouFactor ()
  {
    return touFactor;
  }
  
  public double getInterruptibilityFactor ()
  {
    return interruptibilityFactor;
  }
  
  public double getVariablePricingFactor ()
  {
    return variablePricingFactor;
  }
  
  public double getTieredRateFactor ()
  {
    return tieredRateFactor;
  }

  public int getMinDefaultDuration ()
  {
    return minDefaultDuration;
  }

  public int getMaxDefaultDuration ()
  {
    return maxDefaultDuration;
  }

  public double getRationalityFactor ()
  {
    return rationalityFactor;
  }

  public double getNsInertia ()
  {
    return nsInertia;
  }

  public double getBrokerSwitchFactor ()
  {
    return brokerSwitchFactor;
  }

  public double getWeightInconvenience ()
  {
    return weightInconvenience;
  }

  public int getTariffCount ()
  {
    return tariffCount;
  }

  public int getProfileLength ()
  {
    return profileLength;
  }

  // =================== Configuration ================
  /**
   * Configures this singleton when it's needed. Not called during instance
   * creation to allow testing without a full Spring setup. This version is
   * Deprecated! Please use configure(ServerConfiguration) instead.
   */
  @Deprecated
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
   * Configures this instance from the given configuration service.
   */
  public void configure (ServerConfiguration configSource)
  {
    serverConfiguration = configSource;
    configSource.configureMe(this);
  }

  /**
   * Retrieves the list of configured beans
   */
  public Map<String, Collection<?>> getBeans ()
  {
    if (null != beans) {
      // already configured
      return beans;
    }

    configure();
    beans = new HashMap<>();
    for (String classname : beanTypes) {
      Class<?> clazz;
      try {
        clazz = Class.forName("org.powertac.evcustomer.beans." + classname);
        Collection<?> list = serverConfiguration.configureInstances(clazz);
        beans.put(classname, list);
        log.info("Loaded " + list.size() + " instances of " + classname);
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
      //instance.configure();
    }
    return instance;
  }

  public synchronized static void recycle ()
  {
    instance = null;
  }
}