/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.common;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;
import java.util.List;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.BrokerRepo;
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
public class TariffSpecificationTests
{
  @Autowired
  private TimeService timeService;
  
  private Broker broker;
  private Instant now;

  @BeforeEach
  public void setUp () throws Exception
  {
    timeService.setCurrentTime( ZonedDateTime.now( ZoneOffset.UTC).toInstant());
    now = timeService.getCurrentTime();
    broker = new Broker("Jenny");
    BrokerRepo repo = new BrokerRepo();
    repo.add(broker);
  }

  @Test
  public void testTariffSpecification ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertNotNull(spec, "made something");
    assertEquals(broker, spec.getBroker(), "correct broker");
    assertEquals(PowerType.CONSUMPTION, spec.getPowerType(), "correct power type");
    assertNull(spec.getExpiration(), "no expiration");
    assertEquals(0l, spec.getMinDuration(), "default minDuration");
    assertEquals(0.0, spec.getSignupPayment(), 1e-6, "default signup");
    assertEquals(0.0, spec.getEarlyWithdrawPayment(), 1e-6, "default withdraw");
    assertEquals(0.0, spec.getPeriodicPayment(), 1e-6, "default periodic");
    assertEquals(0, spec.getRates().size(), "no rates");
    assertNull(spec.getSupersedes(), "no supersedes");
  }

  @Test
  public void testSetExpiration ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    Instant exp = now.plusMillis(TimeService.HOUR * 24).truncatedTo(ChronoUnit.MILLIS);
    assertEquals(spec, spec.withExpiration(exp), "correct return");
    assertEquals(exp, spec.getExpiration(), "correct value");
  }

  @Test
  public void testSetMinDuration ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertEquals(spec, spec.withMinDuration(10000l), "correct return");
    assertEquals(10000l, spec.getMinDuration(), "correct value");
  }

  @Test
  public void testSetSignupPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals(PowerType.PRODUCTION, spec.getPowerType(), "correct power type");
    assertEquals(spec, spec.withSignupPayment(10.00), "correct return");
    assertEquals(10.00, spec.getSignupPayment(), 1e-6, "correct value");
  }

  @Test
  public void testSetEarlyWithdrawPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals(spec, spec.withEarlyWithdrawPayment(-20.00), "correct return");
    assertEquals(-20.00, spec.getEarlyWithdrawPayment(), 1e-6, "correct value");
  }

  @Test
  public void testSetPeriodicPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals(spec, spec.withPeriodicPayment(-0.01), "correct return");
    assertEquals(-0.01, spec.getPeriodicPayment(), 1e-6, "correct value");
  }

  @Test
  public void testAddRate ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    Rate r = new Rate().withValue(0.121);
    assertEquals(spec, spec.addRate(r), "correct return");
    assertEquals(1, spec.getRates().size(), "correct length");
    assertEquals(r, spec.getRates().get(0), "correct rate");
  }

  @Test
  public void testAddRates ()
  {
    TariffSpecification spec = new TariffSpecification(broker,
                                                       PowerType.CONSUMPTION);
    Rate r = new Rate().withValue(0.-121);
    RegulationRate rr = new RegulationRate().
            withUpRegulationPayment(.05).
            withDownRegulationPayment(-.05);
    assertEquals(spec, spec.addRate(r), "correct return");
    assertEquals(spec, spec.addRate(rr), "correct return");
    assertEquals(1, spec.getRates().size(), "correct length");
    assertEquals(r, spec.getRates().get(0), "correct rate");
    assertEquals(1, spec.getRegulationRates().size(), "correct length");
    assertEquals(rr, spec.getRegulationRates().get(0), "correct rate");
  }

  @Test
  public void testAddSupersedes ()
  {
    TariffSpecification spec1 = new TariffSpecification(broker, PowerType.CONSUMPTION);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertEquals(spec2, spec2.addSupersedes(spec1.getId()), "correct return");
    assertEquals(1, spec2.getSupersedes().size(), "correct length");
    assertEquals(spec1.getId(), (long)spec2.getSupersedes().get(0), "correct first element");
  }

  // validity test
  @Test
  public void  testValidity ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertFalse(spec.isValid(), "invalid - no rate");
    Rate r = new Rate().withValue(0.1);
    spec.addRate(r);
    assertTrue(spec.isValid(), "valid - has rate");
    r.withMinValue(-.1).withFixed(false);
    assertFalse(spec.isValid(), "invalid - bad rate");
    r.withMaxValue(-0.3).withExpectedMean(-0.2);
    assertTrue(spec.isValid(), "valid rate");
    spec.withMinDuration(-1l);
    assertFalse(spec.isValid(), "invalid - bad minDuration");
  }

  @Test
  public void testXmlSerialization ()
  {
    Rate r = new Rate().withValue(-0.121).
            withDailyBegin(ZonedDateTime.of(2011, 1, 1, 6, 0, 0, 0,  ZoneOffset.UTC)).
            withDailyEnd(ZonedDateTime.of(2011, 1, 1, 8, 0, 0, 0,  ZoneOffset.UTC)).
            withTierThreshold(100.0);
    RegulationRate rr = new RegulationRate().
            withUpRegulationPayment(.05).
            withDownRegulationPayment(-.05).
            withResponse(RegulationRate.ResponseTime.SECONDS);
    Instant now = timeService.getCurrentTime();
    TariffSpecification spec = 
            new TariffSpecification(broker,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(20000l).
                                    withSignupPayment(35.0).
                                    withPeriodicPayment(-0.05).
                                    withExpiration(now.plusMillis(TimeService.DAY * 2)).
                                    addSupersedes(42l).
                                    addRate(r).
                                    addRate(rr);

    XStream xstream = XMLMessageConverter.getXStream();
    xstream.autodetectAnnotations(true);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(spec));
    //System.out.println(serialized.toString());
    TariffSpecification xspec= (TariffSpecification)xstream.fromXML(serialized.toString());
    assertNotNull(xspec, "deserialized something");
    //assertEquals(spec, xspec, "correct match");
    assertEquals(35.0, xspec.getSignupPayment(), 1e-6, "correct signup");
    List<Long> supersedes = xspec.getSupersedes();
    assertNotNull(supersedes, "non-empty supersedes list");
    assertEquals(1, supersedes.size(), "one entry");
    assertEquals(42l, supersedes.get(0).longValue(), "correct entry");
    assertEquals(now.plusMillis(TimeService.DAY * 2).truncatedTo(ChronoUnit.MILLIS) , xspec.getExpiration(), "correct expiration");
    Rate xr = (Rate)xspec.getRates().get(0);
    assertNotNull(xr, "rate present");
    assertTrue(xr.isFixed(), "correct rate type");
    assertEquals(-0.121, xr.getMinValue(), 1e-6, "correct rate value");
    RegulationRate xrr = (RegulationRate)xspec.getRegulationRates().get(0);
    assertNotNull(xrr, "rate present");
    assertEquals(.05, xrr.getUpRegulationPayment(), 1e-6, "correct up-reg rate");
    assertEquals(-.05, xrr.getDownRegulationPayment(), 1e-6, "correct down-reg rate");
    assertEquals(RegulationRate.ResponseTime.SECONDS, xrr.getResponse(), "correct response time");
  }
}
