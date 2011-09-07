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
package org.powertac.tariffmarket

import java.util.List

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Instant
import org.powertac.common.AbstractCustomer
import org.powertac.common.Broker
import org.powertac.common.PluginConfig
import org.powertac.common.Tariff
import org.powertac.common.TariffSpecification
import org.powertac.common.TariffSubscription
import org.powertac.common.TariffTransaction
import org.powertac.common.TimeService
import org.powertac.common.enumerations.PowerType
import org.powertac.common.enumerations.TariffTransactionType
import org.powertac.common.interfaces.Accounting
import org.powertac.common.interfaces.BrokerProxy
import org.powertac.common.interfaces.CompetitionControl
import org.powertac.common.interfaces.NewTariffListener
import org.powertac.common.interfaces.TimeslotPhaseProcessor
import org.powertac.common.msg.TariffExpire
import org.powertac.common.msg.TariffRevoke
import org.powertac.common.msg.TariffStatus
import org.powertac.common.msg.TariffUpdate
import org.powertac.common.msg.VariableRateUpdate

/**
 * Implements the Tariff Market abstraction. Incoming tariff-related
 * messages from brokers are received and processed, tariffs are published
 * periodically, and subscriptions are processed on behalf of customers.
 * @author John Collins
 */
class TariffMarketService
  implements org.powertac.common.interfaces.TariffMarket,
  org.powertac.common.interfaces.BrokerMessageListener,
  org.powertac.common.interfaces.TimeslotPhaseProcessor 
{
  //static transactional = false

  def timeService
  Accounting accountingService
  CompetitionControl competitionControlService
  BrokerProxy brokerProxyService

  // maps power type to id of corresponding default tariff
  Map defaultTariff = [:]

  // read this from plugin config
  //PluginConfig configuration
  int simulationPhase = 2
  double tariffPublicationFee = 0.0
  double tariffRevocationFee = 0.0
  int publicationInterval = 6
  
  // synchronized queue for incoming messages
  List incoming = []
  Object incomingLock = new Object()
  
  /**
   * Sets up to receive incoming messages. This needs to be done before brokers
   * can log in.
   */
  void setup ()
  {
    brokerProxyService?.registerBrokerTariffListener(this)
  }

  /**
   * Registers for phase 2 activation, to drive tariff publication
   */
  void init (PluginConfig config)
  {
    competitionControlService?.registerTimeslotPhase(this, simulationPhase)
    double fee = config.configuration['tariffPublicationFee']?.toDouble()
    if (fee == null) {
      log.error "Tariff publication fee not specified. Default to ${tariffPublicationFee}"
    }
    else {
      tariffPublicationFee = fee
    }
    fee = config.configuration['tariffRevocationFee']?.toDouble()
    if (fee == null) {
      log.error "Tariff revocation fee not specified. Default to ${tariffPublicationFee}"
    }
    else {
      tariffRevocationFee = fee
    }
    Integer interval = config.configuration['publicationInterval']?.toInteger()
    if (interval == null) {
      log.error "Tariff publication interval not specified. Default to ${publicationInterval}"
    }
    else {
      publicationInterval = interval
    }
  }

  // ----------------- Broker message API --------------------
  /**
   * Receives and dispatches an incoming broker message
   */
  @Override
  public void receiveMessage (msg)
  {
    // queue incoming message
    synchronized(incomingLock) {
      incoming << msg
    }
  }

  /**
   * Process a bogus null input.
   */
  @Override
  public TariffStatus processTariff (junk)
  {
    log.error("bogus tariff input ${junk}")
    return null
  }

  /**
   * Processes a newly-published tariff.
   */
  @Override
  public TariffStatus processTariff (TariffSpecification spec)
  {
    Broker broker = Broker.get(spec.broker.id)
    if (broker == null) {
      log.error("No such broker ${spec}")
      return null
    }
    if (!spec.validate()) {
      log.error("Failed to validate TariffSpec ${spec.id} from ${spec.brokerId}: ${spec.errors.allErrors}")
      return new TariffStatus(broker: spec.broker,
                              tariffId: spec.id, updateId: spec.id,
                              status: TariffStatus.Status.invalidTariff,
                              message: "spec: ${spec.errors.allErrors}")
    }
    spec.save()
    Tariff tariff = new Tariff(tariffSpec: spec)
    tariff.init()
    if (!tariff.validate()) {
      log.error("Failed to validate new Tariff: ${tariff.errors.allErrors}")
      return new TariffStatus(broker: spec.broker,
                              tariffId: spec.id, updateId: spec.id,
                              status: TariffStatus.Status.invalidTariff,
                              message: "tariff: ${tariff.errors.allErrors}")
    }
    else {
      log.info("new tariff ${spec.id}")
      tariff.save()
      broker.addToTariffs(tariff)
      broker.save()
      //newTariffs << tariff
      TariffTransaction pub =
          accountingService.addTariffTransaction(TariffTransactionType.PUBLISH,
                                                 tariff, null, 0, 0.0, tariffPublicationFee)
    }
    return new TariffStatus(broker: spec.broker,
                            tariffId: spec.id, updateId: spec.id,
                            status: TariffStatus.Status.success)
  }

  /**
   * Handles changes in tariff expiration date.
   */
  @Override
  public TariffStatus processTariff (TariffExpire update)
  {
    def (tariff, result) = validateUpdate(update)
    if (tariff == null)
      return result
    else {
      // update expiration date
      tariff.expiration = update.newExpiration
      tariff.save()
      log.info("Tariff ${update.tariffId} expires at ${new DateTime(tariff.expiration, DateTimeZone.UTC).toString()}")
    }
    return success(update)
  }

  /**
   * Handles tariff revocation.
   */
  @Override
  public TariffStatus processTariff (TariffRevoke update)
  {
    def (tariff, result) = validateUpdate(update)
    if (tariff == null)
      return result
    else {
      tariff.state = Tariff.State.KILLED
      tariff.save()
      log.info("Revoke tariff ${update.tariffId}")
      // The actual revocation processing is delegated to the Customer,
      // who is obligated to call getRevokedSubscriptions periodically.

      // If there are active subscriptions, then we have to charge a fee.
      def activeSubscriptions =
          TariffSubscription.findAllByTariff(tariff)
              .findAll { sub -> sub.customersCommitted > 0 }
      if (activeSubscriptions.size() > 0) {
        log.info("Revoked tariff has ${activeSubscriptions.size()} active subscriptions")
        TariffTransaction rev =
            accountingService.addTariffTransaction(TariffTransactionType.REVOKE,
                                                   tariff, null, 0, 0.0,
                                                   tariffRevocationFee)
      }
    }
    return success(update)
  }

  /**
   * Applies a new HourlyCharge to an existing Tariff with a variable Rate.
   */
  @Override
  public TariffStatus processTariff (VariableRateUpdate update)
  {
    def (tariff, result) = validateUpdate(update)
    if (tariff == null)
      return result
    else if (tariff.addHourlyCharge(update.payload, update.rateId)) {
      tariff.save()
      // TODO - do we broadcast this?
      //broadcast << update
      return success(update)
    }
    else {
      // failed to add hourly charge
      new TariffStatus(broker: update.broker,
                       tariffId: update.tariffId,
                       updateId: update.id,
                       status: TariffStatus.Status.invalidUpdate,
                       message: "update: could not add hourly charge")
    }
  }

  // ----------------------- Customer API --------------------------

  def registrations = []

  @Override
  public void registerNewTariffListener (NewTariffListener listener)
  {
    registrations.add(listener)
  }

  // Process queued messages, then
  // handle distribution of new tariffs to customers
  void activate (Instant time, int phase)
  {
    processIncoming()
    if (publicationInterval > 24) {
      log.error "tariff publication interval ${publicationInterval} > 24 hr"
      publicationInterval = 24
    }
    long msec = timeService.currentTime.millis
    if (msec % (publicationInterval * TimeService.HOUR) == 0) {
      // time to publish
      publishTariffs()
    }
  }
  
  void processIncoming()
  {
    while (incoming.size() > 0) {
      def msg
      synchronized (incomingLock) {
        msg = incoming.first()
        incoming = incoming.tail()
      }
      // dispatch incoming message
      def result = processTariff(msg)
      // return non-null result as msg
      if (result != null) {
        brokerProxyService.sendMessage(result.broker, result)
      }
    }
  }

  /**
   * Publishes pending tariffs to customers and brokers
   */
  void publishTariffs ()
  {
    List publishedTariffs = Tariff.findAllByState(Tariff.State.PENDING)
    log.info "publishing ${publishedTariffs.size()} new tariffs"
    publishedTariffs.each { tariff ->
      tariff.state = Tariff.State.OFFERED
      tariff.save()
    }

    def publishedTariffSpecs =  publishedTariffs.collect { it.tariffSpec }
    publishedTariffSpecs.each { spec ->
      log.info "publishing spec ${spec.id}, broker: ${spec.broker.username}, exp: ${spec.expiration}"
    }

    registrations*.publishNewTariffs(publishedTariffs)
    brokerProxyService?.broadcastMessages(publishedTariffSpecs)
  }

  /**
   * Subscribes a block of Customers from a single Customer model to
   * this Tariff, as long as this Tariff has not expired. If the
   * subscription succeeds, then the TariffSubscription instance is
   * return, otherwise null.
   * <p>
   * Note that you cannot unsubscribe directly from a Tariff -- you have to do
   * that from the TariffSubscription that represents the Tariff you want
   * to unsubscribe from.</p>
   */
  @Override
  TariffSubscription subscribeToTariff (Tariff tariff,
                                        AbstractCustomer customer,
                                        int customerCount)
  {
    if (tariff.isExpired())
      return null

    TariffSubscription sub = TariffSubscription.findByTariffAndCustomer(tariff, customer)
    if (sub == null) {
      sub = new TariffSubscription(customer: customer,
          tariff: tariff)
      sub.save()
      // temp fix
      //sub.accountingService = accountingService
    }
    sub.subscribe(customerCount)
    //tariff.addToSubscriptions(sub)
    sub.save()
    log.debug "${sub} id=${sub.id}"
    return sub
  }

  /**
   * Returns the list of active tariffs for the given PowerType
   */
  @Override
  public List<Tariff> getActiveTariffList (PowerType type)
  {
    return Tariff.list().findAll { tariff ->
      !tariff.isExpired() && !tariff.isRevoked() && tariff.getPowerType() == type }
  }

  /**
   * Returns the list of subscriptions for this customer that have been
   * revoked and have non-zero committed customers.
   */
  @Override
  public List<TariffSubscription> getRevokedSubscriptionList (AbstractCustomer customer)
  {
    return TariffSubscription.findAllByCustomer(customer). findAll { sub ->
      sub.tariff.state == Tariff.State.KILLED && sub.customersCommitted > 0 
    }
  }

  /**
   * Returns the default tariff
   */
  @Override
  public Tariff getDefaultTariff (PowerType type)
  {
    return Tariff.get(defaultTariff[type])
  }

  @Override
  public boolean setDefaultTariff (TariffSpecification newSpec)
  {
    if (!newSpec.validate()) {
      log.error("failed to validate default tariff spec ${newSpec}")
      return false
    }
    newSpec.save()
    Tariff tariff = new Tariff(tariffSpec: newSpec)
    tariff.init()
    if (!tariff.validate()) {
      log.error("failed to validate default tariff ${newSpec}")
      return false
    }
    if (!tariff.save()) {
      log.error("failed to save default tariff ${newSpec}")
      return false
    }
    defaultTariff[newSpec.getPowerType()] = tariff.id
    return true
  }

  @Override
  public List<TariffTransaction> getTransactions ()
  {
    // TODO Auto-generated method stub
    return null;
  }

  private List validateUpdate (TariffUpdate update)
  {
    Broker broker = Broker.get(update.broker.id)
    if (!update.validate()) {
      log.error("Failed to validate TariffUpdate: ${update.errors.allErrors}")
      return [
        null,
        new TariffStatus(broker: broker,
                         tariffId: update.tariffId, updateId: update.id,
                         status: TariffStatus.Status.invalidUpdate,
                         message: "update: ${update.errors.allErrors}")
      ]
    }
    Tariff tariff = Tariff.findBySpecId(update.tariffId)
    if (tariff == null) {
      log.error("update - no such tariff ${update.tariffId}, broker ${update.brokerId}")
      return [
        null,
        new TariffStatus(broker: broker,
                         tariffId: update.tariffId, updateId: update.id,
                         status: TariffStatus.Status.noSuchTariff)
      ]
    }
    return [tariff, null]
  }

  private TariffStatus success (TariffUpdate update)
  {
    Broker broker = Broker.get(update.broker.id)
    return new TariffStatus(broker: broker,
                            tariffId: update.tariffId, updateId: update.id,
                            status: TariffStatus.Status.success)
  }
}
