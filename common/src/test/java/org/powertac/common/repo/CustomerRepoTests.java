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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.List;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;

/**
 * @author John Collins
 */
public class CustomerRepoTests
{
  CustomerRepo repo;
  Instant start;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.newInstance("test");
    repo = new CustomerRepo();
  }

  @Test
  public void testCustomerRepo ()
  {
    assertNotNull(repo, "created a repo");
    assertEquals(0, repo.size(), "no entries");
  }

  @Test
  public void testCreate ()
  {
    CustomerInfo cust = repo.createCustomerInfo("Podunk", 42);
    assertEquals(1, repo.size(), "1 customer");
    assertEquals("Podunk", cust.getName(), "correct name");
    assertEquals(42, cust.getPopulation(), "correct population");
  }
  
  @Test
  public void testAddList ()
  {
    CustomerInfo c1 = new CustomerInfo("Podunk", 42);
    repo.add(c1);
    assertEquals(1, repo.size(), "one so far");
    Collection<CustomerInfo> customers = repo.list();
    assertEquals(1, customers.size(), "one in list");
    assertTrue(customers.contains(c1), "correct one");
    
    CustomerInfo c2 = new CustomerInfo("Nowthen", 9);
    repo.add(c2);
    assertEquals(2, repo.size(), "two");
    customers = repo.list();
    assertEquals(2, customers.size(), "two in list");
    assertTrue(customers.contains(c1), "c1 there");
    assertTrue(customers.contains(c2), "c2 there");
  }

  @Test
  public void testFindById ()
  {
    CustomerInfo c1 = new CustomerInfo("Podunk", 42);
    CustomerInfo c2 = new CustomerInfo("Nowthen", 9);
    repo.add(c1);
    repo.add(c2);
    CustomerInfo result = repo.findById(c1.getId());
    assertNotNull(result, "found something");
    assertEquals(c1, result, "found c1");
    result = repo.findById(c2.getId());
    assertNotNull(result, "found something");
    assertEquals(c2, result, "found c2");
    result = repo.findById(27182818281828l);
    assertNull(result, "nothing with that id");
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
    assertEquals(1, result.size(), "one in list");
    assertEquals(c1, result.get(0), "it's Podunk");
    result = repo.findByName("Nowthen");
    assertEquals(2, result.size(), "two in list");
    assertTrue(result.contains(c2), "Nowthen c");
    assertTrue(result.contains(c3), "Nowthen ic");
    result = repo.findByName("Somewhere");
    assertEquals(0, result.size(), "none");
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
    assertEquals(c1, result, "found c1");
    result = repo.findByNameAndPowerType("Nowthen",
                                         PowerType.CONSUMPTION);
    assertEquals(c2, result, "found c2");
    result = repo.findByNameAndPowerType("Nowthen",
                                         PowerType.INTERRUPTIBLE_CONSUMPTION);
    assertEquals(c3, result, "found c3");
    result = repo.findByNameAndPowerType("Nowthen",
                                         PowerType.PRODUCTION);
    assertNull(result, "null");
  }

  @Test
  public void testRecycle ()
  {
    repo.createCustomerInfo("Nowhere", 3);
    repo.createCustomerInfo("Nowthen", 7);
    assertEquals(2, repo.size(), "2 customers");
    repo.recycle();
    assertEquals(0, repo.size(), "none remain");
  }
}
