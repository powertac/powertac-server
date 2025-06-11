/*
 * Copyright (c) 2011-13 by the original author
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

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Competition;
import org.powertac.common.Orderbook;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
public class OrderbookRepoTests
{
  TimeService timeService;
  TimeslotRepo timeslotRepo;

  OrderbookRepo repo;
  Instant start;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.newInstance("test");
    repo = new OrderbookRepo();
    timeslotRepo = new TimeslotRepo();
    timeService = new TimeService();
    start = ZonedDateTime.of(2011, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
    timeService.setCurrentTime(start);
    ReflectionTestUtils.setField(repo, "timeService", timeService);
  }

  @Test
  public void testOrderbookRepo ()
  {
    assertNotNull(repo, "created a repo");
    assertEquals(0, repo.size(), "no entries");
  }

  @Test
  public void testMakeOrderbook ()
  {
    Timeslot timeslot = timeslotRepo.makeTimeslot(start);
    Orderbook ob = repo.makeOrderbook(timeslot, 22.0);
    assertNotNull(ob, "created orderbook");
    assertEquals(timeslot.getSerialNumber(), ob.getTimeslotIndex(), "correct timeslot");
    assertEquals(22.0, ob.getClearingPrice(), 1e-6, "correct clearing price");
    assertEquals(start, ob.getDateExecuted(), "correct date");
  }

  @Test
  public void testFindByTimeslot ()
  {
    Timeslot timeslot = timeslotRepo.makeTimeslot(start);
    Orderbook ob = repo.makeOrderbook(timeslot, 22.0);
    assertEquals(1, repo.size(), "size 1");
    assertEquals(ob, repo.findByTimeslot(timeslot), "found this one");
    Timeslot timeslot2 = timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR));
    assertNull(repo.findByTimeslot(timeslot2), "no orderbook yet");
    Orderbook ob2 = repo.makeOrderbook(timeslot2, 23.0);
    assertEquals(2, repo.size(), "size 2");
    assertEquals(ob2, repo.findByTimeslot(timeslot2), "found ob2");
    ob = repo.makeOrderbook(timeslot, 24.0);
    assertEquals(2, repo.size(), "still size 2");
    assertEquals(ob, repo.findByTimeslot(timeslot), "replaced original");    
  }

  @Test
  public void testRecycle ()
  {
    Timeslot timeslot = timeslotRepo.makeTimeslot(start);
    Timeslot timeslot2 = timeslotRepo.makeTimeslot(start.plusMillis(TimeService.HOUR));
    Orderbook ob1 = repo.makeOrderbook(timeslot, 22.0);
    Orderbook ob2 = repo.makeOrderbook(timeslot2, 23.0);
    assertEquals(ob1, repo.findByTimeslot(timeslot), "found this one");
    assertEquals(ob2, repo.findByTimeslot(timeslot2), "found ob2");
    assertEquals(2, repo.getOrderbookCount(), "correct count");
    repo.recycle();
    assertNull(repo.findByTimeslot(timeslot), "ts1 empty");
    assertNull(repo.findByTimeslot(timeslot2), "ts2 empty");
    assertEquals(0, repo.size(), "size zero");
    assertEquals(0, repo.getOrderbookCount(), "count reset to zero");
  }

  @Test
  public void testRecycle2 ()
  {
    long start = Instant.now().toEpochMilli();
    int rate = 3600;
    timeService.setClockParameters(Instant.now().toEpochMilli(),
                                   rate, TimeService.HOUR);
    timeService.setStart(start);
    timeService.updateTime();
    repo.recycle();
    Timeslot timeslot;
    Orderbook ob;
    assertEquals(0, repo.size(), "size zero");
    assertEquals(0, repo.getOrderbookCount(), "count reset to zero");
    for (int i = 0; i < 168*4; i++) {
      // set the start back an hour to make the current time go forward
      start = start - TimeService.HOUR / rate;
      timeService.setStart(start);
      timeService.updateTime();
      //System.out.println("hour " + timeService.getHourOfDay());
      timeslot = timeslotRepo.makeTimeslot(timeService.getCurrentTime());
      ob = repo.makeOrderbook(timeslot, 42.0);
    }
    int remains = repo.size();
    assertTrue(remains < 500, "small enough");
    assertTrue(remains > 168*2+36, "large enough");
    assertEquals(remains, repo.getOrderbookCount(), "correct count");
  }
  
  @Test
  public void testMinAsk ()
  {
    Double[] minAskPrices = {1.0, 2.0, 3.0};
    repo.setMinAskPrices(minAskPrices);
    Double[] val = repo.getMinAskPrices();
    assertEquals(minAskPrices.length, val.length, "same length");
    for (int i = 0; i < minAskPrices.length; i++) {
      assertEquals(minAskPrices[i], val[i], 1e-8, "same value");
    }
    timeService.setCurrentTime(start.plusMillis(TimeService.HOUR * 2));
  }

}
