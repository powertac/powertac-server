/*
 * Copyright (c) 2012 by the original author
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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;

/**
 * @author John Collins
 */
public class TariffRepoTest
{

  TariffRepo repo;
  TariffSpecification spec;
  Tariff tariff;
  Broker broker;
  Rate rate;
  
  @Before
  public void setUp () throws Exception
  {
    Competition.newInstance("test");
    repo = new TariffRepo();
    broker = new Broker("Sally");
    rate = new Rate().withValue(0.121);
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
    assertNotNull("repo created", repo);
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#addSpecification(org.powertac.common.TariffSpecification)}.
   */
  @Test
  public void testAddSpecification ()
  {
    List<TariffSpecification> specs = repo.findAllTariffSpecifications();
    assertTrue("initially empty", specs.isEmpty());
    repo.addSpecification(spec);
    specs = repo.findAllTariffSpecifications();
    assertEquals("one spec", 1, specs.size());
    assertEquals("correct spec", spec, specs.get(0));
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    specs = repo.findAllTariffSpecifications();
    assertEquals("two specs", 2, specs.size());
    assertTrue("contains first", specs.contains(spec));
    assertTrue("contains second", specs.contains(spec2));
  }

  @Test
  public void testAddDuplicateSpecification ()
  {
    repo.addSpecification(spec);
    List<TariffSpecification> specs = repo.findAllTariffSpecifications();
    assertEquals("one spec", 1, specs.size());
    assertEquals("correct spec", spec, specs.get(0));
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    specs = repo.findAllTariffSpecifications();
    assertEquals("two specs", 2, specs.size());
    repo.addSpecification(spec2);
    specs = repo.findAllTariffSpecifications();
    assertEquals("two specs", 2, specs.size());
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#findSpecificationById(long)}.
   */
  @Test
  public void testFindSpecificationById ()
  {
    repo.addSpecification(spec);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    assertEquals("first", spec, repo.findSpecificationById(spec.getId()));
    assertEquals("second", spec2, repo.findSpecificationById(spec2.getId()));
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#addTariff(org.powertac.common.Tariff)}.
   */
  @Test
  public void testAddTariff ()
  {
    repo.addSpecification(spec);
    Tariff t1 = new Tariff(spec);
    repo.addTariff(t1);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    Tariff t2 = new Tariff(spec2);
    repo.addTariff(t2);
    List<Tariff> tariffs = repo.findAllTariffs();
    assertEquals("tariffs inserted", 2, tariffs.size());
    assertTrue("contains first", tariffs.contains(t1));
    assertTrue("contains second", tariffs.contains(t2));
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#findTariffById(long)}.
   */
  @Test
  public void testFindTariffById ()
  {
    repo.addSpecification(spec);
    Tariff t1 = new Tariff(spec);
    repo.addTariff(t1);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    Tariff t2 = new Tariff(spec2);
    repo.addTariff(t2);
    assertEquals("found t1", t1, repo.findTariffById(spec.getId()));
    assertEquals("found t2", t2, repo.findTariffById(spec2.getId()));
    assertEquals("found t1", t1, repo.findTariffById(t1.getId()));
    assertEquals("found t2", t2, repo.findTariffById(t2.getId()));
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#findTariffsByState(org.powertac.common.Tariff.State)}.
   */
  @Test
  public void testFindTariffsByState ()
  {
    repo.addSpecification(spec);
    Tariff t1 = new Tariff(spec);
    repo.addTariff(t1);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    Tariff t2 = new Tariff(spec2);
    repo.addTariff(t2);
    List<Tariff> tariffs = repo.findTariffsByState(Tariff.State.PENDING);
    assertEquals("two pending tariffs", 2, tariffs.size());
    t2.setState(Tariff.State.WITHDRAWN);
    tariffs = repo.findTariffsByState(Tariff.State.PENDING);
    assertEquals("just one pending", 1, tariffs.size());
    assertEquals("it's t1", t1, tariffs.get(0));
    tariffs = repo.findTariffsByState(Tariff.State.WITHDRAWN);
    assertEquals("one withdrawn", 1, tariffs.size());
    assertEquals("it's t2", t2, tariffs.get(0));
    assertTrue("none offered", repo.findTariffsByState(Tariff.State.OFFERED).isEmpty());
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#findActiveTariffs(org.powertac.common.enumerations.PowerType)}.
   */
  @Test
  public void testFindActiveTariffs ()
  {
    repo.addSpecification(spec);
    Tariff t1 = new Tariff(spec);
    repo.addTariff(t1);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    Tariff t2 = new Tariff(spec2);
    repo.addTariff(t2);
    TariffSpecification spec3 = new TariffSpecification(broker, PowerType.SOLAR_PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec3);
    Tariff t3 = new Tariff(spec3);
    repo.addTariff(t3);
    List<Tariff> tariffs = repo.findActiveTariffs(PowerType.CONSUMPTION);
    assertTrue("no active consumption tariffs", tariffs.isEmpty());
    tariffs = repo.findActiveTariffs(PowerType.PRODUCTION);
    assertTrue("no active production tariffs", tariffs.isEmpty());
    tariffs = repo.findAllActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertTrue("no active solar production tariffs", tariffs.isEmpty());
    t2.setState(Tariff.State.ACTIVE);
    tariffs = repo.findActiveTariffs(PowerType.PRODUCTION);
    assertEquals("1 active production tariffs", 1, tariffs.size());
    assertEquals("it's t2", t2, tariffs.get(0));
    tariffs = repo.findActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertTrue("no active solar production tariffs", tariffs.isEmpty());
    tariffs = repo.findAllActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertEquals("1 active generic production tariffs", 1, tariffs.size());
    assertEquals("it's t2", t2, tariffs.get(0));
    t3.setState(Tariff.State.ACTIVE);
    tariffs = repo.findActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertEquals("1 active solar production tariffs", 1, tariffs.size());
    assertEquals("it's t3", t3, tariffs.get(0));
    tariffs = repo.findAllActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertEquals("2 active generic production tariffs", 2, tariffs.size());
    assertTrue("includes t2", tariffs.contains(t2));
    assertTrue("includes t3", tariffs.contains(t3));
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#findRateById(long)}.
   */
  @Test
  public void testFindRateById ()
  {
    repo.addSpecification(spec);
    Tariff t1 = new Tariff(spec);
    repo.addTariff(t1);
    Rate r2 = new Rate().withValue(0.2);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(r2);
    repo.addSpecification(spec2);
    Tariff t2 = new Tariff(spec2);
    repo.addTariff(t2);
    assertEquals("found r1", rate, repo.findRateById(rate.getId()));
    assertEquals("found r2", r2, repo.findRateById(r2.getId()));
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#removeTariff(org.powertac.common.Tariff)}.
   */
  @Test
  public void testRemoveTariff ()
  {
    repo.addSpecification(spec);
    Tariff t1 = new Tariff(spec);
    repo.addTariff(t1);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    Tariff t2 = new Tariff(spec2);
    repo.addTariff(t2);
    TariffSpecification spec3 = new TariffSpecification(broker, PowerType.SOLAR_PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec3);
    Tariff t3 = new Tariff(spec3);
    repo.addTariff(t3);
    assertEquals("3 tariffs", 3, repo.findAllTariffs().size());
    assertEquals("3 specs", 3, repo.findAllTariffSpecifications().size());
    repo.removeTariff(t2);
    assertEquals("2 tariffs", 2, repo.findAllTariffs().size());
    assertEquals("2 specs", 2, repo.findAllTariffSpecifications().size());
    assertFalse("t3 not deleted", repo.isDeleted(t3.getId()));
    assertTrue("t2 is deleted", repo.isDeleted(t2.getId()));
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#addBalancingOrder(org.powertac.common.msg.BalancingOrder)}.
   */
  @Test
  public void testAddBalancingOrder ()
  {
    //fail("Not yet implemented");
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#getBalancingOrders()}.
   */
  @Test
  public void testGetBalancingOrders ()
  {
    //fail("Not yet implemented");
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#recycle()}.
   */
  @Test
  public void testRecycle ()
  {
    //fail("Not yet implemented");
  }

}
