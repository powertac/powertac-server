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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.genco.Genco;

/**
 * @author jcollins
 */
//@SpringJUnitConfig(locations = {"classpath:cc-config.xml"})
//@DirtiesContext
//@TestExecutionListeners(listeners = {
//  DependencyInjectionTestExecutionListener.class,
//  DirtiesContextTestExecutionListener.class
//})
public class ServerPropertiesTest
{
  private ServerPropertiesService serverPropertiesService;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    serverPropertiesService = new ServerPropertiesService();
    serverPropertiesService.recycle();
    serverPropertiesService.allowTestPaths();
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#getProperty(java.lang.String)}.
   */
  @Test
  public void testGetPropertyString ()
  {
    assertEquals(serverPropertiesService.getProperty("server.prop1"), "foo");
    assertEquals(serverPropertiesService.getProperty("test.prop1"), "bar");
    assertNull(serverPropertiesService.getProperty("foo.bar"));
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#getProperty(java.lang.String, java.lang.String)}.
   */
  @Test
  public void testGetPropertyStringString ()
  {
    assertEquals(serverPropertiesService.getProperty("foo.bar", "xyzzy"), "xyzzy");
    assertEquals(serverPropertiesService.getProperty("server.prop1", "foo"), "foo");
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#setUserConfig(java.net.URL)}.
   */
  @Test
  public void testSetUserConfigLast ()
  {
    assertEquals(serverPropertiesService.getProperty("server.prop1"), "foo");
    assertNull(serverPropertiesService.getProperty("test.prop2"));
    try {
      serverPropertiesService.setUserConfig(new URL("file:src/test/propfiles/server.properties"));
    }
    catch (ConfigurationException e) {
      fail(e.toString());
    }
    catch (IOException e) {
      fail(e.toString());
    }
    assertEquals(serverPropertiesService.getProperty("server.prop1"), "foo");
    assertEquals(serverPropertiesService.getProperty("test.prop2"), "bag");
    // when you set the user config after initialization, it's last priority
    assertEquals(serverPropertiesService.getProperty("test.prop1"), "bar");
  }

  @Test
  public void testSetUserConfigFirst ()
  {
    try {
      serverPropertiesService.setUserConfig(new URL("file:src/test/propfiles/server.properties"));
    }
    catch (ConfigurationException e) {
      fail(e.toString());
    }
    catch (IOException e) {
      fail(e.toString());
    }
    assertEquals(serverPropertiesService.getProperty("server.prop1"), "foo");
    assertEquals(serverPropertiesService.getProperty("test.prop2"), "bag");
    // when you set user config first, it's top priority
    assertEquals(serverPropertiesService.getProperty("test.prop1"), "baz");
  }

  /**
   * Test Integer property
   */
  @Test
  public void testIntegerProperty ()
  {
    assertEquals(42, (int)serverPropertiesService.getIntegerProperty("server.int",36));
    assertEquals(42.42,
                 (double)serverPropertiesService.getDoubleProperty("server.double", 36.36),
                 1e-6);
    assertEquals(42, (int)serverPropertiesService.getIntegerProperty("server.intCopy", 36));
    assertEquals(36, (int)serverPropertiesService.getIntegerProperty("server.missing", 36));
    //assertEquals(36.36, (double)serverPropertiesService.getDoubleProperty("test.prop1", 36.36), 1e-6);
  }

  @Test
  public void testGencoConfig()
  {
    Collection<?> gencos = serverPropertiesService.configureInstances(Genco.class);
    assertEquals(2, gencos.size(), "2 gencos generated");
    Genco nsp1 = null;
    Genco nsp2 = null;
    for (Object item : gencos) {
      Genco genco = (Genco)item;
      if ("nsp1".equals(genco.getUsername()))
        nsp1 = genco;
      else if ("nsp2".equals(genco.getUsername()))
        nsp2 = genco;
    }
    assertNotNull(nsp1, "nsp1 created");
    assertEquals(20.0, nsp1.getNominalCapacity(), 1e-6, "nsp1 capacity");
    assertEquals(0.05, nsp1.getVariability(), 1e-6, "nsp1 variability");
    assertEquals(0.98, nsp1.getReliability(), 1e-6, "nsp1 reliability");
    assertEquals(20.0, nsp1.getCost(), 1e-6, "nsp1 cost");
    assertEquals(1.0, nsp1.getCarbonEmissionRate(), 1e-6, "nsp1 emission");
    assertEquals(8, nsp1.getCommitmentLeadtime(), "nsp1 leadtime");
    assertNotNull(nsp2, "nsp2 created");
    assertEquals(30.0, nsp2.getNominalCapacity(), 1e-6, "nsp2 capacity");
    assertEquals(0.05, nsp2.getVariability(), 1e-6, "nsp2 variability");
    assertEquals(0.97, nsp2.getReliability(), 1e-6, "nsp2 reliability");
    assertEquals(30.0, nsp2.getCost(), 1e-6, "nsp2 cost");
    assertEquals(0.95, nsp2.getCarbonEmissionRate(), 1e-6, "nsp2 emission");
    assertEquals(6, nsp2.getCommitmentLeadtime(), "nsp2 leadtime");
  }
}
