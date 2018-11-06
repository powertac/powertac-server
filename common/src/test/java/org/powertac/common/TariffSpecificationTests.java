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

import static org.junit.Assert.*;

import java.io.StringWriter;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import com.thoughtworks.xstream.XStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
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

  @Before
  public void setUp () throws Exception
  {
    timeService.setCurrentTime(new DateTime());
    now = timeService.getCurrentTime();
    broker = new Broker("Jenny");
    BrokerRepo repo = new BrokerRepo();
    repo.add(broker);
  }

  @Test
  public void testTariffSpecification ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertNotNull("made something", spec);
    assertEquals("correct broker", broker, spec.getBroker());
    assertEquals("correct power type", PowerType.CONSUMPTION, spec.getPowerType());
    assertNull("no expiration", spec.getExpiration());
    assertEquals("default minDuration", 0l, spec.getMinDuration());
    assertEquals("default signup", 0.0, spec.getSignupPayment(), 1e-6);
    assertEquals("default withdraw", 0.0, spec.getEarlyWithdrawPayment(), 1e-6);
    assertEquals("default periodic", 0.0, spec.getPeriodicPayment(), 1e-6);
    assertEquals("no rates", 0, spec.getRates().size());
    assertNull("no supersedes", spec.getSupersedes());
  }

  @Test
  public void testSetExpiration ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    Instant exp = now.plus(TimeService.HOUR * 24);
    assertEquals("correct return", spec, spec.withExpiration(exp));
    assertEquals("correct value", exp, spec.getExpiration());
  }

  @Test
  public void testSetMinDuration ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertEquals("correct return", spec, spec.withMinDuration(10000l));
    assertEquals("correct value", 10000l, spec.getMinDuration());
  }

  @Test
  public void testSetSignupPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals("correct power type", PowerType.PRODUCTION, spec.getPowerType());
    assertEquals("correct return", spec, spec.withSignupPayment(10.00));
    assertEquals("correct value", 10.00, spec.getSignupPayment(), 1e-6);
  }

  @Test
  public void testSetEarlyWithdrawPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals("correct return", spec, spec.withEarlyWithdrawPayment(-20.00));
    assertEquals("correct value", -20.00, spec.getEarlyWithdrawPayment(), 1e-6);
  }

  @Test
  public void testSetPeriodicPayment ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    assertEquals("correct return", spec, spec.withPeriodicPayment(-0.01));
    assertEquals("correct value", -0.01, spec.getPeriodicPayment(), 1e-6);
  }

  @Test
  public void testAddRate ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.PRODUCTION);
    Rate r = new Rate().withValue(0.121);
    assertEquals("correct return", spec, spec.addRate(r));
    assertEquals("correct length", 1, spec.getRates().size());
    assertEquals("correct rate", r, spec.getRates().get(0));
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
    assertEquals("correct return", spec, spec.addRate(r));
    assertEquals("correct return", spec, spec.addRate(rr));
    assertEquals("correct length", 1, spec.getRates().size());
    assertEquals("correct rate", r, spec.getRates().get(0));
    assertEquals("correct length", 1, spec.getRegulationRates().size());
    assertEquals("correct rate", rr, spec.getRegulationRates().get(0));
  }

  @Test
  public void testAddSupersedes ()
  {
    TariffSpecification spec1 = new TariffSpecification(broker, PowerType.CONSUMPTION);
    TariffSpecification spec2 = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertEquals("correct return", spec2, spec2.addSupersedes(spec1.getId()));
    assertEquals("correct length", 1, spec2.getSupersedes().size());
    assertEquals("correct first element", spec1.getId(), (long)spec2.getSupersedes().get(0));
  }

  // validity test
  @Test
  public void  testValidity ()
  {
    TariffSpecification spec = new TariffSpecification(broker, PowerType.CONSUMPTION);
    assertFalse("invalid - no rate", spec.isValid());
    Rate r = new Rate().withValue(0.1);
    spec.addRate(r);
    assertTrue("valid - has rate", spec.isValid());
    r.withMinValue(-.1).withFixed(false);
    assertFalse("invalid - bad rate", spec.isValid());
    r.withMaxValue(-0.3).withExpectedMean(-0.2);
    assertTrue("valid rate", spec.isValid());
    spec.withMinDuration(-1l);
    assertFalse("invalid - bad minDuration", spec.isValid());
  }

  @Test
  public void testXmlSerialization ()
  {
    Rate r = new Rate().withValue(-0.121).
            withDailyBegin(new DateTime(2011, 1, 1, 6, 0, 0, 0, DateTimeZone.UTC)).
            withDailyEnd(new DateTime(2011, 1, 1, 8, 0, 0, 0, DateTimeZone.UTC)).
            withTierThreshold(100.0);
    RegulationRate rr = new RegulationRate().
            withUpRegulationPayment(.05).
            withDownRegulationPayment(-.05).
            withResponse(RegulationRate.ResponseTime.SECONDS);
    TariffSpecification spec = 
            new TariffSpecification(broker,
                                    PowerType.CONSUMPTION).
                                    withMinDuration(20000l).
                                    withSignupPayment(35.0).
                                    withPeriodicPayment(-0.05).
                                    addSupersedes(42l).
                                    addRate(r).
                                    addRate(rr);

    XStream xstream = XMLMessageConverter.getXStream();
    xstream.autodetectAnnotations(true);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(spec));
//    System.out.println(serialized.toString());
    TariffSpecification xspec= (TariffSpecification)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xspec);
    //assertEquals("correct match", spec, xspec);
    assertEquals("correct signup", 35.0, xspec.getSignupPayment(), 1e-6);
    List<Long> supersedes = xspec.getSupersedes();
    assertNotNull("non-empty supersedes list", supersedes);
    assertEquals("one entry", 1, supersedes.size());
    assertEquals("correct entry", new Long(42l), supersedes.get(0));
    Rate xr = (Rate)xspec.getRates().get(0);
    assertNotNull("rate present", xr);
    assertTrue("correct rate type", xr.isFixed());
    assertEquals("correct rate value", -0.121, xr.getMinValue(), 1e-6);
    RegulationRate xrr = (RegulationRate)xspec.getRegulationRates().get(0);
    assertNotNull("rate present", xrr);
    assertEquals("correct up-reg rate",
                 .05, xrr.getUpRegulationPayment(), 1e-6);
    assertEquals("correct down-reg rate",
                 -.05, xrr.getDownRegulationPayment(), 1e-6);
    assertEquals("correct response time",
                 RegulationRate.ResponseTime.SECONDS, xrr.getResponse());
  }
}
