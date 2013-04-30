/*
 * Copyright (c) 2013 by John Collins
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

import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Orderbook;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.enumerations.PowerType;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
public class CustomerRepoTests
{
  CustomerRepo repo;
  Instant start;
  
  @Before
  public void setUp () throws Exception
  {
    Competition.newInstance("test");
    repo = new CustomerRepo();
  }

  @Test
  public void testCustomerRepo ()
  {
    assertNotNull("created a repo", repo);
    assertEquals("no entries", 0, repo.size());
  }

  @Test
  public void testCreate ()
  {
    CustomerInfo cust = repo.createCustomerInfo("Podunk", 42);
    assertEquals("1 customer", 1, repo.size());
    assertEquals("correct name", "Podunk", cust.getName());
    assertEquals("correct population", 42, cust.getPopulation());
  }
  
  @Test
  public void testAddList ()
  {
    CustomerInfo c1 = new CustomerInfo("Podunk", 42);
    repo.add(c1);
    assertEquals("one so far", 1, repo.size());
    Collection<CustomerInfo> customers = repo.list();
    assertEquals("one in list", 1, customers.size());
    assertTrue("correct one", customers.contains(c1));
    
    CustomerInfo c2 = new CustomerInfo("Nowthen", 9);
    repo.add(c2);
    assertEquals("two", 2, repo.size());
    customers = repo.list();
    assertEquals("two in list", 2, customers.size());
    assertTrue("c1 there", customers.contains(c1));
    assertTrue("c2 there", customers.contains(c2));
  }

  @Test
  public void testFindById ()
  {
    CustomerInfo c1 = new CustomerInfo("Podunk", 42);
    CustomerInfo c2 = new CustomerInfo("Nowthen", 9);
    repo.add(c1);
    repo.add(c2);
    CustomerInfo result = repo.findById(c1.getId());
    assertNotNull("found something", result);
    assertEquals("found c1", c1, result);
    result = repo.findById(c2.getId());
    assertNotNull("found something", result);
    assertEquals("found c2", c2, result);
    result = repo.findById(27182818281828l);
    assertNull("nothing with that id", result);
  }

  @Test
  public void testFindByName ()
  {
    CustomerInfo c1 = new CustomerInfo("Podunk", 42);
    CustomerInfo c2 = new CustomerInfo("Nowthen", 9).
            withPowerType(PowerType.CONSUMPTION);
    CustomerInfo c3 = new CustomerInfo("Nowthen", 9).
            withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
    repo.add(c1);
    repo.add(c2);
    repo.add(c3);
    
    List<CustomerInfo> result = repo.findByName("Podunk");
    assertEquals("one in list", 1, result.size());
    assertEquals("it's Podunk", c1, result.get(0));
    result = repo.findByName("Nowthen");
    assertEquals("two in list", 2, result.size());
    assertTrue("Nowthen c", result.contains(c2));
    assertTrue("Nowthen ic", result.contains(c3));
    result = repo.findByName("Somewhere");
    assertEquals("none", 0, result.size());
  }

  @Test
  public void testFindByNameAndPowerType ()
  {
    CustomerInfo c1 = new CustomerInfo("Podunk", 42).
            withPowerType(PowerType.CONSUMPTION);
    CustomerInfo c2 = new CustomerInfo("Nowthen", 9).
            withPowerType(PowerType.CONSUMPTION);
    CustomerInfo c3 = new CustomerInfo("Nowthen", 9).
            withPowerType(PowerType.INTERRUPTIBLE_CONSUMPTION);
    repo.add(c1);
    repo.add(c2);
    repo.add(c3);
    
    CustomerInfo result = repo.findByNameAndPowerType("Podunk",
                                                      PowerType.CONSUMPTION);
    assertEquals("found c1", c1, result);
    result = repo.findByNameAndPowerType("Nowthen",
                                         PowerType.CONSUMPTION);
    assertEquals("found c2", c2, result);
    result = repo.findByNameAndPowerType("Nowthen",
                                         PowerType.INTERRUPTIBLE_CONSUMPTION);
    assertEquals("found c3", c3, result);
    result = repo.findByNameAndPowerType("Nowthen",
                                         PowerType.PRODUCTION);
    assertNull("null", result);
  }

  @Test
  public void testRecycle ()
  {
    repo.createCustomerInfo("Nowhere", 3);
    repo.createCustomerInfo("Nowthen", 7);
    assertEquals("2 customers", 2, repo.size());
    repo.recycle();
    assertEquals("none remain", 0, repo.size());
  }
}
