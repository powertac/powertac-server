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
package org.powertac.common.spring;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.DomainRepo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for application context interface
 * @author jcollins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class SpringApplicationContextTests
{

  @Before
  public void setUp () throws Exception
  {
  }

  /**
   * Test method for {@link org.powertac.common.spring.SpringApplicationContext#getBean(java.lang.String)}.
   */
  @Test
  public void testGetBean ()
  {
    BrokerRepo repo = (BrokerRepo)SpringApplicationContext.getBean("brokerRepo");
    assertNotNull("found the repo", repo);
  }

  /**
   * Test method for {@link org.powertac.common.spring.SpringApplicationContext#getBeanByType(java.lang.Class)}.
   */
  @Test
  public void testGetBeanByType ()
  {
    BrokerRepo repo = SpringApplicationContext.getBeanByType(BrokerRepo.class);
    assertNotNull("found the repo", repo);
  }

  /**
   * Test method for {@link org.powertac.common.spring.SpringApplicationContext#listBeansOfType(java.lang.Class)}.
   */
  @Test
  public void testListBeansOfType ()
  {
    List<DomainRepo> repos =
        SpringApplicationContext.listBeansOfType(DomainRepo.class);
    assertEquals("8 repos", 8, repos.size());
    // assertTrue("type match", repos.get("brokerRepo") instanceof BrokerRepo);
  }

  @Test
  public void testMapBeansOfType ()
  {
    Map<String, DomainRepo> repos =
        SpringApplicationContext.mapBeansOfType(DomainRepo.class);
    assertEquals("8 repos", 8, repos.size());
    assertTrue("type match", repos.get("brokerRepo") instanceof BrokerRepo);
  }

}
