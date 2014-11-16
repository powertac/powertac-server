/*
 * Copyright 2013 the original author or authors.
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

package org.powertac.evcustomer.beans;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


/**
 * @author Govert Buijs
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class SocialClassDetailTest
{
  private SocialClassDetail socialClassDetail;
  private String name = "Test SocialClassDetail";
  private int minCount = 1;
  private int maxCount = 100;

  private int detailId = 1;
  private double probability = 0.5;
  private double maleProbability = 0.5;
  private Map<Integer, ClassGroup> classGroups =
      new HashMap<Integer, ClassGroup>();

  @Before
  public void setUp ()
  {
    initialize();
  }

  @After
  public void tearDown ()
  {
    socialClassDetail = null;
    classGroups = null;
  }

  private void initialize ()
  {
    classGroups = new HashMap<Integer, ClassGroup>();
    ClassGroup sgd = new ClassGroup("uut");
    sgd.initialize(detailId, probability, maleProbability);
    classGroups.put(1, sgd);

    socialClassDetail =
        new SocialClassDetail(name, minCount, maxCount, classGroups);
  }

  @Test
  public void testInitialization ()
  {
    assertEquals(name,                socialClassDetail.getName());
    assertEquals(minCount,            socialClassDetail.getMinCount());
    assertEquals(maxCount,            socialClassDetail.getMaxCount());
    assertEquals(classGroups,  socialClassDetail.getSocialGroupDetails());
  }
}