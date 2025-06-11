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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.configuration2.MapConfiguration;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  @BeforeEach
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
    assertEquals(15, comp.getTimeslotLength(), "correct timeslot length");
    assertEquals(600, comp.getMinimumTimeslotCount(), "correct min ts count");
    Instant inst = ZonedDateTime.of(2009, 10, 10, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
    assertEquals(inst, comp.getSimulationBaseTime(), "correct base time");
  }

  @Test
  public void testNoInit ()
  {
    Configurator uut = new Configurator();
    //uut.setConfiguration(config);
    uut.configureSingleton(comp);
    assertEquals(60, comp.getTimeslotLength(), "correct timeslot length");
    assertEquals(480, comp.getMinimumTimeslotCount(), "correct min ts count");
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
    assertEquals(0, dummy.getIntProperty(), "original value");
    uut.configureSingleton(dummy);
    assertEquals(4, dummy.getIntProperty(), "new value");
    assertEquals(4.2, dummy.getFixedPerKwh(), 1e-6, "new value");
    assertEquals("dummy", dummy.stringProperty, "original string");
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
    assertEquals(0, dummy.getIntProperty(), "original value");
    assertEquals("dummy", dummy.stringProperty, "original string");
    uut.configureSingleton(dummy);
    assertEquals(-4, dummy.getIntProperty(), "new value");
    assertEquals(-0.06, dummy.getFixedPerKwh(), 1e-6, "original value");
    assertEquals("new string", dummy.stringProperty, "new string");
  }

  @Test
  public void testForeignXml ()
  {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("pt.configTestDummy.intProperty", "4");
    map.put("pt.configTestDummy.fixedPerKwh", "4.2");
    map.put("pt.configTestDummy.xmlProperty", "<list><int>42</int><double>42.42</double></list>");
    MapConfiguration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);

    ConfigTestDummy dummy = new ConfigTestDummy();
    assertEquals(0, dummy.getIntProperty(), "original value");
    uut.configureSingleton(dummy);
    assertEquals(4, dummy.getIntProperty(), "new value");
    assertEquals(4.2, dummy.getFixedPerKwh(), 1e-6, "new value");
    assertEquals("dummy", dummy.stringProperty, "original string");
    List<Object> xmlProp = dummy.getXmlProperty();
    assertEquals(2, xmlProp.size());
    assertEquals(42, (int)xmlProp.get(0));
    assertEquals(42.42, (double)xmlProp.get(1));
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
    assertNull(dummy.getListProperty(), "original value");
    assertNull(dummy.getSecondList(), "2nd original");
    uut.configureSingleton(dummy);
    assertNotNull(dummy.getListProperty(), "not null");
    List<String> val = dummy.getListProperty();
    assertEquals(3, val.size(), "correct length");
    assertEquals("1.0", val.get(0), "correct 1st");
    assertEquals("3.2", val.get(2), "correct last");
    assertNotNull(dummy.getSecondList(), "2nd not null");
    val = dummy.getSecondList();
    assertEquals(3, val.size(), "correct 2nd length");
    assertEquals("0.1", val.get(0), "correct 2nd 1st");
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
    assertEquals(2, map.size(), "two entries");
    assertEquals("dummy", (String)map.get("pt.configTestDummy.stringProperty"), "correct String");
    assertEquals(-0.06, (Double)map.get("pt.configTestDummy.fixedPerKwh"), 1e-6, "correct double");
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
    assertEquals(2, map.size(), "two entries");
    assertEquals(0, ((Integer)map.get("pt.configTestDummy.intProperty")).intValue(), "correct int");
    assertEquals(-0.06, (Double)map.get("pt.configTestDummy.fixedPerKwh"), 1e-6, "correct double");
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
    assertEquals(2, result.size(), "two instances");
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
    assertEquals("x1", x1.getName(), "name x1");
    assertEquals("x2", x2.getName(), "name x2");
    assertEquals(42, x1.simpleProp, "simpleProp");
    assertEquals(2, x2.sequence, "sequence");
    assertEquals(2, x1.coefficients.size(), "2 coefficients");
    assertEquals("4.2", x2.coefficients.get(0), "1st coefficient");
    assertEquals("4.2", x1.coefficients.get(1), "2nd coefficient");
  }

  @Test
  public void testConfigInstanceNull ()
  {
    TreeMap<String,String> map = new TreeMap<String, String>();
    MapConfiguration conf = new MapConfiguration(map);
    Configurator uut = new Configurator();
    uut.setConfiguration(conf);
    Collection<?> result = uut.configureInstances(ConfigInstance.class);
    assertNotNull(result, "non-null result");
    assertEquals(0, result.size(), "zero instances");
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
    assertEquals(2, result.size(), "two instances");
    assertEquals("x1", ci1.getName(), "name x1");
    assertEquals("x2", ci2.getName(), "name x2");
    assertEquals(42, ci1.simpleProp, "simpleProp");
    assertEquals(2, ci2.sequence, "sequence");
    assertEquals(2, ci1.coefficients.size(), "2 coefficients");
    assertEquals("4.2", ci2.coefficients.get(0), "1st coefficient");
    assertEquals("4.2", ci1.coefficients.get(1), "2nd coefficient");
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
    assertEquals(6, map.size(), "six entries");
    assertEquals(-3, map.get("common.config.configInstance.a1.stateProp"), "a1.stateProp");
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
    assertEquals(6, cr.items.size(), "six entries");
    assertEquals(-3, cr.items.get("common.config.configInstance.a1.stateProp"), "a1.stateProp");
    assertEquals(4, cr.items.get("common.config.configInstance.b1.sequence"), "b1.sequence");
    // simpleProp is not a bootstrap item
    assertNull(cr.items.get("common.config.configInstance.b1.simpleProp"), "b1.simpleProp");
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
    assertEquals(2, result.size(), "two instances");
    assertEquals(10, cr.items.size(), "10 in items");
    assertEquals("42", cr.items.get("common.config.configInstance.x1.simpleProp").toString(), "x1.simpleProp");
    assertEquals("[4.2, 3.2]", cr.items.get("common.config.configInstance.x2.coefficients").toString(), "x2.coefficients");
    assertEquals(5, cr.metadata.size(), "5 in metadata");
    assertNull(cr.metadata.get("common.config.configInstance.x1.stateProp"), "no instance metadata");
    assertEquals("sample state", cr.metadata.get("common.config.configInstance.stateProp").description, "simpleProp description");
    assertEquals("List", cr.metadata.get("common.config.configInstance.coefficients").valueType, "x2.coefficients valueType");
    assertTrue(cr.metadata.get("common.config.configInstance.factor").publish, "x1.factor published");
    assertFalse(cr.metadata.get("common.config.configInstance.sequence").publish, "x2.sequence not published");
    //assertEquals(1, cr.instanceLists.size(), "one instance list");
    //assertEquals("[x1, x2]", cr.instanceLists.get("common.config.configInstance.instances").toString(), "correct list");
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
