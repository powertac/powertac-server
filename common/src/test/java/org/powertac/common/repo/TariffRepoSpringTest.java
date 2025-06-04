/*
 * Copyright (c) 2013 by the original author
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
package org.powertac.common.repo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 * TariffRepo tests that need the Spring context to run. Basically, it's
 * tests that need to init Tariffs.
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class TariffRepoSpringTest
{
  //@Autowired
  //private TimeService timeService; // dependency injection

  @Autowired
  private TariffRepo repo;

  TariffSpecification spec;
  Tariff tariff;
  Broker broker;
  Rate rate;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.newInstance("test");
    repo.recycle();
    broker = new Broker("Sally");
    rate = new Rate().withValue(-0.121);
    spec = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 8)
      .addRate(rate);
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#TariffRepo()}.
   */
  @Test
  public void testTariffRepo ()
  {
    assertNotNull(repo, "repo created");
  }
  
  // set/get default tariff
  @Test
  public void testSetGetDefault ()
  {
    Tariff result = repo.getDefaultTariff(PowerType.CONSUMPTION);
    assertNull(result, "no default tariff yet");

    repo.setDefaultTariff(spec);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    result = repo.getDefaultTariff(PowerType.CONSUMPTION);
    assertEquals(spec, result.getTariffSpecification(), "correct default CONSUMPTION");
    assertNull(repo.getDefaultTariff(PowerType.PRODUCTION), "no default PRODUCTION");

    repo.setDefaultTariff(spec2);
    result = repo.getDefaultTariff(PowerType.CONSUMPTION);
    assertEquals(spec, result.getTariffSpecification(), "correct default CONSUMPTION");
    result = repo.getDefaultTariff(PowerType.PRODUCTION);
    assertEquals(spec2, result.getTariffSpecification(), "correct default PRODUCTION");
    result = repo.getDefaultTariff(PowerType.SOLAR_PRODUCTION);
    assertEquals(spec2, result.getTariffSpecification(), "correct default SOLAR_PRODUCTION");
  }
}
