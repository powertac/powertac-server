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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.DomainRepo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 * Tests for application context interface
 * @author jcollins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class SpringApplicationContextTests
{

  @BeforeEach
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
    assertNotNull(repo, "found the repo");
  }

  /**
   * Test method for {@link org.powertac.common.spring.SpringApplicationContext#getBeanByType(java.lang.Class)}.
   */
  @Test
  public void testGetBeanByType ()
  {
    BrokerRepo repo = SpringApplicationContext.getBeanByType(BrokerRepo.class);
    assertNotNull(repo, "found the repo");
  }

  /**
   * Test method for {@link org.powertac.common.spring.SpringApplicationContext#listBeansOfType(java.lang.Class)}.
   */
  @Test
  public void testListBeansOfType ()
  {
    List<DomainRepo> repos =
        SpringApplicationContext.listBeansOfType(DomainRepo.class);
    assertEquals(7, repos.size(), "7 repos");
    // assertTrue(repos.get("brokerRepo") instanceof BrokerRepo, "type match");
  }

  @Test
  public void testMapBeansOfType ()
  {
    Map<String, DomainRepo> repos =
        SpringApplicationContext.mapBeansOfType(DomainRepo.class);
    assertEquals(7, repos.size(), "7 repos");
    assertTrue(repos.get("brokerRepo") instanceof BrokerRepo, "type match");
  }

}
