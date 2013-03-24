package org.powertac.common;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.AfterClass;
import org.junit.Before;
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
@ContextConfiguration(locations = {"classpath:test-config.xml"})
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
    Competition.setCurrent(Competition.newInstance("market order test"));
    broker = new Broker("Sam");
    brokerRepo.add(broker);
    now = Competition.currentCompetition().getSimulationBaseTime().plus(TimeService.DAY);
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
  
  @Test
  public void xmlSerializationLocalBroker ()
  {
    DummyBroker db = new DummyBroker("Dummy", true, false);
    brokerRepo.add(db);
    Order mo1 = new Order(db, timeslot, 0.5, -12.0);
    XStream xstream = new XStream();
    xstream.processAnnotations(Order.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    xstream.aliasSystemAttribute(null, "class");
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mo1));
    //System.out.println(serialized.toString());
    
    Order xmo1 = (Order)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xmo1);
    assertEquals("correct broker", db, xmo1.getBroker());
    assertEquals("correct timeslot", timeslot, xmo1.getTimeslot());
    assertEquals("correct quantity", 0.5, xmo1.getMWh(), 1e-6);
    assertEquals("correct price", -12.0, xmo1.getLimitPrice(), 1e-6);
  }
  
  @Test
  public void stateLogTest ()
  {
    DummyBroker db = new DummyBroker("Dummy", false, false);
    brokerRepo.add(db);
    String xml = "<order id=\"200000394\" timeslot=\""
        + timeslot.getSerialNumber()
        + "\" mWh=\"22.7\" limitPrice=\"-70.0\"> "
        + "<broker>Dummy</broker> </order>";
    //System.out.println(xml);
    XStream xstream = new XStream();
    xstream.processAnnotations(Order.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    xstream.aliasSystemAttribute(null, "class");
    Order xmo1 = (Order)xstream.fromXML(xml);
    assertNotNull("deserialized something", xmo1);
    assertEquals("correct broker", db, xmo1.getBroker());
    assertEquals("correct timeslot", timeslot, xmo1.getTimeslot());
    assertEquals("correct quantity", 22.7, xmo1.getMWh(), 1e-6);
    assertEquals("correct price", -70.0, xmo1.getLimitPrice(), 1e-6);
    
    // Opens the state file and checks the last entry.
    // This works when running the individual test, but not as a suite.
    // Also, it might not work unless there's a way to flush the
    // state log.
//    String item = null;
//    try {
//      BufferedReader input =
//          new BufferedReader(new FileReader("log/OrderTests.state"));
//      String test = input.readLine();
//      while (test != null) {
//        // looking for the last line
//        item = test;
//        test = input.readLine();
//      }
//    }
//    catch (FileNotFoundException e) {
//      fail("cannot find state file: " + e.toString());
//    }
//    catch (IOException e) {
//      fail(e.toString());
//    }
//    if (item != null) {
//      System.out.println("item=" + item);
//      // should not get here if item == null
//      String[] items = item.split("::");
//      assertEquals("class", "org.powertac.common.Order", items[1]);
//      assertEquals("id", "200000394", items[2]);
//      assertEquals("broker id", db.getId(), Long.parseLong(items[4]));
//      assertEquals("timeslot id", timeslot.getId(), Long.parseLong(items[5]));
//      assertEquals("mwh", 22.7, Double.parseDouble(items[6]), 1e-6);
//      assertEquals("price", -70.0, Double.parseDouble(items[7]), 1e-6);
//    }
  }
  
  class DummyBroker extends Broker
  {

    public DummyBroker (String username, boolean local, boolean wholesale)
    {
      super(username, local, wholesale);
    }
    
  }
}
