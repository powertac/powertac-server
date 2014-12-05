/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.evcustomer;

import static org.junit.Assert.*;
import static org.powertac.util.ListTools.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.evcustomer.beans.Activity;
import org.powertac.evcustomer.beans.ClassCar;
import org.powertac.util.Predicate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 */
public class ConfigTest
{
  private DummyConfig configSvc;
  private CompositeConfiguration config;

  // Unit under test
  Config uut;

  @Before
  public void setUp () throws Exception
  {
    configSvc = new DummyConfig();
    configSvc.initialize();
    Config.recycle();
  }

  /**
   * Test retrieval by key
   */
  @Test
  public void configByKey ()
  {
    double result = config.getDouble("evcustomer.config.touFactor");
    assertEquals("Correct value", 0.25, result, 1e-6);
  }

  /**
   * Test method for {@link org.powertac.evcustomer.Config#getInstance()}.
   */
  @Test
  public void testGetInstance ()
  {
    Config item = Config.getInstance();
    assertNotNull("Config created", item);
  }

  /**
   * Test property config
   */
  @Test
  public void testPropertyConfig ()
  {
    Config item = Config.getInstance();
    ReflectionTestUtils.setField(item, "serverConfiguration", configSvc);
    item.configure();
    assertEquals("ConfiguredValue", 0.25, item.getTouFactor(), 1e-6);
  }

  @Test
  public void testBeanConfig ()
  {
    Config item = Config.getInstance();
    ReflectionTestUtils.setField(item, "serverConfiguration", configSvc);
    item.configure();
    Map<String, Collection<?>> result = item.getBeans();

    // Social groups
    Collection<?> coll = result.get("SocialGroup");
    assertEquals("3 groups", 3, coll.size());

    // Activities
    ArrayList<Object> list = new ArrayList<Object>(result.get("Activity"));
    assertEquals("2 activities", 2, list.size());
    assertTrue("one of them is commuting",
               ((Activity)list.get(0)).getName().equals("commuting")
               || ((Activity)list.get(1)).getName().equals("commuting"));
    assertTrue("one of them is business_trip",
               ((Activity)list.get(0)).getName().equals("business_trip")
               || ((Activity)list.get(1)).getName().equals("business_trip"));
    int index = 0;
    if (((Activity)list.get(1)).getName().equals("commuting"))
      index = 1;
    Activity commuting = (Activity)list.get(index);
    assertEquals("correct id", 0, commuting.getId());
    assertEquals("correct weekday weight", 0.99,
                 commuting.getWeekdayWeight(), 1e-6);
  }

  @Test
  public void testClassCarConfig ()
  {
    Config item = Config.getInstance();
    ReflectionTestUtils.setField(item, "serverConfiguration", configSvc);
    item.configure();
    Map<String, Collection<?>> result = item.getBeans();

    @SuppressWarnings("unchecked")
    Collection<Object> list = (Collection<Object>) result.get("ClassCar");
    assertEquals("8 instances", 8, list.size());

    Object thing = findFirst(list, new Predicate<Object>() {
      @Override
      public boolean apply (Object thing)
      {
        return ((thing instanceof ClassCar) &&
            ((ClassCar)thing).getName().equals("HI_2_T40"));
      }
    });
    assertNotNull("found HI_2_T40", thing);
    ClassCar cc = (ClassCar)thing;
    assertEquals("correct class", "HighIncome_2", cc.getSocialClassName());
    assertEquals("correct car", "Tesla_40_kWh", cc.getCarName());
    assertEquals("correct probability", 0.7, cc.getProbability(), 1e-6);
  }

  class DummyConfig implements ServerConfiguration
  {
    private Configurator configurator;

    DummyConfig ()
    {
      super();
    }

    void initialize ()
    {
      config = new CompositeConfiguration();
      configurator = new Configurator();
      InputStream stream =
          ConfigTest.class.getResourceAsStream("/config/test-properties.xml");
      XMLConfiguration xconfig = new XMLConfiguration();
      try {
        xconfig.load(stream);
        config.addConfiguration(xconfig);
        configurator.setConfiguration(config);
      }
      catch (ConfigurationException e) {
        e.printStackTrace();
        fail(e.toString());
      }
    }

    @Override
    public void configureMe (Object target)
    {
      configurator.configureSingleton(target);
    }

    @Override
    public Collection<?> configureInstances (Class<?> target)
    {
      return configurator.configureInstances(target);
    }

    @Override
    public void publishConfiguration (Object target)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public void saveBootstrapState (Object thing)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public Collection<?> configureNamedInstances (List<?> instances)
    {
      // Auto-generated method stub
      return null;
    }
    
  }
}
