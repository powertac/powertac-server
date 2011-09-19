package org.powertac.common;

import static org.junit.Assert.*;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompetitionTests
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
  public void testNewInstance ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertNotNull("c1 created", c1);
    assertEquals("c1 retreival", c1, Competition.currentCompetition());
    assertEquals("name property", "c1", c1.getName());
    Competition c2 = Competition.newInstance("c2");
    assertEquals("c2 retreival", c2, Competition.currentCompetition());
    assertEquals("name property", "c2", c2.getName());
  }

  @Test
  public void testSetDescription ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("empty description", "", c1.getDescription());
    Competition cx = c1.setDescription("test version");
    assertEquals("correct return", c1, cx);
    assertEquals("description", "test version", Competition.currentCompetition().getDescription());
  }

  @Test
  public void testSetTimeslotLength ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("default length", 60, c1.getTimeslotLength());
    Competition cx = c1.setTimeslotLength(30);
    assertEquals("correct return", c1, cx);
    assertEquals("new length", 30, c1.getTimeslotLength());
  }

  @Test
  public void testSetMinimumTimeslotCount ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("default count", 240, c1.getMinimumTimeslotCount());
    Competition cx = c1.setMinimumTimeslotCount(300);
    assertEquals("correct return", c1, cx);
    assertEquals("new count", 300, c1.getMinimumTimeslotCount());
  }

  @Test
  public void testSetExpectedTimeslotCount ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("default count", 288, c1.getExpectedTimeslotCount());
    Competition cx = c1.setExpectedTimeslotCount(360);
    assertEquals("correct return", c1, cx);
    assertEquals("new count", 360, c1.getExpectedTimeslotCount());
  }

  @Test
  public void testSetTimeslotsOpen ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("default count", 23, c1.getTimeslotsOpen());
    Competition cx = c1.setTimeslotsOpen(13);
    assertEquals("correct return", c1, cx);
    assertEquals("new count", 13, c1.getTimeslotsOpen());
  }

  @Test
  public void testSetDeactivateTimeslotsAhead ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("default count", 1, c1.getDeactivateTimeslotsAhead());
    Competition cx = c1.setDeactivateTimeslotsAhead(3);
    assertEquals("correct return", c1, cx);
    assertEquals("new count", 3, c1.getDeactivateTimeslotsAhead());
  }

  @Test
  public void testSetSimulationBaseTime ()
  {
    Competition c1 = Competition.newInstance("c1");
    Instant base = new DateTime(2010, 6, 21, 0, 0, 0, 0, DateTimeZone.UTC).toInstant();
    assertEquals("default base", base, c1.getSimulationBaseTime());
    Instant newBase = base.plus(TimeService.DAY);
    Competition cx = c1.setSimulationBaseTime(newBase);
    assertEquals("correct return", c1, cx);
    assertEquals("new base", newBase, c1.getSimulationBaseTime());
  }

  @Test
  public void testSetSimulationRate ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("default rate", 720l, c1.getSimulationRate());
    Competition cx = c1.setSimulationRate(300l);
    assertEquals("correct return", c1, cx);
    assertEquals("new rate", 300l, c1.getSimulationRate());
  }

  @Test
  public void testSetSimulationModulo ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("default mod", 60*60*1000, c1.getSimulationModulo());
    Competition cx = c1.setSimulationModulo(30*60000);
    assertEquals("correct return", c1, cx);
    assertEquals("new mod", 30*60000, c1.getSimulationModulo());
  }

  @Test
  public void testAddBroker ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("no brokers", 0, c1.getBrokers().size());
    Competition cx = c1.addBroker("Jill");
    assertEquals("correct return", c1, cx);
    assertEquals("one broker", 1, c1.getBrokers().size());
    assertEquals("correct broker", "Jill", c1.getBrokers().get(0));
  }

  @Test
  public void testAddPlugin ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("no configs", 0, c1.getPluginConfigs().size());
    PluginConfig pc1 = new PluginConfig("A", "B");
    Competition cx = c1.addPluginConfig(pc1);
    assertEquals("correct return", c1, cx);
    assertEquals("one config", 1, c1.getPluginConfigs().size());
    assertEquals("correct config", pc1, c1.getPluginConfigs().get(0));
  }

  @Test
  public void testAddCustomer ()
  {
    Competition c1 = Competition.newInstance("c1");
    assertEquals("no customers", 0, c1.getCustomers().size());
    CustomerInfo info = new CustomerInfo("Podunk", 42);
    Competition cx = c1.addCustomer(info);
    assertEquals("correct return", c1, cx);
    assertEquals("one customer", 1, c1.getCustomers().size());
    assertEquals("correct customer", info, c1.getCustomers().get(0));
  }

}
