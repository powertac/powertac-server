package org.powertac.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.enumerations.PowerType;

import com.thoughtworks.xstream.XStream;

public class CustomerInfoTests
{

  @Before
  public void setUp () throws Exception
  {
  }

  @Test
  public void testCustomerInfo ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    assertNotNull("not null", info);
    assertEquals("name", "t1", info.getName());
    assertEquals("population", 33, info.getPopulation());
    assertEquals("correct power type", PowerType.CONSUMPTION, info.getPowerType());
    assertFalse("no multicontracting", info.isMultiContracting());
    assertFalse("can't negotiate", info.isCanNegotiate());
  }

  @Test
  public void testSetPopulation ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    info.setPopulation(42);
    assertEquals("correct population", 42, info.getPopulation());
  }

  @Test
  public void testWithPowerType ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.withPowerType(PowerType.PRODUCTION);
    assertEquals("correct return", info, result);
    assertEquals("correct type", PowerType.PRODUCTION, info.getPowerType());
  }

  @Test
  public void testSetMultiContracting ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.withMultiContracting(true);
    assertEquals("correct return", info, result);
    assertTrue("multi-contracting", info.isMultiContracting());
  }

  @Test
  public void testSetCanNegotiate ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.withCanNegotiate(true);
    assertEquals("correct return", info, result);
    assertTrue("can negotiate", info.isCanNegotiate());
  }

  @Test
  public void testToString ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    assertEquals("correct string", "CustomerInfo(t1)", info.toString());
  }

  @Test
  public void xmlSerializationTest ()
  {
    CustomerInfo ci = new CustomerInfo("Sam", 44);
    ci.withPowerType(PowerType.ELECTRIC_VEHICLE);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(CustomerInfo.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(ci));
    //System.out.println(serialized.toString());
    CustomerInfo xci = (CustomerInfo)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xci);
    assertEquals("correct id", ci.getId(), xci.getId());
    assertEquals("correct name", "Sam", xci.getName());
    assertEquals("correct population", 44, xci.getPopulation());
    //assertEquals("correct number of PowerTypes", 2, xci.getPowerTypes().size());
    assertEquals("electric vehicle", PowerType.ELECTRIC_VEHICLE,
                 xci.getPowerType());
  }

}
