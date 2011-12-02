package org.powertac.common;

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.Map;

import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class PluginConfigTests
{

  @Test
  public void testGetRoleName ()
  {
    PluginConfig uut = new PluginConfig("test-role", "test");
    assertNotNull("not null", uut);
    assertEquals("correct role", "test-role", uut.getRoleName());
    assertEquals("correct name", "test", uut.getName());
  }

  @Test
  public void testGetConfiguration ()
  {
    PluginConfig uut = new PluginConfig("test-role", "test");
    uut.addConfiguration("attribute1", "42");
    uut.addConfiguration("attribute2", "some value");
    Map<String, String> config = uut.getConfiguration();
    assertNotNull("config not null", config);
    assertEquals("correct count", 2, config.size());
    assertEquals("correct attribute1", "42", config.get("attribute1"));
    assertEquals("correct attribute2", "some value", config.get("attribute2"));
  }

  @Test
  public void testToString ()
  {
    PluginConfig uut = new PluginConfig("test-role", "test");
    assertEquals("correct string", "PluginConfig:test-role:test[]", uut.toString());
  }

  @Test
  public void xmlSerializationTest ()
  {
    PluginConfig uut = new PluginConfig("test-role", "test");
    uut.addConfiguration("attribute1", "42");
    uut.addConfiguration("attribute2", "some value");

    XStream xstream = new XStream();
    xstream.processAnnotations(PluginConfig.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(uut));
    //System.out.println(serialized.toString());
    PluginConfig xuut = (PluginConfig)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xuut);
    assertEquals("correct role name", "test-role", xuut.getRoleName());
    assertEquals("correct name", "test", xuut.getName());
    Map<String, String> config = xuut.getConfiguration();
    assertNotNull("config not null", config);
    assertEquals("correct count", 2, config.size());
    assertEquals("correct attribute1", "42", config.get("attribute1"));
    assertEquals("correct attribute2", "some value", config.get("attribute2"));
  }
}
