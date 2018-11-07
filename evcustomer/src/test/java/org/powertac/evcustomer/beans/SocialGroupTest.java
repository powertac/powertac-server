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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * @author Govert Buijs
 */
public class SocialGroupTest
{
  private SocialGroup socialGroup;
  private int id = 1;
  private String name = "TestSocialGroup";

  @BeforeEach
  public void setUp ()
  {
    initialize();
  }

  @AfterEach
  public void tearDown ()
  {
    socialGroup = null;
  }

  private void initialize ()
  {
    socialGroup = new SocialGroup(id, name);
  }

  @Test
  public void testInitialization ()
  {
    assertEquals(id,    socialGroup.getId());
    assertEquals(name,  socialGroup.getName());
  }
}