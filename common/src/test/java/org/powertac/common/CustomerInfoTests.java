package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.enumerations.PowerType;

import com.thoughtworks.xstream.XStream;

public class CustomerInfoTests
{

  @BeforeEach
  public void setUp () throws Exception
  {
  }

  @Test
  public void testCustomerInfo ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    assertNotNull(info, "not null");
    assertEquals("t1", info.getName(), "name");
    assertEquals(33, info.getPopulation(), "population");
    assertEquals(PowerType.CONSUMPTION, info.getPowerType(), "correct power type");
    assertFalse(info.isMultiContracting(), "no multicontracting");
    assertFalse(info.isCanNegotiate(), "can't negotiate");
    assertEquals(CustomerClass.SMALL, info.getCustomerClass(), "small customer");
  }

  @Test
  public void testSetPopulation ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    info.setPopulation(42);
    assertEquals(42, info.getPopulation(), "correct population");
  }

  @Test
  public void testWithPowerType ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.withPowerType(PowerType.PRODUCTION);
    assertEquals(info, result, "correct return");
    assertEquals(PowerType.PRODUCTION, info.getPowerType(), "correct type");
  }

  @Test
  public void testSetMultiContracting ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.withMultiContracting(true);
    assertEquals(info, result, "correct return");
    assertTrue(info.isMultiContracting(), "multi-contracting");
  }

  @Test
  public void testSetCanNegotiate ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.withCanNegotiate(true);
    assertEquals(info, result, "correct return");
    assertTrue(info.isCanNegotiate(), "can negotiate");
  }

  @Test
  public void testToString ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    assertEquals("CustomerInfo(t1)", info.toString(), "correct string");
  }

  @Test
  public void xmlSerializationTest ()
  {
    CustomerInfo ci = new CustomerInfo("Sam", 44);
    ci.withPowerType(PowerType.ELECTRIC_VEHICLE);
    ci.withCustomerClass(CustomerClass.LARGE);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(CustomerInfo.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ci));
    //System.out.println(serialized.toString());
    CustomerInfo xci = (CustomerInfo)xstream.fromXML(serialized.toString());
    assertNotNull(xci, "deserialized something");
    assertEquals(ci.getId(), xci.getId(), "correct id");
    assertEquals("Sam", xci.getName(), "correct name");
    assertEquals(44, xci.getPopulation(), "correct population");
    //assertEquals(2, xci.getPowerTypes().size(), "correct number of PowerTypes");
    assertEquals(PowerType.ELECTRIC_VEHICLE, xci.getPowerType(), "electric vehicle");
    assertEquals(CustomerClass.LARGE, xci.getCustomerClass(), "correct class");
  }
}
