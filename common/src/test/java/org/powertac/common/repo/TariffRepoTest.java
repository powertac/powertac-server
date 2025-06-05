/*
 * Copyright (c) 2012-2014 by the original author
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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  
  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.newInstance("test");
    repo = new TariffRepo();
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

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#addSpecification(org.powertac.common.TariffSpecification)}.
   */
  @Test
  public void testAddSpecification ()
  {
    List<TariffSpecification> specs = repo.findAllTariffSpecifications();
    assertTrue(specs.isEmpty(), "initially empty");
    repo.addSpecification(spec);
    specs = repo.findAllTariffSpecifications();
    assertEquals(1, specs.size(), "one spec");
    assertEquals(spec, specs.get(0), "correct spec");
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    specs = repo.findAllTariffSpecifications();
    assertEquals(2, specs.size(), "two specs");
    assertTrue(specs.contains(spec), "contains first");
    assertTrue(specs.contains(spec2), "contains second");
  }

  @Test
  public void testRemoveSpecification ()
  {
    List<TariffSpecification> specs = repo.findAllTariffSpecifications();
    assertTrue(specs.isEmpty(), "initially empty");
    repo.addSpecification(spec);
    specs = repo.findAllTariffSpecifications();
    assertEquals(1, specs.size(), "one spec");
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    specs = repo.findAllTariffSpecifications();
    assertEquals(2, specs.size(), "two specs");
    assertTrue(specs.contains(spec), "contains first");
    assertTrue(specs.contains(spec2), "contains second");
    repo.removeSpecification(spec.getId());
    specs = repo.findAllTariffSpecifications();    
    assertEquals(1, specs.size(), "one spec");
    assertTrue(specs.contains(spec2), "contains only second");
    repo.removeSpecification(spec2.getId());
    specs = repo.findAllTariffSpecifications();
    assertEquals(0, specs.size(), "no specs");
  }

  // remove and re-add specification
  @Test
  public void removeReaddSpec ()
  {
    repo.addSpecification(spec);
    List<TariffSpecification> specs = repo.findAllTariffSpecifications();
    assertEquals(1, specs.size(), "one spec");
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    specs = repo.findAllTariffSpecifications();
    assertEquals(2, specs.size(), "two specs");
    assertTrue(specs.contains(spec), "contains first");
    assertTrue(specs.contains(spec2), "contains second");
    repo.removeSpecification(spec.getId());
    specs = repo.findAllTariffSpecifications();
    assertEquals(1, specs.size(), "one spec");
    assertTrue(specs.contains(spec2), "contains only second");
    repo.addSpecification(spec);
    specs = repo.findAllTariffSpecifications();
    assertEquals(2, specs.size(), "back to 2 specs");
    specs = repo.findAllTariffSpecifications();
    assertTrue(specs.contains(spec2), "still contains second");
    assertTrue(specs.contains(spec), "now contains first");
  }

  @Test
  public void testAddDuplicateSpecification ()
  {
    repo.addSpecification(spec);
    List<TariffSpecification> specs = repo.findAllTariffSpecifications();
    assertEquals(1, specs.size(), "one spec");
    assertEquals(spec, specs.get(0), "correct spec");
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    specs = repo.findAllTariffSpecifications();
    assertEquals(2, specs.size(), "two specs");
    repo.addSpecification(spec2);
    specs = repo.findAllTariffSpecifications();
    assertEquals(2, specs.size(), "two specs");
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
    assertEquals(spec, repo.findSpecificationById(spec.getId()), "first");
    assertEquals(spec2, repo.findSpecificationById(spec2.getId()), "second");
  }
  
  // find spec by power type, including supertypes
  @Test
  public void testFindByPowerType ()
  {
    repo.addSpecification(spec);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    TariffSpecification spec3 = new TariffSpecification(broker, PowerType.PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec3);
    TariffSpecification spec4 = new TariffSpecification(broker, PowerType.SOLAR_PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec4);
    List<TariffSpecification> specs =
            repo.findTariffSpecificationsByPowerType(PowerType.CONSUMPTION);
    assertEquals(2, specs.size(), "two specs");
    assertTrue(specs.contains(spec), "contains first");
    assertTrue(specs.contains(spec2), "contains second");
    specs = repo.findTariffSpecificationsByPowerType(PowerType.PRODUCTION);
    assertEquals(2, specs.size(), "two specs");
    assertTrue(specs.contains(spec3), "contains first");
    assertTrue(specs.contains(spec4), "contains second");
  }

  // find spec by broker
  @Test
  public void testFindByBroker ()
  {
    repo.addSpecification(spec);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    Broker mary = new Broker("Mary");
    TariffSpecification spec3 = new TariffSpecification(mary, PowerType.PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec3);
    TariffSpecification spec4 = new TariffSpecification(mary, PowerType.SOLAR_PRODUCTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec4);
    List<TariffSpecification> specs =
            repo.findTariffSpecificationsByBroker(broker);
    assertEquals(2, specs.size(), "two specs");
    assertTrue(specs.contains(spec), "contains first");
    assertTrue(specs.contains(spec2), "contains second");
    specs = repo.findTariffSpecificationsByBroker(mary);
    assertEquals(2, specs.size(), "two specs");
    assertTrue(specs.contains(spec3), "contains first");
    assertTrue(specs.contains(spec4), "contains second");
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
    assertEquals(2, tariffs.size(), "tariffs inserted");
    assertTrue(tariffs.contains(t1), "contains first");
    assertTrue(tariffs.contains(t2), "contains second");
  }

  // make sure we don't get null for broker with no tariffs (#813)
  @Test
  public void testTariffByBroker ()
  {
    List<Tariff> tariffs = repo.findTariffsByBroker(broker);
    assertNotNull(tariffs, "should be non-null");
    assertEquals(0, tariffs.size(), "should be empty list");
    repo.addSpecification(spec);
    Tariff t1 = new Tariff(spec);
    repo.addTariff(t1);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION)
      .withMinDuration(TimeService.WEEK * 10)
      .addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2);
    Tariff t2 = new Tariff(spec2);
    repo.addTariff(t2);
    tariffs = repo.findTariffsByBroker(broker);
    assertEquals(2, tariffs.size(), "tariffs inserted");
    assertTrue(tariffs.contains(t1), "contains first");
    assertTrue(tariffs.contains(t2), "contains second");
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
      .addRate(new Rate().withValue(-0.2));
    repo.addSpecification(spec2);
    Tariff t2 = new Tariff(spec2);
    repo.addTariff(t2);
    assertEquals(t1, repo.findTariffById(spec.getId()), "found t1");
    assertEquals(t2, repo.findTariffById(spec2.getId()), "found t2");
    assertEquals(t1, repo.findTariffById(t1.getId()), "found t1");
    assertEquals(t2, repo.findTariffById(t2.getId()), "found t2");
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
    assertEquals(2, tariffs.size(), "two pending tariffs");
    t2.setState(Tariff.State.WITHDRAWN);
    tariffs = repo.findTariffsByState(Tariff.State.PENDING);
    assertEquals(1, tariffs.size(), "just one pending");
    assertEquals(t1, tariffs.get(0), "it's t1");
    tariffs = repo.findTariffsByState(Tariff.State.WITHDRAWN);
    assertEquals(1, tariffs.size(), "one withdrawn");
    assertEquals(t2, tariffs.get(0), "it's t2");
    assertTrue(repo.findTariffsByState(Tariff.State.OFFERED).isEmpty(), "none offered");
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
    assertTrue(tariffs.isEmpty(), "no active consumption tariffs");
    tariffs = repo.findActiveTariffs(PowerType.PRODUCTION);
    assertTrue(tariffs.isEmpty(), "no active production tariffs");
    tariffs = repo.findAllActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertTrue(tariffs.isEmpty(), "no active solar production tariffs");
    t2.setState(Tariff.State.ACTIVE);
    tariffs = repo.findActiveTariffs(PowerType.PRODUCTION);
    assertEquals(1, tariffs.size(), "1 active production tariffs");
    assertEquals(t2, tariffs.get(0), "it's t2");
    tariffs = repo.findActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertTrue(tariffs.isEmpty(), "no active solar production tariffs");
    tariffs = repo.findAllActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertEquals(1, tariffs.size(), "1 active generic production tariffs");
    assertEquals(t2, tariffs.get(0), "it's t2");
    t3.setState(Tariff.State.ACTIVE);
    tariffs = repo.findActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertEquals(1, tariffs.size(), "1 active solar production tariffs");
    assertEquals(t3, tariffs.get(0), "it's t3");
    tariffs = repo.findAllActiveTariffs(PowerType.SOLAR_PRODUCTION);
    assertEquals(2, tariffs.size(), "2 active generic production tariffs");
    assertTrue(tariffs.contains(t2), "includes t2");
    assertTrue(tariffs.contains(t3), "includes t3");
  }

  /**
   * Test method for {@link org.powertac.common.repo.TariffRepo#findRecentActiveTariffs(org.powertac.common.enumerations.PowerType)}.
   */
  @Test
  public void testFindRecentActiveTariffs ()
  {
    // naming convention: broker-pt-index
    repo.addSpecification(spec);
    Tariff t1c1 = new Tariff(spec);
    repo.addTariff(t1c1);
    // production
    TariffSpecification spec1p1 =
            new TariffSpecification(broker, PowerType.PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec1p1);
    Tariff t1p1 = new Tariff(spec1p1);
    repo.addTariff(t1p1);
    TariffSpecification spec1p2 =
            new TariffSpecification(broker, PowerType.PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            addRate(new Rate().withValue(0.19));
    repo.addSpecification(spec1p2);
    Tariff t1p2 = new Tariff(spec1p2);
    repo.addTariff(t1p2);
    //solar production
    TariffSpecification spec1sp1 =
            new TariffSpecification(broker, PowerType.SOLAR_PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec1sp1);
    Tariff t1sp1 = new Tariff(spec1sp1);
    repo.addTariff(t1sp1);
    TariffSpecification spec1sp2 =
            new TariffSpecification(broker, PowerType.SOLAR_PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec1sp2);
    Tariff t1sp2 = new Tariff(spec1sp2);
    repo.addTariff(t1sp2);
    TariffSpecification spec1sp3 =
            new TariffSpecification(broker, PowerType.SOLAR_PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec1sp3);
    Tariff t1sp3 = new Tariff(spec1sp3);
    repo.addTariff(t1sp3);

    Broker broker2 = new Broker("Mary");
    // production
    TariffSpecification spec2p1 =
            new TariffSpecification(broker2, PowerType.PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            withPeriodicPayment(1.0).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2p1);
    Tariff t2p1 = new Tariff(spec2p1);
    repo.addTariff(t2p1);
    TariffSpecification spec2p2 =
            new TariffSpecification(broker2, PowerType.PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            withPeriodicPayment(2.0).
            addRate(new Rate().withValue(0.19));
    repo.addSpecification(spec2p2);
    Tariff t2p2 = new Tariff(spec2p2);
    repo.addTariff(t2p2);
    TariffSpecification spec2p3 =
            new TariffSpecification(broker2, PowerType.PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            withPeriodicPayment(3.0).
            addRate(new Rate().withValue(0.19));
    repo.addSpecification(spec2p3);
    Tariff t2p3 = new Tariff(spec2p3);
    repo.addTariff(t2p3);
    // solar production
    TariffSpecification spec2sp1 =
            new TariffSpecification(broker2, PowerType.SOLAR_PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            withPeriodicPayment(1.0).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2sp1);
    Tariff t2sp1 = new Tariff(spec2sp1);
    repo.addTariff(t2sp1);
    TariffSpecification spec2sp2 =
            new TariffSpecification(broker2, PowerType.SOLAR_PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            withPeriodicPayment(2.0).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2sp2);
    Tariff t2sp2 = new Tariff(spec2sp2);
    repo.addTariff(t2sp2);
    TariffSpecification spec2sp3 =
            new TariffSpecification(broker2, PowerType.SOLAR_PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            withPeriodicPayment(3.0).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2sp3);
    Tariff t2sp3 = new Tariff(spec2sp3);
    repo.addTariff(t2sp3);
    // wind production
    TariffSpecification spec2wp1 =
            new TariffSpecification(broker2, PowerType.WIND_PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            withPeriodicPayment(1.0).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2wp1);
    Tariff t2wp1 = new Tariff(spec2wp1);
    repo.addTariff(t2wp1);
    TariffSpecification spec2wp2 =
            new TariffSpecification(broker2, PowerType.WIND_PRODUCTION).
            withMinDuration(TimeService.WEEK * 10).
            withPeriodicPayment(2.0).
            addRate(new Rate().withValue(0.2));
    repo.addSpecification(spec2wp2);
    Tariff t2wp2 = new Tariff(spec2wp2);
    repo.addTariff(t2wp2);

    List<Tariff> tariffs = repo.findRecentActiveTariffs(2, PowerType.CONSUMPTION);
    assertTrue(tariffs.isEmpty(), "no active consumption tariffs");
    tariffs = repo.findRecentActiveTariffs(1, PowerType.PRODUCTION);
    assertTrue(tariffs.isEmpty(), "no active production tariffs");
    
    t1p2.setState(Tariff.State.ACTIVE);
    tariffs = repo.findRecentActiveTariffs(2, PowerType.PRODUCTION);
    assertEquals(1, tariffs.size(), "1 active production tariffs");
    assertEquals(t1p2, tariffs.get(0), "it's 1p2");
    tariffs = repo.findRecentActiveTariffs(3, PowerType.SOLAR_PRODUCTION);
    assertEquals(1, tariffs.size(), "1 active generic production tariffs");
    assertEquals(t1p2, tariffs.get(0), "it's t1p2");
    
    t1p1.setState(Tariff.State.ACTIVE);
    t1sp1.setState(Tariff.State.ACTIVE);
    t1sp2.setState(Tariff.State.ACTIVE);
    t1sp3.setState(Tariff.State.ACTIVE);
    tariffs = repo.findRecentActiveTariffs(1, PowerType.PRODUCTION);
    assertEquals(1, tariffs.size(), "1 active production tariffs");
    assertEquals(t1p2, tariffs.get(0), "it's t1p2");
    tariffs = repo.findRecentActiveTariffs(2, PowerType.PRODUCTION);
    assertEquals(2, tariffs.size(), "2 active production tariffs");
    assertTrue(tariffs.contains(t1p2), "contains t1p2");
    assertTrue(tariffs.contains(t1p2), "contains t1p1");
    
    tariffs = repo.findRecentActiveTariffs(1, PowerType.SOLAR_PRODUCTION);
    assertEquals(2, tariffs.size(), "2 active solar production tariffs");
    assertTrue(tariffs.contains(t1p2), "includes t1p2");
    assertTrue(tariffs.contains(t1sp3), "includes t1sp3");
    tariffs = repo.findRecentActiveTariffs(2, PowerType.SOLAR_PRODUCTION);
    assertEquals(4, tariffs.size(), "4 active solar production tariffs");
    assertTrue(tariffs.contains(t1p1), "includes t1p1");
    assertTrue(tariffs.contains(t1p2), "includes t1p2");
    assertTrue(tariffs.contains(t1sp2), "includes t1sp2");
    assertTrue(tariffs.contains(t1sp3), "includes t1sp3");
    tariffs = repo.findRecentActiveTariffs(3, PowerType.SOLAR_PRODUCTION);
    assertEquals(5, tariffs.size(), "5 active solar production tariffs");
    assertTrue(tariffs.contains(t1p1), "includes t1p1");
    assertTrue(tariffs.contains(t1p2), "includes t1p2");
    assertTrue(tariffs.contains(t1sp1), "includes t1sp1");
    assertTrue(tariffs.contains(t1sp2), "includes t1sp2");
    assertTrue(tariffs.contains(t1sp3), "includes t1sp2");
    
    t2p1.setState(Tariff.State.ACTIVE);
    t2p2.setState(Tariff.State.ACTIVE);
    t2p3.setState(Tariff.State.ACTIVE);
    t2sp1.setState(Tariff.State.ACTIVE);
    t2sp2.setState(Tariff.State.ACTIVE);
    t2sp3.setState(Tariff.State.ACTIVE);
    t2wp1.setState(Tariff.State.ACTIVE);
    t2wp2.setState(Tariff.State.ACTIVE);
    tariffs = repo.findRecentActiveTariffs(1, PowerType.PRODUCTION);
    assertEquals(2, tariffs.size(), "2 active production tariffs");
    assertTrue(tariffs.contains(t1p2), "contains t1p2");
    assertTrue(tariffs.contains(t2p3), "contains t2p3");
    tariffs = repo.findRecentActiveTariffs(2, PowerType.PRODUCTION);
    assertEquals(4, tariffs.size(), "4 active production tariffs");
    assertTrue(tariffs.contains(t1p2), "contains t1p2");
    assertTrue(tariffs.contains(t1p1), "contains t1p1");
    assertTrue(tariffs.contains(t2p3), "contains t2p3");
    assertTrue(tariffs.contains(t2p2), "contains t2p2");
    
    tariffs = repo.findRecentActiveTariffs(1, PowerType.SOLAR_PRODUCTION);
    assertEquals(4, tariffs.size(), "4 active solar production tariffs");
    assertTrue(tariffs.contains(t1p2), "includes t1p2");
    assertTrue(tariffs.contains(t1sp3), "includes t1sp3");
    assertTrue(tariffs.contains(t2p3), "includes t2p3");
    assertTrue(tariffs.contains(t2sp3), "includes t2sp3");
    tariffs = repo.findRecentActiveTariffs(2, PowerType.SOLAR_PRODUCTION);
    assertEquals(8, tariffs.size(), "8 active solar production tariffs");
    tariffs = repo.findRecentActiveTariffs(3, PowerType.SOLAR_PRODUCTION);
    assertEquals(11, tariffs.size(), "11 active solar production tariffs");
    tariffs = repo.findRecentActiveTariffs(3, PowerType.WIND_PRODUCTION);
    assertEquals(7, tariffs.size(), "7 active wind production tariffs");
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
    assertEquals(rate, repo.findRateById(rate.getId()), "found r1");
    assertEquals(r2, repo.findRateById(r2.getId()), "found r2");
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
    assertEquals(3, repo.findAllTariffs().size(), "3 tariffs");
    assertEquals(3, repo.findAllTariffSpecifications().size(), "3 specs");
    repo.removeTariff(t2);
    assertEquals(2, repo.findAllTariffs().size(), "2 tariffs");
    assertEquals(2, repo.findAllTariffSpecifications().size(), "2 specs");
    assertFalse(repo.isRemoved(t3.getId()), "t3 not deleted");
    assertTrue(repo.isRemoved(t2.getId()), "t2 is deleted");
  }

  // tariff deletion without keeping a copy around
  @Test
  public void testDeleteTariff ()
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
    assertEquals(2, repo.findAllTariffs().size(), "2 tariffs");
    assertEquals(2, repo.findTariffsByBroker(broker).size(), "2 tariffs for broker");
    repo.deleteTariff(t1);
    assertEquals(1, repo.findAllTariffs().size(), "1 tariff");
    assertEquals(1, repo.findTariffsByBroker(broker).size(), "1 tariff for broker");
    repo.addTariff(t1);
    assertEquals(2, repo.findAllTariffs().size(), "2 tariffs again");
    assertEquals(2, repo.findTariffsByBroker(broker).size(), "2 tariffs for broker");
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
