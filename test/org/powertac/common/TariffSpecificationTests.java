/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.common;

import static org.junit.Assert.*;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powertac.common.enumerations.PowerType;

public class TariffSpecificationTests
{

  private TimeService timeService;
  private Broker broker;
  private Instant now;

  @BeforeClass
  public static void setUpLog () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    timeService = new TimeService();
    timeService.setCurrentTime(new DateTime());
    now = timeService.getCurrentTime();
    broker = new Broker("Jenny");
  }

  @Test
  public void testTariffSpecification ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertNotNull("made something", spec);
    assertEquals("correct broker", broker, spec.getBroker());
    assertEquals("correct power type", PowerType.CONSUMPTION, spec.getPowerType());
    assertNull("no expiration", spec.getExpiration());
    assertEquals("default minDuration", 0l, spec.getMinDuration());
    assertEquals("default signup", 0.0, spec.getSignupPayment(), 1e-6);
    assertEquals("default withdraw", 0.0, spec.getEarlyWithdrawPayment(), 1e-6);
    assertEquals("default periodic", 0.0, spec.getPeriodicPayment(), 1e-6);
    assertEquals("no rates", 0, spec.getRates().size());
    assertNull("no supersedes", spec.getSupersedes());
  }

  @Test
  public void testSetExpiration ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    Instant exp = now.plus(TimeService.HOUR * 24);
    assertEquals("correct return", spec, spec.setExpiration(exp));
    assertEquals("correct value", exp, spec.getExpiration());
  }

  @Test
  public void testSetMinDuration ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertEquals("correct return", spec, spec.setMinDuration(10000l));
    assertEquals("correct value", 10000l, spec.getMinDuration());
  }

  @Test
  public void testSetSignupPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals("correct power type", PowerType.PRODUCTION, spec.getPowerType());
    assertEquals("correct return", spec, spec.setSignupPayment(-10.00));
    assertEquals("correct value", -10.00, spec.getSignupPayment(), 1e-6);
  }

  @Test
  public void testSetEarlyWithdrawPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals("correct return", spec, spec.setEarlyWithdrawPayment(20.00));
    assertEquals("correct value", 20.00, spec.getEarlyWithdrawPayment(), 1e-6);
  }

  @Test
  public void testSetPeriodicPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals("correct return", spec, spec.setPeriodicPayment(0.01));
    assertEquals("correct value", 0.01, spec.getPeriodicPayment(), 1e-6);
  }

  @Test
  public void testAddRate ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    Rate r = new Rate().setValue(0.121);
    assertEquals("correct return", spec, spec.addRate(r));
    assertEquals("correct length", 1, spec.getRates().size());
    assertEquals("correct rate", r, spec.getRates().get(0));
  }

  @Test
  public void testAddSupersedes ()
  {
    TariffSpecification spec1 = new TariffSpecification(broker, PowerType.CONSUMPTION);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertEquals("correct return", spec2, spec2.addSupersedes(spec1.getId()));
    assertEquals("correct length", 1, spec2.getSupersedes().size());
    assertEquals("correct first element", spec1.getId(), (long)spec2.getSupersedes().get(0));
  }

}
