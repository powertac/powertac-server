/*
 * Copyright (c) 2011 by the original author
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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Orderbook;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.ProductType;
import org.springframework.test.util.ReflectionTestUtils;


public class OrderbookRepoTests
{
  TimeService timeService;
  TimeslotRepo timeslotRepo;

  OrderbookRepo repo;
  Instant start;
  
  @Before
  public void setUp () throws Exception
  {
    repo = new OrderbookRepo();
    timeslotRepo = new TimeslotRepo();
    timeService = new TimeService();
    start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeService.setCurrentTime(start);
    ReflectionTestUtils.setField(repo, "timeService", timeService);
  }

  @Test
  public void testOrderbookRepo ()
  {
    assertNotNull("created a repo", repo);
    assertEquals("no entries", 0, repo.size());
  }

  @Test
  public void testMakeOrderbook ()
  {
    Timeslot timeslot = timeslotRepo.makeTimeslot(start, 
                                                  start.plus(TimeService.HOUR));
    Orderbook ob = repo.makeOrderbook(timeslot, 22.0);
    assertNotNull("created orderbook", ob);
    assertEquals("correct timeslot", timeslot, ob.getTimeslot());
    assertEquals("correct clearing price", 22.0, ob.getClearingPrice(), 1e-6);
    assertEquals("correct product", ProductType.Future, ob.getProduct());
    assertEquals("correct date", start, ob.getDateExecuted());
  }

  @Test
  public void testFindByTimeslot ()
  {
    Timeslot timeslot = timeslotRepo.makeTimeslot(start, 
                                                  start.plus(TimeService.HOUR));
    Orderbook ob = repo.makeOrderbook(timeslot, 22.0);
    assertEquals("size 1", 1, repo.size());
    assertEquals("found this one", ob, repo.findByTimeslot(timeslot));
    Timeslot timeslot2 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR), 
                                                   start.plus(TimeService.HOUR * 2));
    assertNull("no orderbook yet", repo.findByTimeslot(timeslot2));
    Orderbook ob2 = repo.makeOrderbook(timeslot2, 23.0);
    assertEquals("size 2", 2, repo.size());
    assertEquals("found ob2", ob2, repo.findByTimeslot(timeslot2));
    ob = repo.makeOrderbook(timeslot, 24.0);
    assertEquals("still size 2", 2, repo.size());
    assertEquals("replaced original", ob, repo.findByTimeslot(timeslot));    
  }

  @Test
  public void testRecycle ()
  {
    Timeslot timeslot = timeslotRepo.makeTimeslot(start, 
                                                  start.plus(TimeService.HOUR));
    Timeslot timeslot2 = timeslotRepo.makeTimeslot(start.plus(TimeService.HOUR), 
                                                   start.plus(TimeService.HOUR * 2));
    Orderbook ob1 = repo.makeOrderbook(timeslot, 22.0);
    Orderbook ob2 = repo.makeOrderbook(timeslot2, 23.0);
    assertEquals("found this one", ob1, repo.findByTimeslot(timeslot));
    assertEquals("found ob2", ob2, repo.findByTimeslot(timeslot2));
    repo.recycle();
    assertNull("ts1 empty", repo.findByTimeslot(timeslot));
    assertNull("ts2 empty", repo.findByTimeslot(timeslot2));
    assertEquals("size zero", 0, repo.size());
  }

}
