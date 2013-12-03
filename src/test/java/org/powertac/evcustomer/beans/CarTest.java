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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;


/**
 * @author Govert Buijs
 * @version 0.5, Date: 2013.11.28
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class CarTest
{
  private Car car;
  private String carName = "TestCar";
  private double maxCapacity = 100.0;
  private double range = 200.0;
  private double homeCharging = 20.0;
  private double awayCharging = 10.0;

  @Before
  public void setUp ()
  {
    initialize();
  }

  @After
  public void tearDown ()
  {
    car = null;
  }

  private void initialize ()
  {
    car = new Car(carName, maxCapacity, range, homeCharging, awayCharging);
  }

  @Test
  public void testInitialization ()
  {
    assertEquals(car.getName(), carName);
    assertEquals(car.getMaxCapacity(), maxCapacity, 1E-06);
    assertEquals(car.getRange(), range, 1E-06);
    assertEquals(car.getHomeCharging(), homeCharging, 1E-06);
    assertEquals(car.getAwayCharging(), awayCharging, 1E-06);
  }

  @Test
  public void testCurrentCapacity ()
  {
    // We assume a new car has a full battery
    assertEquals(car.getCurrentCapacity(), maxCapacity, 1E-06);
  }

  @Test
  public void testDischargeValid () throws Car.ChargeException
  {
    assertEquals(car.getCurrentCapacity(), maxCapacity, 1E-06);
    car.discharge(50);
    assertEquals(car.getCurrentCapacity(), maxCapacity - 50, 1E-06);
  }

  @Test(expected = Car.ChargeException.class)
  public void testDischargeInvalid () throws Car.ChargeException
  {
    assertEquals(car.getCurrentCapacity(), maxCapacity, 1E-06);
    car.discharge(300);
    assertEquals(car.getCurrentCapacity(), maxCapacity - 300, 1E-06);
  }

  @Test
  public void testChargeValid () throws Car.ChargeException
  {
    assertEquals(car.getCurrentCapacity(), maxCapacity, 1E-06);
    car.discharge(100);
    car.charge(50);
    assertEquals(car.getCurrentCapacity(), maxCapacity - 50, 1E-06);
  }

  @Test(expected = Car.ChargeException.class)
  public void testChargeInvalid () throws Car.ChargeException
  {
    assertEquals(car.getCurrentCapacity(), maxCapacity, 1E-06);
    car.charge(50);
    assertEquals(car.getCurrentCapacity(), maxCapacity - 300, 1E-06);
  }

  @Test
  public void testNeededCapacity ()
  {
    assertEquals(car.getNeededCapacity(range), maxCapacity, 1E-06);
  }
}