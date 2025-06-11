package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.StringWriter;

import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class OrderTests
{
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;

  private Broker broker;
  private Timeslot timeslot;
  private int timeslotNum;
  private Instant now;

  @AfterAll
  public static void saveLogs () throws Exception
  {
    File state = new File("log/test.state");
    state.renameTo(new File("log/OrderTests.state"));
    File trace = new File("log/test.trace");
    trace.renameTo(new File("log/OrderTests.trace"));
  }
  
  @BeforeEach
  public void setUp () throws Exception
  {
    timeslotRepo.recycle();
    brokerRepo.recycle();
    Competition.setCurrent(Competition.newInstance("market order test"));
    broker = new Broker("Sam");
    brokerRepo.add(broker);
    now = Competition.currentCompetition().getSimulationBaseTime().plusMillis(TimeService.DAY);
    timeslot = timeslotRepo.makeTimeslot(now);
    timeslotNum = timeslot.getSerialNumber();
  }

  @Test
  public void testOrder ()
  {
    Order mo = new Order(broker, timeslotNum, 0.5, -12.0);
    assertNotNull(mo, "created something");
    assertEquals(broker, mo.getBroker(), "correct broker");
    assertEquals(timeslot, mo.getTimeslot(), "correct timeslot");
    assertEquals(0.5, mo.getMWh(), 1e-6, "correct quantity");
    assertEquals(-12.0, mo.getLimitPrice(), 1e-6, "correct price");
  }

  @Test
  public void testOrderNull ()
  {
    Order mo = new Order(broker, timeslotNum, 0.5, null);
    assertNotNull(mo, "created something");
    assertEquals(broker, mo.getBroker(), "correct broker");
    assertEquals(timeslot, mo.getTimeslot(), "correct timeslot");
    assertEquals(0.5, mo.getMWh(), 1e-6, "correct quantity");
    assertNull(mo.getLimitPrice(), "null price");
  }

  @Test
  public void testOrderNaN ()
  {
    Order mo = new Order(broker, timeslotNum, 0.5, Double.NaN);
    assertNotNull(mo, "created something");
    assertEquals(broker, mo.getBroker(), "correct broker");
    assertEquals(timeslot, mo.getTimeslot(), "correct timeslot");
    assertEquals(0.5, mo.getMWh(), 1e-6, "correct quantity");
    assertNull(mo.getLimitPrice(), "null price");
  }

  @Test
  public void testOrderMin ()
  {
    Competition.currentCompetition().withMinimumOrderQuantity(0.01);
    Order mo = new Order(broker, timeslotNum, 0.005, null);
    assertNotNull(mo, "created something");
    assertEquals(broker, mo.getBroker(), "correct broker");
    assertEquals(timeslot, mo.getTimeslot(), "correct timeslot");
    assertEquals(0.005, mo.getMWh(), 1e-6, "correct quantity");
    assertNull(mo.getLimitPrice(), "null price");
  }

  @Test
  public void xmlSerializationTest ()
  {
    Order mo1 = new Order(broker, timeslotNum, 0.5, -12.0);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Order.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mo1));
    //System.out.println(serialized.toString());
    
    Order xmo1 = (Order)xstream.fromXML(serialized.toString());
    assertNotNull(xmo1, "deserialized something");
    assertEquals(broker, xmo1.getBroker(), "correct broker");
    assertEquals(timeslot, xmo1.getTimeslot(), "correct timeslot");
    assertEquals(0.5, xmo1.getMWh(), 1e-6, "correct quantity");
    assertEquals(-12.0, xmo1.getLimitPrice(), 1e-6, "correct price");
  }

  @Test
  public void xmlSerializationTestNull ()
  {
    Order mo1 = new Order(broker, timeslotNum, 0.5, null);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Order.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mo1));
    //System.out.println(serialized.toString());
    
    Order xmo1 = (Order)xstream.fromXML(serialized.toString());
    assertNotNull(xmo1, "deserialized something");
    assertEquals(broker, xmo1.getBroker(), "correct broker");
    assertEquals(timeslot, xmo1.getTimeslot(), "correct timeslot");
    assertEquals(0.5, xmo1.getMWh(), 1e-6, "correct quantity");
    assertNull(xmo1.getLimitPrice(), "null price");
  }
  
  @Test
  public void xmlSerializationLocalBroker ()
  {
    DummyBroker db = new DummyBroker("Dummy", true, false);
    brokerRepo.add(db);
    Order mo1 = new Order(db, timeslotNum, 0.5, -12.0);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Order.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    xstream.aliasSystemAttribute(null, "class");
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mo1));
    //System.out.println(serialized.toString());
    
    Order xmo1 = (Order)xstream.fromXML(serialized.toString());
    assertNotNull(xmo1, "deserialized something");
    assertEquals(db, xmo1.getBroker(), "correct broker");
    assertEquals(timeslot, xmo1.getTimeslot(), "correct timeslot");
    assertEquals(0.5, xmo1.getMWh(), 1e-6, "correct quantity");
    assertEquals(-12.0, xmo1.getLimitPrice(), 1e-6, "correct price");
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
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Order.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    xstream.aliasSystemAttribute(null, "class");
    Order xmo1 = (Order)xstream.fromXML(xml);
    assertNotNull(xmo1, "deserialized something");
    assertEquals(db, xmo1.getBroker(), "correct broker");
    assertEquals(timeslot, xmo1.getTimeslot(), "correct timeslot");
    assertEquals(22.7, xmo1.getMWh(), 1e-6, "correct quantity");
    assertEquals(-70.0, xmo1.getLimitPrice(), 1e-6, "correct price");
    
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
//      assertEquals("org.powertac.common.Order", items[1], "class");
//      assertEquals("200000394", items[2], "id");
//      assertEquals(db.getId(), Long.parseLong(items[4]), "broker id");
//      assertEquals(timeslot.getId(), Long.parseLong(items[5]), "timeslot id");
//      assertEquals(22.7, Double.parseDouble(items[6]), 1e-6, "mwh");
//      assertEquals(-70.0, Double.parseDouble(items[7]), 1e-6, "price");
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
