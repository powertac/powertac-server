package org.powertac.common;

import static org.junit.Assert.*;

import java.io.File;
import java.io.StringWriter;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/test/resources/test-config.xml"})
@DirtiesContext
public class OrderTests
{
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;

  private Broker broker;
  private Timeslot timeslot;
  private Instant now;
  
  @BeforeClass
  public static void setUpLog () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/log.config");
  }

  @AfterClass
  public static void saveLogs () throws Exception
  {
    File state = new File("log/test.state");
    state.renameTo(new File("log/OrderTests.state"));
    File trace = new File("log/test.trace");
    trace.renameTo(new File("log/OrderTests.trace"));
  }
  
  @Before
  public void setUp () throws Exception
  {
    timeslotRepo.recycle();
    brokerRepo.recycle();
    Competition.newInstance("market order test");
    broker = new Broker("Sam");
    brokerRepo.add(broker);
    now = new DateTime(2011, 10, 10, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeslot = timeslotRepo.makeTimeslot(now);
  }

  @Test
  public void testOrder ()
  {
    Order mo = new Order(broker, timeslot, 0.5, -12.0);
    assertNotNull("created something", mo);
    assertEquals("correct broker", broker, mo.getBroker());
    assertEquals("correct timeslot", timeslot, mo.getTimeslot());
    assertEquals("correct quantity", 0.5, mo.getMWh(), 1e-6);
    assertEquals("correct price", -12.0, mo.getLimitPrice(), 1e-6);
  }

  @Test
  public void testOrderNull ()
  {
    Order mo = new Order(broker, timeslot, 0.5, null);
    assertNotNull("created something", mo);
    assertEquals("correct broker", broker, mo.getBroker());
    assertEquals("correct timeslot", timeslot, mo.getTimeslot());
    assertEquals("correct quantity", 0.5, mo.getMWh(), 1e-6);
    assertNull("null price", mo.getLimitPrice());
  }

  @Test
  public void xmlSerializationTest ()
  {
    Order mo1 = new Order(broker, timeslot, 0.5, -12.0);
    XStream xstream = new XStream();
    xstream.processAnnotations(Order.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mo1));
    //System.out.println(serialized.toString());
    
    Order xmo1 = (Order)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xmo1);
    assertEquals("correct broker", broker, xmo1.getBroker());
    assertEquals("correct timeslot", timeslot, xmo1.getTimeslot());
    assertEquals("correct quantity", 0.5, xmo1.getMWh(), 1e-6);
    assertEquals("correct price", -12.0, xmo1.getLimitPrice(), 1e-6);
  }

  @Test
  public void xmlSerializationTestNull ()
  {
    Order mo1 = new Order(broker, timeslot, 0.5, null);
    XStream xstream = new XStream();
    xstream.processAnnotations(Order.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mo1));
    //System.out.println(serialized.toString());
    
    Order xmo1 = (Order)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xmo1);
    assertEquals("correct broker", broker, xmo1.getBroker());
    assertEquals("correct timeslot", timeslot, xmo1.getTimeslot());
    assertEquals("correct quantity", 0.5, xmo1.getMWh(), 1e-6);
    assertNull("null price", xmo1.getLimitPrice());
  }
}
