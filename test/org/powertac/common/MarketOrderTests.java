package org.powertac.common;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:test/test-config.xml"})
public class MarketOrderTests
{
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;

  private Competition competition;
  private Broker broker;
  private Timeslot timeslot;
  private Instant now;
  
  @BeforeClass
  public static void setUpLog () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }
  
  @Before
  public void setUp () throws Exception
  {
    competition = Competition.newInstance("market order test");
    broker = new Broker("Sam");
    brokerRepo.add(broker);
    now = new DateTime(2011, 10, 10, 12, 0, 0, 0, DateTimeZone.UTC).toInstant();
    timeslot = timeslotRepo.makeTimeslot(now);
  }

  @Test
  public void testMarketOrder ()
  {
    MarketOrder mo = new MarketOrder(broker, timeslot, 0.5, -12.0);
    assertNotNull("created something", mo);
    assertEquals("correct broker", broker, mo.getBroker());
    assertEquals("correct timeslot", timeslot, mo.getTimeslot());
    assertEquals("correct quantity", 0.5, mo.getMWh(), 1e-6);
    assertEquals("correct price", -12.0, mo.getLimitPrice(), 1e-6);
  }


  @Test
  public void xmlSerializationTest ()
  {
    MarketOrder mo1 = new MarketOrder(broker, timeslot, 0.5, -12.0);
    XStream xstream = new XStream();
    xstream.processAnnotations(MarketOrder.class);
    xstream.processAnnotations(Broker.class);
    xstream.processAnnotations(Timeslot.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mo1));
    System.out.println(serialized.toString());
    
    MarketOrder xmo1 = (MarketOrder)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xmo1);
    assertEquals("correct broker", broker, xmo1.getBroker());
    assertEquals("correct timeslot", timeslot, xmo1.getTimeslot());
    assertEquals("correct quantity", 0.5, xmo1.getMWh(), 1e-6);
    assertEquals("correct price", -12.0, xmo1.getLimitPrice(), 1e-6);
  }
}
