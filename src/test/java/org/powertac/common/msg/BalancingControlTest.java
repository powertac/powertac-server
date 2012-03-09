package org.powertac.common.msg;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;
import org.powertac.common.TariffSpecification;
import org.powertac.common.enumerations.PowerType;

import com.thoughtworks.xstream.XStream;

public class BalancingControlTest
{
  private Broker broker;
  private TariffSpecification spec;

  @Before
  public void setUp () throws Exception
  {
    broker = new Broker("Jenny");
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION);
  }

  @Test
  public void testBalancingControlEvent ()
  {
    BalancingControlEvent bce = new BalancingControlEvent(spec.getId(), 21.0);
    assertEquals("correct tariff", spec.getId(), bce.getTariffId());
    assertEquals("correct amount", 21.0, bce.getKwh(), 1e-6);
  }

  @Test
  public void xmlSerializationTest ()
  {
    BalancingControlEvent bce =
        new BalancingControlEvent(spec.getId(), 23.0);
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
  }

}
