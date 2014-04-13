/*
 * Copyright (c) 2012-2014 by the original author
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
package org.powertac.common.config;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Competition;

import pt.ConfigTestDummy;

/**
 * Test for PowerTAC Configurator
 * @author John Collins
 */
public class ConfiguratorTest
{
  Competition comp;
  Configuration config;
  
  /**
   *
   */
  @Before
  public void setUp () throws Exception
  {
    comp = Competition.newInstance("test");
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("common.competition.timeslotLength", "15");
    map.put("common.competition.minimumTimeslotCount", "600");
    map.put("common.competition.simulationBaseTime", "2009-10-10");
    config = new MapConfiguration(map);
  }

  @Test
  public void testTimeslotLength ()
  {
    Configurator uut = new Configurator();
    uut.setConfiguration(config);
    uut.configureSingleton(comp);
    assertEquals("correct timeslot length", 15, comp.getTimeslotLength());
    assertEquals("correct min ts count", 600, comp.getMinimumTimeslotCount());
    Instant inst = new DateTime(2009, 10, 10, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    assertEquals("correct base time", inst, comp.getSimulationBaseTime());
  }

  @Test
  public void testNoInit ()
  {
    Configurator uut = new Configurator();
    //uut.setConfiguration(config);
    uut.configureSingleton(comp);
    assertEquals("correct timeslot length", 60, comp.getTimeslotLength());
    assertEquals("correct min ts count", 480, comp.getMinimumTimeslotCount());    
  }

  @Test
  public void testForeign1 ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("pt.configTestDummy.intProperty", "4");
    map.put("pt.configTestDummy.fixedPerKwh", "4.2");
    Configuration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);

    ConfigTestDummy dummy = new ConfigTestDummy();
    assertEquals("original value", 0, dummy.getIntProperty());
    uut.configureSingleton(dummy);
    assertEquals("new value", 4, dummy.getIntProperty());
    assertEquals("new value", 4.2, dummy.getFixedPerKwh(), 1e-6);
    assertEquals("original string", "dummy", dummy.stringProperty);
  }

  @Test
  public void testForeign2 ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("pt.configTestDummy.intProperty", "-4");
    map.put("pt.configTestDummy.fixedPerKwh", "a6.2"); // bad string
    map.put("pt.configTestDummy.stringProperty", "new string");
    Configuration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);

    ConfigTestDummy dummy = new ConfigTestDummy();
    assertEquals("original value", 0, dummy.getIntProperty());
    assertEquals("original string", "dummy", dummy.stringProperty);
    uut.configureSingleton(dummy);
    assertEquals("new value", -4, dummy.getIntProperty());
    assertEquals("original value", -0.06, dummy.getFixedPerKwh(), 1e-6);
    assertEquals("new string", "new string", dummy.stringProperty);
  }

  @Test
  public void testListConfig ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("pt.configTestDummy.listProperty", "1.0, 2.1, 3.2");
    map.put("pt.configTestDummy.secondList", "0.1, 1.2, 2.3");
    Configuration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);
    ConfigTestDummy dummy = new ConfigTestDummy();
    assertNull("original value", dummy.getListProperty());
    assertNull("2nd original", dummy.getSecondList());
    uut.configureSingleton(dummy);
    assertNotNull("not null", dummy.getListProperty());
    List<String> val = dummy.getListProperty();
    assertEquals("correct length", 3, val.size());
    assertEquals("correct 1st", "1.0", val.get(0));
    assertEquals("correct last", "3.2", val.get(2));
    assertNotNull("2nd not null", dummy.getSecondList());
    val = dummy.getSecondList();
    assertEquals("correct 2nd length", 3, val.size());
    assertEquals("correct 2nd 1st", "0.1", val.get(0));
  }

  @Test
  public void testPublishConfig ()
  {
    final TreeMap<String,Object> map = new TreeMap<String, Object>();
    ConfigurationRecorder cr =
        new ConfigurationRecorder () {
          @Override
          public void recordItem (String key, Object value) {
            map.put(key, value);
          }
    };
    ConfigTestDummy dummy = new ConfigTestDummy();
    Configurator uut = new Configurator();
    uut.gatherPublishedConfiguration(dummy, cr);
    assertEquals("two entries", 2, map.size());
    assertEquals("correct String", "dummy",
                 (String)map.get("pt.configTestDummy.stringProperty"));
    assertEquals("correct double", -0.06,
                 (Double)map.get("pt.configTestDummy.fixedPerKwh"), 1e-6);
  }

  @Test
  public void testBootstrapState()
  {
    final TreeMap<String,Object> map = new TreeMap<String, Object>();
    ConfigurationRecorder cr =
        new ConfigurationRecorder () {
          @Override
          public void recordItem (String key, Object value) {
            map.put(key, value);
          }
    };
    ConfigTestDummy dummy = new ConfigTestDummy();
    Configurator uut = new Configurator();
    uut.gatherBootstrapState(dummy, cr);
    assertEquals("two entries", 2, map.size());
    assertEquals("correct int", 0,
                 ((Integer)map.get("pt.configTestDummy.intProperty")).intValue());
    assertEquals("correct double", -0.06,
                 (Double)map.get("pt.configTestDummy.fixedPerKwh"), 1e-6);
  }

  @Test
  public void testConfigInstance ()
  {
    TreeMap<String,String> map = new TreeMap<String, String>();
    map.put("common.config.configInstance.instances", "x1, x2");
    map.put("common.config.configInstance.x1.simpleProp", "42");
    map.put("common.config.configInstance.x1.sequence", "1");
    map.put("common.config.configInstance.x2.simpleProp", "32");
    map.put("common.config.configInstance.x2.sequence", "2");
    Configuration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);
    Collection<?> result = uut.configureInstances(ConfigInstance.class);
    assertEquals("two instances", 2, result.size());
  }

  @Test
  public void testBootstrapInstance ()
  {
    final TreeMap<String,Object> map = new TreeMap<String, Object>();
    ConfigurationRecorder cr =
        new ConfigurationRecorder () {
          @Override
          public void recordItem (String key, Object value) {
            map.put(key, value);
          }
    };
    ConfigInstance ci1 = new ConfigInstance("a1");
    ci1.sequence = 3;
    ci1.simpleProp = 21;
    ci1.stateProp = -3;
    ConfigInstance ci2 = new ConfigInstance("b1");
    ci2.sequence = 4;
    ci2.simpleProp = 31;
    ci2.stateProp = -13;
    List<ConfigInstance> instances = Arrays.asList(ci1, ci2);
    Configurator uut = new Configurator();
    uut.gatherBootstrapState(instances, cr);
    assertEquals("four entries", 4, map.size());
    assertEquals("a1.stateProp", -3,
                 map.get("common.config.configInstance.a1.stateProp"));
  }

}
