package org.powertac.common;

import static org.junit.Assert.*;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.enumerations.PowerType;

public class CustomerInfoTests
{

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

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
    assertEquals("customerType", CustomerType.CustomerHousehold, info.getCustomerType());
    assertEquals("no power types", 0, info.getPowerTypes().size());
    assertFalse("no multicontracting", info.isMultiContracting());
    assertFalse("can't negotiate", info.isCanNegotiate());
  }

  @Test
  public void testSetPopulation ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.setPopulation(42);
    assertEquals("correct return", info, result);
    assertEquals("correct population", 42, info.getPopulation());
  }

  @Test
  public void testSetCustomerType ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.setCustomerType(CustomerType.CustomerFactory);
    assertEquals("correct return", info, result);
    assertEquals("customerType", CustomerType.CustomerFactory, info.getCustomerType());
  }

  @Test
  public void testAddPowerType ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.addPowerType(PowerType.CONSUMPTION);
    assertEquals("correct return", info, result);
    assertEquals("one type", 1, info.getPowerTypes().size());
    assertEquals("correct type", PowerType.CONSUMPTION, info.getPowerTypes().get(0));
  }

  @Test
  public void testSetMultiContracting ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.setMultiContracting(true);
    assertEquals("correct return", info, result);
    assertTrue("multi-contracting", info.isMultiContracting());
  }

  @Test
  public void testSetCanNegotiate ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    CustomerInfo result = info.setCanNegotiate(true);
    assertEquals("correct return", info, result);
    assertTrue("can negotiate", info.isCanNegotiate());
  }

  @Test
  public void testToString ()
  {
    CustomerInfo info = new CustomerInfo("t1", 33);
    assertEquals("correct string", "CustomerInfo(t1)", info.toString());
  }

}
