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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.configuration.ConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.genco.Genco;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

/**
 * @author jcollins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:cc-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class ServerPropertiesTest
{

  @Autowired
  private ServerPropertiesService serverPropertiesService;
  
  @Before
  public void setUp () throws Exception
  {
    serverPropertiesService.recycle();
    serverPropertiesService.allowTestPaths();
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#getProperty(java.lang.String)}.
   */
  @Test
  public void testGetPropertyString ()
  {
    assertEquals("foo", serverPropertiesService.getProperty("server.prop1"));
    assertEquals("bar", serverPropertiesService.getProperty("test.prop1"));
    assertNull(serverPropertiesService.getProperty("foo.bar"));
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#getProperty(java.lang.String, java.lang.String)}.
   */
  @Test
  public void testGetPropertyStringString ()
  {
    assertEquals("xyzzy", serverPropertiesService.getProperty("foo.bar", "xyzzy"));
    assertEquals("foo", serverPropertiesService.getProperty("server.prop1", "xyzzy"));
  }

  /**
   * Test method for {@link org.powertac.server.ServerPropertiesService#setUserConfig(java.lang.String)}.
   */
  @Test
  public void testSetUserConfigLast ()
  {
    assertEquals("foo", serverPropertiesService.getProperty("server.prop1"));
    assertNull(serverPropertiesService.getProperty("test.prop2"));
    try {
      serverPropertiesService.setUserConfig(new URL("file:src/test/propfiles/server.properties"));
    }
    catch (MalformedURLException e) {
      fail(e.toString());
    }
    catch (ConfigurationException e) {
      fail(e.toString());
    }
    catch (IOException e) {
      fail(e.toString());
    }
    assertEquals("foo", serverPropertiesService.getProperty("server.prop1"));
    assertEquals("bag", serverPropertiesService.getProperty("test.prop2"));
    // when you set the user config after initialization, it's last priority
    assertEquals("bar", serverPropertiesService.getProperty("test.prop1"));
  }

  @Test
  public void testSetUserConfigFirst ()
  {
    try {
      serverPropertiesService.setUserConfig(new URL("file:src/test/propfiles/server.properties"));
    }
    catch (MalformedURLException e) {
      fail(e.toString());
    }
    catch (ConfigurationException e) {
      fail(e.toString());
    }
    catch (IOException e) {
      fail(e.toString());
    }
    assertEquals("foo", serverPropertiesService.getProperty("server.prop1"));
    assertEquals("bag", serverPropertiesService.getProperty("test.prop2"));
    // when you set user config first, it's top priority
    assertEquals("baz", serverPropertiesService.getProperty("test.prop1"));
  }

  /**
   * Test Integer property
   */
  @Test
  public void testIntegerProperty ()
  {
    assertEquals(42, (int)serverPropertiesService.getIntegerProperty("server.int", 36));
    assertEquals(42.42,
                 (double)serverPropertiesService.getDoubleProperty("server.double", 36.36),
                 1e-6);
    assertEquals(42, (int)serverPropertiesService.getIntegerProperty("server.intCopy", 36));
    assertEquals(36, (int)serverPropertiesService.getIntegerProperty("server.missing", 36));
    //assertEquals(36.36,
    //             (double)serverPropertiesService.getDoubleProperty("test.prop1", 36.36),
    //             1e-6);
  }

  @Test
  public void testGencoConfig()
  {
    Collection<?> gencos = serverPropertiesService.configureInstances(Genco.class);
    assertEquals("2 gencos generated", 2, gencos.size());
    Genco nsp1 = null;
    Genco nsp2 = null;
    for (Object item : gencos) {
      Genco genco = (Genco)item;
      if ("nsp1".equals(genco.getUsername()))
        nsp1 = genco;
      else if ("nsp2".equals(genco.getUsername()))
        nsp2 = genco;
    }
    assertNotNull("nsp1 created", nsp1);
    assertEquals("nsp1 capacity", 20.0, nsp1.getNominalCapacity(), 1e-6);
    assertEquals("nsp1 variability", 0.05, nsp1.getVariability(), 1e-6);
    assertEquals("nsp1 reliability", 0.98, nsp1.getReliability(), 1e-6);
    assertEquals("nsp1 cost", 20.0, nsp1.getCost(), 1e-6);
    assertEquals("nsp1 emission", 1.0, nsp1.getCarbonEmissionRate(), 1e-6);
    assertEquals("nsp1 leadtime", 8, nsp1.getCommitmentLeadtime());
    assertNotNull("nsp2 created", nsp2);
    assertEquals("nsp2 capacity", 30.0, nsp2.getNominalCapacity(), 1e-6);
    assertEquals("nsp2 variability", 0.05, nsp2.getVariability(), 1e-6);
    assertEquals("nsp2 reliability", 0.97, nsp2.getReliability(), 1e-6);
    assertEquals("nsp2 cost", 30.0, nsp2.getCost(), 1e-6);
    assertEquals("nsp2 emission", 0.95, nsp2.getCarbonEmissionRate(), 1e-6);
    assertEquals("nsp2 leadtime", 6, nsp2.getCommitmentLeadtime());
  }
}
