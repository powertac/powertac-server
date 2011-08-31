package org.powertac.common;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.apache.log4j.PropertyConfigurator;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powertac.common.repo.BrokerRepo;

import com.thoughtworks.xstream.XStream;

public class BalancingTransactionTests
{
  Instant baseTime;
  Broker broker;
  BrokerRepo brokerRepo;

  @BeforeClass
  public static void setUpLog () throws Exception
  {
    PropertyConfigurator.configure("test/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    baseTime = new DateTime().toInstant();
    brokerRepo = new BrokerRepo();
    broker = new Broker("Sally");
    brokerRepo.add(broker);
  }

  @Test
  public void testBalancingTransaction ()
  {
    BalancingTransaction bt = new BalancingTransaction(baseTime, broker, 42.1, 3.22);
    assertNotNull("not null", bt);
    assertEquals("first id", 0, bt.getId());
    assertEquals("correct time", baseTime, bt.getPostedTime());
    assertEquals("correct broker", broker, bt.getBroker());
    assertEquals("correct qty", 42.1, bt.getQuantity(), 1e-6);
    assertEquals("correct charge", 3.22, bt.getCharge(), 1e-6);
  }

  @Test
  public void testToString ()
  {
    BalancingTransaction bt = new BalancingTransaction(baseTime, broker, 42.1, 3.22);
    String sut = bt.toString();
    //System.out.println(sut);
    assertTrue("match", sut.matches("Balance tx \\d+-Sally-42.1-3.22"));
  }
  
  @Test
  public void xmlSerializationTest ()
  {
    BalancingTransaction bt = new BalancingTransaction(baseTime, broker, 42.1, 3.22);
    XStream xstream = new XStream();
    xstream.processAnnotations(BalancingTransaction.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(bt));
    //System.out.println(serialized.toString());
    BalancingTransaction xbt = (BalancingTransaction)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xbt);
    assertEquals("correct time", baseTime.getMillis(), xbt.getPostedTime().getMillis());
    assertEquals("correct qty", 42.1, xbt.getQuantity(), 1e-6);
    assertEquals("correct charge", 3.22, xbt.getCharge(), 1e-6);
  }
}
