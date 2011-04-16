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

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.powertac.common.Broker;
import org.powertac.common.Rate
import org.powertac.common.TariffSpecification
import org.powertac.common.TimeService
import org.powertac.common.msg.TariffStatus
import com.thoughtworks.xstream.*

import grails.test.*

/**
 * Tests for the Broker Proxy Service.
 * @author John Collins
 */
class BrokerProxyServiceTests extends GroovyTestCase 
{
  
  def timeService
  def tariffMarketService
  def brokerProxyService

  Broker bob
  def bobMsgs = []
  Broker jim
  def jimMsgs = []
  
  TariffSpecification tariffSpec

  protected void setUp() 
  {
    super.setUp()
    
    // init time service
    def start = new DateTime(2011, 1, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant()
    timeService.setCurrentTime(start)

    bob = new Broker(username: "Bob", local: true)
    assert (bob.save())
    def bobProxy =
      [receive: { msg -> bobMsgs << msg }]
    bob.testProxy = bobProxy
    jim = new Broker(username: "Jim", local: true)
    assert (jim.save())
    
    Instant exp = new DateTime(2011, 3, 1, 12, 0, 0, 0, DateTimeZone.UTC).toInstant()
    tariffSpec = new TariffSpecification(broker: bob, expiration: exp,
                                         minDuration: TimeService.WEEK * 8)
    Rate r1 = new Rate(value: 0.121)
    tariffSpec.rates = [r1]
    //assert tariffSpec.save()
    tariffMarketService.afterPropertiesSet()
  }

  protected void tearDown() 
  {
    super.tearDown()
  }

  void testTariffProcess() 
  {
    XStream xstream = new XStream()
    xstream.processAnnotations(TariffSpecification.class)
    xstream.processAnnotations(Rate.class)
    
    tariffSpec.id = 't1'
    tariffSpec.rates[0].id = 'r1'
    String xml = xstream.toXML(tariffSpec)

//    brokerProxyService.receiveMessage(xml)
//    TariffStatus status = bobMsgs[0]
//    assertNotNull("non-null status", status)

  }

  void testLocalBroadcastMessage() {
     brokerProxyService.broadcastMessage(tariffSpec)
     def receivedMessage = bobMsgs[0]
     assertNotNull("non-null tariffSpec", receivedMessage)
     assertEquals(tariffSpec, receivedMessage)
  }
}
