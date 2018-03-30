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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
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
  MapConfiguration config;
  
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
    MapConfiguration conf = new MapConfiguration(map);
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
    MapConfiguration conf = new MapConfiguration(map);
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
    MapConfiguration conf = new MapConfiguration(map);
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
  public void testConfigInstanceList ()
  {
    TreeMap<String,String> map = new TreeMap<String, String>();
    map.put("common.config.configInstance.instances", "x1, x2");
    map.put("common.config.configInstance.x1.simpleProp", "42");
    map.put("common.config.configInstance.x1.sequence", "1");
    map.put("common.config.configInstance.x1.coefficients", "0.2, 4.2");
    map.put("common.config.configInstance.x2.simpleProp", "32");
    map.put("common.config.configInstance.x2.sequence", "2");
    map.put("common.config.configInstance.x2.coefficients", "4.2, 3.2");
    MapConfiguration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);
    Collection<?> result = uut.configureInstances(ConfigInstance.class);
    assertEquals("two instances", 2, result.size());
    Object[] instances = result.toArray();
    ConfigInstance x1, x2;
    if (((ConfigInstance)instances[0]).getName().equals("x1")) {
      x1 = (ConfigInstance)instances[0];
      x2 = (ConfigInstance)instances[1];
    }
    else {
      x1 = (ConfigInstance)instances[1];
      x2 = (ConfigInstance)instances[0];
    }
    assertEquals("name x1", "x1", x1.getName());
    assertEquals("name x2", "x2", x2.getName());
    assertEquals("simpleProp", 42, x1.simpleProp);
    assertEquals("sequence", 2, x2.sequence);
    assertEquals("2 coefficients", 2, x1.coefficients.size());
    assertEquals("1st coefficient", "4.2", x2.coefficients.get(0));
    assertEquals("2nd coefficient", "4.2", x1.coefficients.get(1));
  }

  @Test
  public void testConfigInstanceNull ()
  {
    TreeMap<String,String> map = new TreeMap<String, String>();
    MapConfiguration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);
    Collection<?> result = uut.configureInstances(ConfigInstance.class);
    assertNotNull("non-null result", result);
    assertEquals("zero instances", 0, result.size());
  }

  @Test
  public void testConfigNamedInstance ()
  {
    TreeMap<String,String> map = new TreeMap<String, String>();
    map.put("common.config.configInstance.x1.simpleProp", "42");
    map.put("common.config.configInstance.x1.sequence", "1");
    map.put("common.config.configInstance.x1.coefficients", "0.2, 4.2");
    map.put("common.config.configInstance.x2.simpleProp", "32");
    map.put("common.config.configInstance.x2.sequence", "2");
    map.put("common.config.configInstance.x2.coefficients", "4.2, 3.2");
    ArrayList<ConfigInstance> instanceList = new ArrayList<ConfigInstance>();
    ConfigInstance ci1 = new ConfigInstance("x1");
    instanceList.add(ci1);
    ConfigInstance ci2 = new ConfigInstance("x2");
    instanceList.add(ci2);
    MapConfiguration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);
    Collection<?> result = uut.configureNamedInstances(instanceList);
    assertEquals("two instances", 2, result.size());
    assertEquals("name x1", "x1", ci1.getName());
    assertEquals("name x2", "x2", ci2.getName());
    assertEquals("simpleProp", 42, ci1.simpleProp);
    assertEquals("sequence", 2, ci2.sequence);
    assertEquals("2 coefficients", 2, ci1.coefficients.size());
    assertEquals("1st coefficient", "4.2", ci2.coefficients.get(0));
    assertEquals("2nd coefficient", "4.2", ci1.coefficients.get(1));
  }

  @Test
  public void testBootstrapInstanceOrig ()
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
    assertEquals("six entries", 6, map.size());
    assertEquals("a1.stateProp", -3,
                 map.get("common.config.configInstance.a1.stateProp"));
  }

  @Test
  public void testBootstrapList ()
  {
    Recorder cr = new Recorder();
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
    assertEquals("six entries", 6, cr.items.size());
    assertEquals("a1.stateProp", -3,
                 cr.items.get("common.config.configInstance.a1.stateProp"));
    assertEquals("b1.sequence", 4,
                 cr.items.get("common.config.configInstance.b1.sequence"));
    // simpleProp is not a bootstrap item
    assertNull("b1.simpleProp",
               cr.items.get("common.config.configInstance.b1.simpleProp"));
  }

  @Test
  public void testConfigDump ()
  {
    Recorder cr = new Recorder();
    Configurator uut = new Configurator();
    uut.setConfigOutput(cr);

    TreeMap<String,String> map = new TreeMap<String, String>();
    map.put("common.config.configInstance.instances", "x1, x2");
    map.put("common.config.configInstance.x1.simpleProp", "42");
    map.put("common.config.configInstance.x1.sequence", "1");
    map.put("common.config.configInstance.x1.coefficients", "0.2, 4.2");
    map.put("common.config.configInstance.x2.booleanProperty", "true");
    map.put("common.config.configInstance.x2.simpleProp", "32");
    map.put("common.config.configInstance.x2.sequence", "2");
    map.put("common.config.configInstance.x2.coefficients", "4.2, 3.2");
    map.put("common.config.configInstance.x2.booleanProperty", "false");
    MapConfiguration conf = new MapConfiguration(map);

    uut.setConfiguration(conf);
    Collection<?> result = uut.configureInstances(ConfigInstance.class);
    assertEquals("two instances", 2, result.size());
    assertEquals("10 in items", 10, cr.items.size());
    assertEquals("x1.simpleProp", "42",
                 cr.items.get("common.config.configInstance.x1.simpleProp").toString());
    assertEquals("x2.coefficients", "[4.2, 3.2]",
                 cr.items.get("common.config.configInstance.x2.coefficients").toString());
    assertEquals("5 in metadata", 5, cr.metadata.size());
    assertNull("no instance metadata",
               cr.metadata.get("common.config.configInstance.x1.stateProp"));
    assertEquals("simpleProp description", "sample state",
                 cr.metadata.get("common.config.configInstance.stateProp").description);
    assertEquals("x2.coefficients valueType", "List",
                 cr.metadata.get("common.config.configInstance.coefficients").valueType);
    assertTrue("x1.factor published",
               cr.metadata.get("common.config.configInstance.factor").publish);
    assertFalse("x2.sequence not published",
                cr.metadata.get("common.config.configInstance.sequence").publish);
    //assertEquals("one instance list", 1,
    //             cr.instanceLists.size());
    //assertEquals("correct list", "[x1, x2]",
    //             cr.instanceLists.get("common.config.configInstance.instances").toString());
  }

  @Test
  public void testComposeKey ()
  {
    Configurator uut = new Configurator();
    ConfigInstance ci1 = new ConfigInstance("a1");
    String ck1 = uut.composeKey(ci1.getClass().getName(), "prop", "name");
    assertEquals("common.config.configInstance.name.prop", ck1);
  }

  //Test version of ConfigurationRecorder
  class Recorder implements ConfigurationRecorder
  {
    Map<String, Object> items;
    Map<String, RecordedMetadata> metadata;
    Map<String, List<String>> instanceLists;

    Recorder ()
    {
      super();
      items = new TreeMap<>();
      metadata = new TreeMap<>();
      instanceLists = new TreeMap<>();
    }

    @Override
    public void recordItem (String key, Object value)
    {
      items.put(key, value);
    }

    @Override
    public void recordMetadata(String key, String description,
                               String valueType,
                               boolean publish, boolean bootstrapState)
    {
      metadata.put(key, (new RecordedMetadata(description, valueType,
                                              publish, bootstrapState)));
    }

    @Override
    public void recordInstanceList(String key, List<String> names)
    {
      instanceLists.put(key, names);
    }
  }

  class RecordedMetadata
  {
    String description;
    String valueType;
    boolean publish;
    boolean bootstrapState;

    RecordedMetadata (String description, String valueType,
                      boolean publish, boolean bootstrapState)
    {
      super();
      this.description = description;
      this.valueType = valueType;
      this.publish = publish;
      this.bootstrapState = bootstrapState;
    }
  }
}
