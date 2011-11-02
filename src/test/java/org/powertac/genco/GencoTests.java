/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.genco;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.PluginConfig;
import org.powertac.common.RandomSeed;
import org.powertac.common.Shout;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for the Genco broker type
 * @author John Collins
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"file:src/test/resources/test-config.xml"})
public class GencoTests
{
  //@Autowired
  private BrokerProxy mockProxy;
  
  //@Autowired
  private TimeslotRepo timeslotRepo;

  private Genco genco;
  private Instant start;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/log.config");
  }
  
  @Before
  public void setUp () throws Exception
  {
    Competition.newInstance("Genco test");
    mockProxy = mock(BrokerProxy.class);
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(eq(Genco.class.getName()),
                                    anyInt(),
                                    anyString())).thenReturn(seed);
    timeslotRepo = new TimeslotRepo();
    genco = new Genco("Test");
    genco.init(mockProxy, mockSeedRepo);
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
  }

  @Test
  public void testGenco ()
  {
    assertNotNull("created something", genco);
    assertEquals("correct name", "Test", genco.getUsername());
  }
  
  @Test
  public void testInit()
  {
    // it has already had init() called, should have requested a seed
    verify(mockSeedRepo).getRandomSeed(eq(Genco.class.getName()),
                                       anyInt(), eq("update"));
  }

  @Test
  public void testUpdateModel ()
  {
    when(seed.nextDouble()).thenReturn(0.5);
    PluginConfig config = new PluginConfig("Genco", "");
    genco.configure(config); // all defaults
    assertEquals("correct initial capacity",
                 100.0, genco.getCurrentCapacity(), 1e-6);
    assertTrue("initially in operation", genco.isInOperation());
    genco.updateModel(start);
    assertEquals("correct updated capacity",
                 100.0, genco.getCurrentCapacity(), 1e-6);
    assertTrue("still in operation", genco.isInOperation());
  }

  @Test
  public void testReceiveMessage ()
  {
    PluginConfig config = new PluginConfig("Genco", "");
    genco.configure(config); // all defaults
    Timeslot ts1 = timeslotRepo.makeTimeslot(start);
    Timeslot ts2 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR));
    Timeslot ts3 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 2));
    MarketPosition posn2 = new MarketPosition(genco, ts2, 0.6);
    genco.receiveMessage(posn2);
    assertNull("no position for ts1", genco.findMarketPositionByTimeslot(ts1));
    assertEquals("match for ts2", posn2, genco.findMarketPositionByTimeslot(ts2));
    assertNull("no position for ts3", genco.findMarketPositionByTimeslot(ts3));
    MarketPosition posn3 = new MarketPosition(genco, ts3, 0.6);
    genco.receiveMessage(posn3);
    assertNull("no position for ts1", genco.findMarketPositionByTimeslot(ts1));
    assertEquals("match for ts2", posn2, genco.findMarketPositionByTimeslot(ts2));
    assertEquals("match for ts3", posn3, genco.findMarketPositionByTimeslot(ts3));
  }

  @Test
  public void testGenerateShouts ()
  {
    // set up the genco
    PluginConfig config = new PluginConfig("Genco", "");
    genco.configure(config); // all defaults
    // capture shouts
    final ArrayList<Shout> shoutList = new ArrayList<Shout>(); 
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        shoutList.add((Shout)args[0]);
        return null;
      }
    }).when(mockProxy).routeMessage(isA(Shout.class));
    // set up some timeslots
    Timeslot ts1 = timeslotRepo.makeTimeslot(start);
    ts1.disable();
    Timeslot ts2 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR));
    Timeslot ts3 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR * 2));
    assertEquals("2 enabled timeslots", 2, timeslotRepo.enabledTimeslots().size());
    // 50 mwh already sold in ts2
    MarketPosition posn2 = new MarketPosition(genco, ts2, -50.0);
    genco.receiveMessage(posn2);
    // generate shouts and check
    genco.generateShouts(start, timeslotRepo.enabledTimeslots());
    assertEquals("two shouts", 2, shoutList.size());
    Shout first = shoutList.get(0);
    assertEquals("first shout for ts2", ts2, first.getTimeslot());
    assertEquals("first shout price", 1.0, first.getLimitPrice(), 1e-6);
    assertEquals("first shout for 50 mwh", 50.0, first.getMWh(), 1e-6);
    Shout second = shoutList.get(1);
    assertEquals("second shout for ts3", ts3, second.getTimeslot());
    assertEquals("second shout price", 1.0, second.getLimitPrice(), 1e-6);
    assertEquals("second shout for 100 mwh", 100.0, second.getMWh(), 1e-6);
  }

  @Test
  public void testConfigure ()
  {
    //fail("Not yet implemented");
  }

}
