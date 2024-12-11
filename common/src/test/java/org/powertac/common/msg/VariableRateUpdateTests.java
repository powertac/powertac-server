/* Copyright (c) 2013 by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common.msg;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.*;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for VRU
 * @author John Collins
 */
@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class VariableRateUpdateTests
{
  private Broker broker;
  private TariffSpecification spec;
  private Instant base;

  @BeforeEach
  public void setUp () throws Exception
  {
    Competition.newInstance("test");
    base = Competition.currentCompetition().getSimulationBaseTime();
    broker = new Broker("Jenny");
    BrokerRepo brokerRepo = new BrokerRepo();
    brokerRepo.add(broker);
    spec = new TariffSpecification(broker, PowerType.INTERRUPTIBLE_CONSUMPTION);
  }

  @Test
  public void testCreate ()
  {
    HourlyCharge hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 4.2);
    Rate rate = new Rate().withFixed(false);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertNotNull(vru, "created VRU");
    assertEquals(rate.getId(), vru.getRateId(), "correct rate");
    assertEquals(hc.getId(), vru.getHourlyChargeId(), "correct hourly charge");
  }

  @Test
  public void testValidConsumption ()
  {
    Rate rate = new Rate().withFixed(false)
            .withMinValue(-.01).withMaxValue(-0.1).withExpectedMean(-0.05);
    HourlyCharge hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(), "not valid without Rate");
    assertFalse(vru.isValid(rate), "out of range");

    // validity bounds
    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), -0.02);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), -0.01);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), -0.1);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");

    // out of bounds
    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), -0.0099);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "valid");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), -0.1001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "valid");
  }

  @Test
  public void testValidProduction ()
  {
    Rate rate = new Rate().withFixed(false)
            .withMinValue(.01).withMaxValue(0.1).withExpectedMean(0.05);
    HourlyCharge hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(), "not valid without Rate");
    assertFalse(vru.isValid(rate), "out of range");

    // validity bounds
    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.02);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.01);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.1);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");

    // out of bounds
    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.0099);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "not valid low");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.1001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "not valid high");
  }

  @Test
  public void testValidZero()
  {
    Rate rate = new Rate().withFixed(false)
            .withMinValue(0.0).withMaxValue(0.0).withExpectedMean(0.0);
    HourlyCharge hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "out of range");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), -0.0001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "not valid low");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.0001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "not valid high");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.0);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");
  }

  @Test
  public void testValidSpan()
  {
    Rate rate = new Rate().withFixed(false)
            .withMinValue(-0.1).withMaxValue(0.1).withExpectedMean(0.0);
    HourlyCharge hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "out of range");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), -0.10001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "not valid low");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.10001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse(vru.isValid(rate), "not valid high");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.0);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), -0.1);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");

    hc = new HourlyCharge(base.plusMillis(TimeService.HOUR * 4), 0.1);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue(vru.isValid(rate), "valid");
  }

  @Test
  public void xmlSerialization ()
  {
    Instant when = base.plusMillis(TimeService.HOUR * 4);
    Rate rate = new Rate().withFixed(false);
    rate.setTariffId(spec.getId());
    HourlyCharge hc = new HourlyCharge(when, 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(Rate.class);
    xstream.processAnnotations(HourlyCharge.class);
    xstream.processAnnotations(VariableRateUpdate.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(vru));
    //System.out.println(serialized.toString());
    VariableRateUpdate xvru = (VariableRateUpdate)xstream.fromXML(serialized.toString());
    assertNotNull(xvru, "deserialized something");
    assertEquals(vru.getId(), xvru.getId(), "correct id");
    HourlyCharge xhc = xvru.getHourlyCharge();
    assertNotNull(xhc, "non-null hc");
    assertEquals(when, xhc.getAtTime(), "correct hc time");
    //assertEquals(rate.getId(), xhc.getRateId(), "correct rate id");
  }
}
