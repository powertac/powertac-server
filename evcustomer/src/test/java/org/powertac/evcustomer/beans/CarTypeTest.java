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

package org.powertac.evcustomer.beans;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.ServerConfiguration;


/**
 * @author Govert Buijs
 */
public class CarTypeTest
{
  private CarType carType;
  private String carName = "TestCar";
  private double maxCapacity = 100.0;
  private double range = 200.0;
  private double homeCharging = 20.0;
  private double awayCharging = 10.0;

  private SimpleConfig configSvc;
  private CompositeConfiguration config;

  @BeforeEach
  public void setUp ()
  {
    carType = new CarType(carName);
    configSvc = new SimpleConfig();
  }

  @AfterEach
  public void tearDown ()
  {
    carType = null;
  }

  private void initializeConfig ()
  {
    configSvc = new SimpleConfig();
    configSvc.initialize();
  }

  @Test
  public void testInitialization ()
  {
    carType.configure(carName, maxCapacity, range, homeCharging, awayCharging);
    assertEquals(carName, carType.getName());
    assertEquals(carType.getMaxCapacity(), maxCapacity, 1E-06);
    assertEquals(carType.getRange(), range, 1E-06);
    assertEquals(carType.getHomeChargeKW(), homeCharging, 1E-06);
    assertEquals(carType.getAwayChargeKW(), awayCharging, 1E-06);
  }

  @Test
  public void testAutoConfigure ()
  {
    initializeConfig();
    Class<?> carClass;
    try {
      carClass = Class.forName("org.powertac.evcustomer.beans.CarType");
      @SuppressWarnings("unchecked")
      Collection<CarType> cars =
          (Collection<CarType>) configSvc.configureInstances(carClass);
      assertEquals(2, cars.size(), "correct number");
      CarType result = null;
      for (CarType car : cars) {
        if (car.getName().equals("Tesla_40_kWh")) {
          result = car;
        }
      }
      assertNotNull(result, "collection contains Tesla");
      assertEquals(257.6, result.getRange(), 1e-6, "correct range");
    }
    catch (ClassNotFoundException e) {
      fail("car class not found");
    }
  }

  // ===== Config framework ========
  class SimpleConfig implements ServerConfiguration
  {
    private Configurator configurator;

    SimpleConfig ()
    {
      super();
    }

    void initialize ()
    {
      config = new CompositeConfiguration();
      configurator = new Configurator();
      try {
        config.addConfiguration(Configurator.readXML("config/test-properties.xml"));
        configurator.setConfiguration(config);
      }
      catch (Exception e) {
        e.printStackTrace();
        fail(e.toString());
      }
    }

    @Override
    public void configureMe (Object target)
    {
      configurator.configureSingleton(target);
    }

    @Override
    public Collection<?> configureInstances (Class<?> target)
    {
      return configurator.configureInstances(target);
    }

    @Override
    public void publishConfiguration (Object target)
    {
      
    }

    @Override
    public void saveBootstrapState (Object thing)
    {
      
    }

    @Override
    public Collection<?> configureNamedInstances (List<?> instances)
    {
      return null;
    }
    
  }
}