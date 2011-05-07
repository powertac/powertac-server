/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.server

import org.hibernate.SessionFactory
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.powertac.common.Broker
import org.powertac.common.BrokerRole
import org.powertac.common.Competition
import org.powertac.common.PluginConfig
import org.powertac.common.Rate
import org.powertac.common.TariffSpecification
import org.powertac.common.Tariff
import org.powertac.common.TimeService
import org.powertac.common.msg.TariffStatus
import com.thoughtworks.xstream.*

import grails.test.*
import org.powertac.common.Shout
import org.powertac.common.enumerations.BuySellIndicator
import org.powertac.common.enumerations.ProductType
import org.powertac.common.Timeslot

/**
 * Tests for the Broker Proxy Service.
 * @author John Collins, Daniel Schnurr
 */
class BrokerProxyServiceTests extends GroovyTestCase 
{
  
  def timeService
  def tariffMarketInitializationService
  def tariffMarketService
  def brokerProxyService
  def competitionControlService
  SessionFactory sessionFactory

  Broker bob
  def bobMsgs = []
  Broker jim
  def jimMsgs = []
  
  Competition comp
  TariffSpecification tariffSpec
  Shout incomingShout
  ProductType sampleProduct
  Timeslot sampleTimeslot

  protected void setUp() 
  {
    super.setUp()
    
    // clean up from earlier tests
    //Rate.list()*.delete()
    TariffSpecification.list()*.delete()
    Tariff.list()*.delete()
    //Broker.list().each { BrokerRole.removeAll(it);  it.delete() }
    
    // create a Competition, needed for initialization
    if (Competition.count() == 0) {
      comp = new Competition(name: 'broker-proxy-test')
      assert comp.save()
    }
    else {
      comp = Competition.list().first()
    }

    // init time service
    def start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant()
    timeService.setCurrentTime(start)
    
    // set up plugins
    //competitionControlService.preGame()
    competitionControlService.configurePlugins()
    
    // initialize the tariff market
    //PluginConfig.findByRoleName('TariffMarket')?.delete()
    tariffMarketInitializationService.setDefaults()
    tariffMarketInitializationService.initialize(comp, ['AccountingService'])

    Broker.findByUsername('Bob')?.delete()
    bob = new Broker(username: "Bob", local: true)
    bob.testProxy = [receive: { msg -> bobMsgs << msg }]
    assert (bob.save())
    Broker.findByUsername('Jim')?.delete()
    jim = new Broker(username: "Jim", local: true)
    assert (jim.save())
    
    Instant exp = new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant()
    tariffSpec = new TariffSpecification(broker: bob, expiration: exp,
                                         minDuration: TimeService.WEEK * 8)
    Rate r1 = new Rate(value: 0.121)
    tariffSpec.rates = [r1]
    println "tariffSpec id: ${tariffSpec.id}, r1 id: ${r1.id}"

    // Shout initialization
    sampleProduct = ProductType.Future
    sampleTimeslot = new Timeslot(serialNumber: 1,
                                  startInstant: new Instant(start.millis + TimeService.HOUR),
                                  endInstant: new Instant(start.millis + TimeService.HOUR * 2),
                                  enabled: true)
    assert (sampleTimeslot.save())
    incomingShout = new Shout(broker: bob, product: sampleProduct, timeslot: sampleTimeslot, buySellIndicator: BuySellIndicator.BUY)
  }

  protected void tearDown() 
  {
    super.tearDown()
  }

  void testTariffProcess() 
  {
    //serialize a tariff spec
    XStream xstream = new XStream()
    xstream.processAnnotations(TariffSpecification.class)
    xstream.processAnnotations(Rate.class)
    String xml = xstream.toXML(tariffSpec)

    // delete the spec so we can save it after deserializing
    tariffSpec.delete()
    
    // send the message through the proxy service
    brokerProxyService.receiveMessage(xml)
    TariffStatus status = bobMsgs[0]
    assertNotNull("non-null status", status)

  }

  void testLocalBroadcastMessage() 
  {
     brokerProxyService.broadcastMessage(tariffSpec)
     def receivedMessage = bobMsgs[0]
     assertNotNull("non-null tariffSpec", receivedMessage)
     assertEquals(tariffSpec, receivedMessage)
  }
  
  void testPersistence ()
  {
    // save original tariffSpec id
    String tsid = tariffSpec.id
    
    XStream xstream = new XStream()
    xstream.processAnnotations(TariffSpecification.class)
    xstream.processAnnotations(Rate.class)
    String xml = xstream.toXML(tariffSpec)

    // clear the current session to avoid unique-id conflict
    sessionFactory.currentSession.clear()
    
    // send the message through the proxy service
    // result should be three tariffs, because the defaults are there also
    brokerProxyService.receiveMessage(xml)
    List<TariffSpecification> tss = TariffSpecification.list()
    assertEquals("3 specs", 3, tss.size())
    assertEquals("correct id", tsid, tss[2].id)
    assertNotSame("different object", tariffSpec, tss[0])
  }

  void testShoutProcess()
  {
    incomingShout.limitPrice = 10.0
    incomingShout.quantity = 200.0

    //serialize a tariff spec
    XStream xstream = new XStream()
    xstream.processAnnotations(Shout.class)
    xstream.processAnnotations(Timeslot.class)
    String xml = xstream.toXML(incomingShout)

    //delete shout
    incomingShout.delete()

    // send the message through the proxy service
    brokerProxyService.receiveMessage(xml)

    Shout persistedShout = Shout.findByBroker(bob)
    assertEquals(sampleTimeslot, persistedShout.timeslot)
    assertEquals(BuySellIndicator.BUY, persistedShout.buySellIndicator)
    assertEquals(10.0, persistedShout.limitPrice)
    assertEquals(200.0, persistedShout.quantity)

    //Todo: product is not deserialized correctly
    //assertEquals(sampleProduct, persistedShout.product)

  }

}
