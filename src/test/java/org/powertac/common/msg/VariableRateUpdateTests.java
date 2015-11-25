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

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.HourlyCharge;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.thoughtworks.xstream.XStream;

/**
 * Tests for VRU
 * @author John Collins
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
public class VariableRateUpdateTests
{
  private Broker broker;
  private TariffSpecification spec;
  private Instant base;

  @Before
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
    HourlyCharge hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 4.2);
    Rate rate = new Rate().withFixed(false);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertNotNull("created VRU", vru);
    assertEquals("correct rate", rate.getId(), vru.getRateId());
    assertEquals("correct hourly charge", hc.getId(), vru.getHourlyChargeId());
  }

  @Test
  public void testValidConsumption ()
  {
    Rate rate = new Rate().withFixed(false)
            .withMinValue(-.01).withMaxValue(-0.1).withExpectedMean(-0.05);
    HourlyCharge hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("not valid without Rate", vru.isValid());
    assertFalse("out of range", vru.isValid(rate));

    // validity bounds
    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), -0.02);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), -0.01);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), -0.1);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));

    // out of bounds
    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), -0.0099);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("valid", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), -0.1001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("valid", vru.isValid(rate));
  }

  @Test
  public void testValidProduction ()
  {
    Rate rate = new Rate().withFixed(false)
            .withMinValue(.01).withMaxValue(0.1).withExpectedMean(0.05);
    HourlyCharge hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("not valid without Rate", vru.isValid());
    assertFalse("out of range", vru.isValid(rate));

    // validity bounds
    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.02);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.01);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.1);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));

    // out of bounds
    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.0099);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("not valid low", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.1001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("not valid high", vru.isValid(rate));
  }

  @Test
  public void testValidZero()
  {
    Rate rate = new Rate().withFixed(false)
            .withMinValue(0.0).withMaxValue(0.0).withExpectedMean(0.0);
    HourlyCharge hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("out of range", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), -0.0001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("not valid low", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.0001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("not valid high", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.0);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));
  }

  @Test
  public void testValidSpan()
  {
    Rate rate = new Rate().withFixed(false)
            .withMinValue(-0.1).withMaxValue(0.1).withExpectedMean(0.0);
    HourlyCharge hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("out of range", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), -0.10001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("not valid low", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.10001);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertFalse("not valid high", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.0);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), -0.1);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));

    hc = new HourlyCharge(base.plus(TimeService.HOUR * 4), 0.1);
    vru = new VariableRateUpdate(broker, rate, hc);
    assertTrue("valid", vru.isValid(rate));
  }

  @Test
  public void xmlSerialization ()
  {
    Instant when = base.plus(TimeService.HOUR * 4);
    Rate rate = new Rate().withFixed(false);
    rate.setTariffId(spec.getId());
    HourlyCharge hc = new HourlyCharge(when, 4.2);
    VariableRateUpdate vru = new VariableRateUpdate(broker, rate, hc);
    XStream xstream = new XStream();
    xstream.processAnnotations(Rate.class);
    xstream.processAnnotations(HourlyCharge.class);
    xstream.processAnnotations(VariableRateUpdate.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(vru));
    //System.out.println(serialized.toString());
    VariableRateUpdate xvru =
        (VariableRateUpdate)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xvru);
    assertEquals("correct id", vru.getId(), xvru.getId());
    HourlyCharge xhc = xvru.getHourlyCharge();
    assertNotNull("non-null hc", xhc);
    assertEquals("correct hc time", when, xhc.getAtTime());
    //assertEquals("correct rate id", rate.getId(), xhc.getRateId());
  }
}
