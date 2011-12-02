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

import static org.junit.Assert.*;

import java.io.File;
import java.io.StringWriter;
import java.util.SortedSet;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

/**
 * Tests both Orderbook and OrderbookEntry
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class OrderbookTests
{
  @Autowired
  private TimeslotRepo timeslotRepo;

  private Competition competition;
  private Timeslot timeslot;
  private Instant now;

  @AfterClass
  public static void saveLogs () throws Exception
  {
    File state = new File("log/test.state");
    state.renameTo(new File("log/OrderbookTests.state"));
    File trace = new File("log/test.trace");
    trace.renameTo(new File("log/OrderbookTests.trace"));
  }

  @Before
  public void setUp () throws Exception
  {
    timeslotRepo.recycle();
    competition = Competition.newInstance("market order test");
    now = new DateTime(2011, 10, 10, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeslotRepo.makeTimeslot(now);
    timeslot = timeslotRepo.makeTimeslot(now.plus(competition.getTimeslotDuration()));
  }

  @Test
  public void testOrderbookEmpty ()
  {
    Orderbook ob = new Orderbook(timeslot, null, now);
    assertNotNull("non-null orderbook", ob);
    assertEquals("correct timeslot", timeslot, ob.getTimeslot());
    assertEquals("correct time", now, ob.getDateExecuted());
    assertNull("null clearing price", ob.getClearingPrice());
  }

  @Test
  public void testOrderbook ()
  {
    Orderbook ob = new Orderbook(timeslot, 22.1, now);
    assertNotNull("non-null orderbook", ob);
    assertEquals("correct timeslot", timeslot, ob.getTimeslot());
    assertEquals("correct time", now, ob.getDateExecuted());
    assertEquals("correct clearing price", 22.1, ob.getClearingPrice(), 1e-6);
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
    assertEquals("correct number", 4, bids.size());
    assertEquals("correct first item price", null, bids.first().getLimitPrice());
    assertEquals("correct last item price", -18.2, bids.last().getLimitPrice(), 1e-6);
    SortedSet<OrderbookOrder> asks = ob.getAsks();
    assertEquals("no asks", 0, asks.size());
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
    assertEquals("correct number", 4, asks.size());
    assertEquals("no bids", 0, ob.getBids().size());
    assertNull("correct first", asks.first().getLimitPrice());
    assertEquals("correct last", 24.0, asks.last().getLimitPrice(), 1e-6);
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
    XStream xstream = new XStream();
    xstream.processAnnotations(Orderbook.class);
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ob1));
    //System.out.println(serialized.toString());
    
    Orderbook xob1 = (Orderbook)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xob1);
    assertEquals("correct timeslot", timeslot, xob1.getTimeslot());
    assertEquals("correct clearing price", 22.1, xob1.getClearingPrice(), 1e-6);
    assertEquals("four bids", 4, xob1.getBids().size());
    assertEquals("one ask", 1, xob1.getAsks().size());
  }
}
