package org.powertac.common.msg;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.*;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.TariffRepo;
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

  @BeforeEach
  public void setUp () throws Exception
  {
    tariffRepo.recycle();
    timeslotRepo.recycle();
    Competition.newInstance("test");
    broker = new Broker("Jenny");
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION);
    tariffRepo.addSpecification(spec);
    Instant baseTime = Instant.now();
    timeService.setCurrentTime(baseTime);
    timeslotRepo.makeTimeslot(baseTime);
  }

  @Test
  public void testBalancingControlEvent ()
  {
    BalancingControlEvent bce = new BalancingControlEvent(spec, 21.0, 2.1, 0);
    assertEquals(spec.getId(), bce.getTariffId(), "correct tariff");
    assertEquals(21.0, bce.getKwh(), 1e-6, "correct amount");
    assertEquals(2.1, bce.getPayment(), 1e-6, "correct payment");
  }
  
  @Test
  public void testEconomicControlEvent()
  {
    EconomicControlEvent ece = new EconomicControlEvent(spec, 0.4, 0);
    assertEquals(spec.getId(), ece.getTariffId(), "correct tariff");
    assertEquals(0.4, ece.getCurtailmentRatio(), 1e-6, "correct ratio");
    assertEquals(0, ece.getTimeslotIndex(), "correct timeslot");
  }

  @Test
  public void xmlSerializationBalancing ()
  {
    BalancingControlEvent bce =
        new BalancingControlEvent(spec, 23.0, 2.3, 0);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(BalancingControlEvent.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(bce));
    //System.out.println(serialized.toString());
    BalancingControlEvent xbce = (BalancingControlEvent)xstream.fromXML(serialized.toString());
    assertNotNull(xbce, "deserialized something");
    assertEquals(spec.getId(), xbce.getTariffId(), "correct spec");
    assertEquals(23.0, xbce.getKwh(), 1e-6, "correct kwh");
    assertEquals(2.3, xbce.getPayment(), 1e-6, "correct payment");
  }

  @Test
  public void xmlSerializationEconomic ()
  {
    EconomicControlEvent bce =
        new EconomicControlEvent(spec, 0.3, 0);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(EconomicControlEvent.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(bce));
    //System.out.println(serialized.toString());
    EconomicControlEvent xbce = (EconomicControlEvent)xstream.fromXML(serialized.toString());
    assertNotNull(xbce, "deserialized something");
    assertEquals(spec.getId(), xbce.getTariffId(), "correct spec");
    assertEquals(0.3, xbce.getCurtailmentRatio(), 1e-6, "correct ratio");
    assertEquals(0, xbce.getTimeslotIndex(), "correct timeslot");
  }

}
