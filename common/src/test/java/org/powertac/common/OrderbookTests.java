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
package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.StringWriter;
import java.util.SortedSet;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

/**
 * Tests both Orderbook and OrderbookEntry
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class OrderbookTests
{
  @Autowired
  private TimeslotRepo timeslotRepo;

  private Competition competition;
  private Timeslot timeslot;
  private Instant now;

  @AfterAll
  public static void saveLogs () throws Exception
  {
    File state = new File("log/test.state");
    state.renameTo(new File("log/OrderbookTests.state"));
    File trace = new File("log/test.trace");
    trace.renameTo(new File("log/OrderbookTests.trace"));
  }

  @BeforeEach
  public void setUp () throws Exception
  {
    timeslotRepo.recycle();
    competition = Competition.newInstance("market order test");
    now = ZonedDateTime.of(2011, 10, 10, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
    timeslotRepo.makeTimeslot(now);
    timeslot = timeslotRepo.makeTimeslot(now.plusMillis(competition.getTimeslotDuration()));
  }

  @Test
  public void testOrderbookEmpty ()
  {
    Orderbook ob = new Orderbook(timeslot, null, now);
    assertNotNull(ob, "non-null orderbook");
    assertEquals(timeslot, ob.getTimeslot(), "correct timeslot");
    assertEquals(now, ob.getDateExecuted(), "correct time");
    assertNull(ob.getClearingPrice(), "null clearing price");
  }

  @Test
  public void testOrderbook ()
  {
    Orderbook ob = new Orderbook(timeslot, 22.1, now);
    assertNotNull(ob, "non-null orderbook");
    assertEquals(timeslot, ob.getTimeslot(), "correct timeslot");
    assertEquals(now, ob.getDateExecuted(), "correct time");
    assertEquals(22.1, ob.getClearingPrice(), 1e-6, "correct clearing price");
  }

  @Test
  public void testGetBids ()
  {
    Orderbook ob = new Orderbook(timeslot, 20.1, now);
    ob.addBid(new OrderbookOrder(3.3, -20.0))
      .addBid(new OrderbookOrder(2.1, -18.2))
      .addBid(new OrderbookOrder(5.6, -19.4))
      .addBid(new OrderbookOrder(6.2, null));
    SortedSet<OrderbookOrder> bids = ob.getBids();
    assertEquals(4, bids.size(), "correct number");
    assertNull(bids.first().getLimitPrice(), "correct first item price");
    assertEquals(-18.2, bids.last().getLimitPrice(), 1e-6, "correct last item price");
    SortedSet<OrderbookOrder> asks = ob.getAsks();
    assertEquals(0, asks.size(), "no asks");
  }

  @Test
  public void testGetAsks ()
  {
    Orderbook ob = new Orderbook(timeslot, 20.1, now);
    ob.addAsk(new OrderbookOrder(-3.3, 24.0))
      .addAsk(new OrderbookOrder(-2.1, 20.2))
      .addAsk(new OrderbookOrder(-5.6, 22.4))
      .addAsk(new OrderbookOrder(-6.2, null));
    SortedSet<OrderbookOrder> asks = ob.getAsks();
    assertEquals(4, asks.size(), "correct number");
    assertEquals(0, ob.getBids().size(), "no bids");
    assertNull(asks.first().getLimitPrice(), "correct first");
    assertEquals(24.0, asks.last().getLimitPrice(), 1e-6, "correct last");
  }

  @Test
  public void xmlSerializationTest ()
  {
    Orderbook ob1 = new Orderbook(timeslot, 22.1, now)
      .addBid(new OrderbookOrder(3.3, -20.0))
      .addBid(new OrderbookOrder(2.1, -18.2))
      .addBid(new OrderbookOrder(5.6, -19.4))
      .addBid(new OrderbookOrder(6.2, null))
      .addAsk(new OrderbookOrder(-3.1, 23.4));
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Orderbook.class);
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ob1));
    //System.out.println(serialized.toString());
    
    Orderbook xob1 = (Orderbook)xstream.fromXML(serialized.toString());
    assertNotNull(xob1, "deserialized something");
    assertEquals(timeslot, xob1.getTimeslot(), "correct timeslot");
    assertEquals(22.1, xob1.getClearingPrice(), 1e-6, "correct clearing price");
    assertEquals(4, xob1.getBids().size(), "four bids");
    assertEquals(1, xob1.getAsks().size(), "one ask");
  }

  @Test
  public void xmlSerializationTestEmpty ()
  {
    Orderbook ob1 = new Orderbook(timeslot, 22.1, now);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Orderbook.class);
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ob1));
    //System.out.println(serialized.toString());
    
    Orderbook xob1 = (Orderbook)xstream.fromXML(serialized.toString());
    assertNotNull(xob1, "deserialized something");
    assertEquals(timeslot, xob1.getTimeslot(), "correct timeslot");
    assertEquals(22.1, xob1.getClearingPrice(), 1e-6, "correct clearing price");
    assertNotNull(xob1.getBids().size(), "bids");
    assertNotNull(xob1.getAsks().size(), "asks");
  }
}
