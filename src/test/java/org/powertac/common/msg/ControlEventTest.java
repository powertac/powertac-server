package org.powertac.common.msg;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class ControlEventTest
{
  @Autowired
  private TariffRepo tariffRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private TimeService timeService;
  
  private Broker broker;
  private TariffSpecification spec;

  @Before
  public void setUp () throws Exception
  {
    tariffRepo.recycle();
    timeslotRepo.recycle();
    Competition.newInstance("test");
    broker = new Broker("Jenny");
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION);
    tariffRepo.addSpecification(spec);
    Instant baseTime = new Instant();
    timeService.setCurrentTime(baseTime);
    timeslotRepo.makeTimeslot(baseTime);
  }

  @Test
  public void testBalancingControlEvent ()
  {
    BalancingControlEvent bce =
        new BalancingControlEvent(spec, 21.0, 2.1, 0);
    assertEquals("correct tariff", spec.getId(), bce.getTariffId());
    assertEquals("correct amount", 21.0, bce.getKwh(), 1e-6);
    assertEquals("correct payment", 2.1, bce.getPayment(), 1e-6);
  }
  
  @Test
  public void testEconomicControlEvent()
  {
    EconomicControlEvent ece =
        new EconomicControlEvent(spec, 0.4, 0);
    assertEquals("correct tariff", spec.getId(), ece.getTariffId());
    assertEquals("correct ratio", 0.4, ece.getCurtailmentRatio(), 1e-6);
    assertEquals("correct timeslot", 0, ece.getTimeslotIndex());
  }

  @Test
  public void xmlSerializationBalancing ()
  {
    BalancingControlEvent bce =
        new BalancingControlEvent(spec, 23.0, 2.3, 0);
    XStream xstream = new XStream();
    xstream.processAnnotations(BalancingControlEvent.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(bce));
    //System.out.println(serialized.toString());
    BalancingControlEvent xbce =
        (BalancingControlEvent)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xbce);
    assertEquals("correct spec", spec.getId(), xbce.getTariffId());
    assertEquals("correct kwh", 23.0, xbce.getKwh(), 1e-6);
    assertEquals("correct payment", 2.3, xbce.getPayment(), 1e-6);
  }

  @Test
  public void xmlSerializationEconomic ()
  {
    EconomicControlEvent bce =
        new EconomicControlEvent(spec, 0.3, 0);
    XStream xstream = new XStream();
    xstream.processAnnotations(EconomicControlEvent.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(bce));
    //System.out.println(serialized.toString());
    EconomicControlEvent xbce =
        (EconomicControlEvent)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xbce);
    assertEquals("correct spec", spec.getId(), xbce.getTariffId());
    assertEquals("correct ratio", 0.3, xbce.getCurtailmentRatio(), 1e-6);
    assertEquals("correct timeslot", 0, xbce.getTimeslotIndex());
  }

}
