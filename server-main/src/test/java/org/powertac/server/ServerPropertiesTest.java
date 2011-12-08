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
package org.powertac.server;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * @author jcollins
 */
public class ServerPropertiesTest
{

  private ServerPropertiesService service;
  
  @Before
  public void setUp () throws Exception
  {
    service = new ServerPropertiesService();
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#getProperty(java.lang.String)}.
   */
  @Test
  public void testGetPropertyString ()
  {
    assertEquals("foo", service.getProperty("server.prop1"));
    assertEquals("bar", service.getProperty("test.prop1"));
    assertNull(service.getProperty("foo.bar"));
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#getProperty(java.lang.String, java.lang.String)}.
   */
  @Test
  public void testGetPropertyStringString ()
  {
    assertEquals("xyzzy", service.getProperty("foo.bar", "xyzzy"));
    assertEquals("foo", service.getProperty("server.prop1", "xyzzy"));
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#setUserConfig(java.lang.String)}.
   */
  @Test
  public void testSetUserConfig ()
  {
    assertEquals("foo", service.getProperty("server.prop1"));
    assertNull(service.getProperty("test.prop2"));
    service.setUserConfig("src/test/propfiles/server.properties");
    assertEquals("foo", service.getProperty("server.prop1"));
    assertEquals("bag", service.getProperty("test.prop2"));
    assertEquals("baz", service.getProperty("test.prop1"));
  }

  /**
   * Test Integer property
   */
  @Test
  public void testIntegerProperty ()
  {
    assertEquals(42, (int)service.getIntegerProperty("server.int", 36));
    assertEquals(42.42,
                 (double)service.getDoubleProperty("server.double", 36.36),
                 1e-6);
    assertEquals(36, (int)service.getIntegerProperty("server.prop1", 36));
    assertEquals(36.36,
                 (double)service.getDoubleProperty("test.prop1", 36.36),
                 1e-6);
  }
}
