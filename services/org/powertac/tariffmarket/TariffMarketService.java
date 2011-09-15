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
package org.powertac.tariffmarket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.powertac.accounting.AccountingService;
import org.powertac.common.AbstractCustomer;
import org.powertac.common.Broker;
import org.powertac.common.PluginConfig;
import org.powertac.common.Tariff;
import org.powertac.common.TariffMessage;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.enumerations.TariffTransactionType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TariffMessageProcessor;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.TariffUpdate;
import org.powertac.common.msg.VariableRateUpdate;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implements the Tariff Market abstraction. Incoming tariff-related
 * messages from brokers are received and processed, tariffs are published
 * periodically, and subscriptions are processed on behalf of customers.
 * @author John Collins
 */
@Service
public class TariffMarketService
  implements TariffMarket, BrokerMessageListener,
    TimeslotPhaseProcessor, TariffMessageProcessor 
{
  static private Logger log = Logger.getLogger(TariffMarketService.class.getName());

  @Autowired
  private TimeService timeService;
  
  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private CompetitionControl competitionControlService;
  
  @Autowired
  private BrokerProxy brokerProxyService;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  @Autowired
  private TariffRepo tariffRepo;
  
  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;

  // maps power type to id of corresponding default tariff
  private HashMap<PowerType, Long> defaultTariff;

  // read this from plugin config
  //PluginConfig configuration
  private int simulationPhase = 2;
  private double tariffPublicationFee = 0.0;
  private double tariffRevocationFee = 0.0;
  private int publicationInterval = 6;
  
  // synchronized queue for incoming messages
  private List<TariffMessage> incoming;
  private Object incomingLock = new Object();
  
  /**
   * Default constructor
   */
  public TariffMarketService ()
  {
    super();
  }
  
  /**
   * Sets up to receive incoming messages. This needs to be done before brokers
   * can log in.
   */
  private void setup ()
  {
    brokerProxyService.registerBrokerTariffListener(this);
  }

  /**
   * Registers for phase 2 activation, to drive tariff publication
   */
  public void init (PluginConfig config)
  {
    competitionControlService.registerTimeslotPhase(this, simulationPhase);
    String value = config.getConfigurationValue("tariffPublicationFee");
    if (value == null) {
      log.error("Tariff publication fee not specified. Default to " + tariffPublicationFee);
    }
    else {
      tariffPublicationFee = Double.parseDouble(value);
    }
    value = config.getConfigurationValue("tariffRevocationFee");
    if (value == null) {
      log.error("Tariff revocation fee not specified. Default to " + tariffPublicationFee);
    }
    else {
      tariffRevocationFee = Double.parseDouble(value);
    }
    value = config.getConfigurationValue("publicationInterval");
    if (value == null) {
      log.error("Tariff publication interval not specified. Default to " + publicationInterval);
    }
    else {
      publicationInterval = Integer.parseInt(value);
    }
  }

  // ----------------- Data access -------------------------

  public double getTariffPublicationFee ()
  {
    return tariffPublicationFee;
  }

  public double getTariffRevocationFee ()
  {
    return tariffRevocationFee;
  }

  public int getPublicationInterval ()
  {
    return publicationInterval;
  }  
  
  // default visibility for test support
  int getSimulationPhase ()
  {
    return simulationPhase;
  }

  List<NewTariffListener> getRegistrations ()
  {
    return registrations;
  }

  // ----------------- Broker message API --------------------
  /**
   * Receives and dispatches an incoming broker message
   */
  @Override
  public void receiveMessage (Object msg)
  {
    // queue incoming message
    synchronized(incomingLock) {
      incoming.add((TariffMessage)msg);
    }
  }

  /**
   * Process a bogus null input.
   */
  //@Override
  //public TariffStatus processTariff (Object junk)
  //{
  //  log.error("bogus tariff input ${junk}");
  //  return null;
  //}

  /**
   * Processes a newly-published tariff.
   */
  @Override
  public TariffStatus processTariff (TariffSpecification spec)
  {
    Tariff tariff = new Tariff(spec);
    tariff.init();
    log.info("new tariff ${spec.id}");
    TariffTransaction pub =
      accountingService.addTariffTransaction(TariffTransactionType.PUBLISH,
                                             tariff, null, 0, 0.0, tariffPublicationFee);
    return new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                            TariffStatus.Status.success);
  }

  /**
   * Handles changes in tariff expiration date.
   */
  @Override
  public TariffStatus processTariff (TariffExpire update)
  {
    ValidationResult result = validateUpdate(update);
    if (result.tariff == null)
      return result.message;
    else {
      // update expiration date
      result.tariff.setExpiration(update.getNewExpiration());
      log.info("Tariff " + update.getTariffId() + 
               "now expires at " + new DateTime(result.tariff.getExpiration(), DateTimeZone.UTC).toString());
    }
    return success(update);
  }

  /**
   * Handles tariff revocation.
   */
  @Override
  public TariffStatus processTariff (TariffRevoke update)
  {
    ValidationResult result = validateUpdate(update);
    if (result.tariff == null)
      return result.message;
    else {
      result.tariff.setState(Tariff.State.KILLED);
      log.info("Revoke tariff ${update.tariffId}");
      // The actual revocation processing is delegated to the Customer,
      // who is obligated to call getRevokedSubscriptions periodically.

      // If there are active subscriptions, then we have to charge a fee.
      List<TariffSubscription> activeSubscriptions =
          tariffSubscriptionRepo.findSubscriptionsForTariff(result.tariff);
      for (Iterator<TariffSubscription> subs = activeSubscriptions.iterator(); subs.hasNext(); ) {
        TariffSubscription sub = subs.next();
        if (sub.getCustomersCommitted() <= 0) {
          subs.remove();
        }
      }
      // check whether there are any remaining active subscriptions
      if (activeSubscriptions.size() > 0) {
        log.info("Revoked tariff has " + activeSubscriptions.size() + " active subscriptions");
        accountingService.addTariffTransaction(TariffTransactionType.REVOKE,
                                               result.tariff, null, 0, 0.0,
                                               tariffRevocationFee);
      }
    }
    return success(update);
  }

  /**
   * Applies a new HourlyCharge to an existing Tariff with a variable Rate.
   */
  @Override
  public TariffStatus processTariff (VariableRateUpdate update)
  {
    ValidationResult result = validateUpdate(update);
    if (result.tariff == null)
      return result.message;
    else if (result.tariff.addHourlyCharge(update.getHourlyCharge(), update.getRateId())) {
      return success(update);
    }
    else {
      // failed to add hourly charge
      return new TariffStatus(update.getBroker(), update.getTariffId(), update.getId(),
                              TariffStatus.Status.invalidUpdate)
          .setMessage("update: could not add hourly charge");
    }
  }

  // ----------------------- Customer API --------------------------

  private List<NewTariffListener> registrations = new ArrayList<NewTariffListener>();

  @Override
  public void registerNewTariffListener (NewTariffListener listener)
  {
    registrations.add(listener);
  }

  // Process queued messages, then
  // handle distribution of new tariffs to customers
  public void activate (Instant time, int phase)
  {
    processIncoming();
    if (publicationInterval > 24) {
      log.error("tariff publication interval " + publicationInterval + " > 24 hr");
      publicationInterval = 24;
    }
    long msec = timeService.getCurrentTime().getMillis();
    if (msec % (publicationInterval * TimeService.HOUR) == 0) {
      // time to publish
      publishTariffs();
    }
  }
  
  private void processIncoming()
  {
    while (incoming.size() > 0) {
      TariffMessage msg;
      synchronized (incomingLock) {
        msg = incoming.get(0);
        incoming.remove(0);
      }
      // dispatch incoming message
      TariffStatus result = processTariff(msg);
      // return non-null result as msg
      if (result != null) {
        brokerProxyService.sendMessage(result.getBroker(), result);
      }
    }
  }

  /**
   * Publishes pending tariffs to customers and brokers
   */
  private void publishTariffs ()
  {
    List<Tariff> publishedTariffs = tariffRepo.findTariffsByState(Tariff.State.PENDING);
    log.info("publishing " + publishedTariffs.size() + " new tariffs");
    for (Tariff tariff : publishedTariffs) {
      tariff.setState(Tariff.State.OFFERED);
    }

    List<TariffSpecification> publishedTariffSpecs = new ArrayList<TariffSpecification>();
    for (Tariff tariff : publishedTariffs) {
      TariffSpecification spec = tariff.getTariffSpecification();
      publishedTariffSpecs.add(spec);
      log.info("publishing spec " + spec.getId() + " broker: " + spec.getBroker().getUsername() + ", exp: " + spec.getExpiration());
    }

    for (NewTariffListener listener : registrations) {
      listener.publishNewTariffs(publishedTariffs);
    }
    brokerProxyService.broadcastMessages(publishedTariffSpecs);
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
  public TariffSubscription subscribeToTariff (Tariff tariff,
                                               AbstractCustomer customer,
                                               int customerCount)
  {
    if (tariff.isExpired())
      return null;
    TariffSubscription sub = tariffSubscriptionRepo.getSubscription(customer, tariff);
    sub.subscribe(customerCount);
    return sub;
  }

  public List<Tariff> getActiveTariffList(PowerType type)
  {
    return tariffRepo.findActiveTariffs(type);
  }

  /**
   * Returns the list of subscriptions for this customer that have been
   * revoked and have non-zero committed customers.
   */
  @Override
  public List<TariffSubscription> getRevokedSubscriptionList (AbstractCustomer customer)
  {
    List<TariffSubscription> result = new ArrayList<TariffSubscription>();
    for (TariffSubscription sub : tariffSubscriptionRepo.findSubscriptionsForCustomer(customer)) {
      if (sub.getTariff().getState() == Tariff.State.KILLED && sub.getCustomersCommitted() > 0) {
        result.add(sub);
      }
    }
    return result;
  }

  /**
   * Returns the default tariff
   */
  @Override
  public Tariff getDefaultTariff (PowerType type)
  {
    long defaultId = defaultTariff.get(type);
    return tariffRepo.findTariffById(defaultId);
  }

  @Override
  public boolean setDefaultTariff (TariffSpecification newSpec)
  {
    Tariff tariff = new Tariff(newSpec);
    tariff.init();
    defaultTariff.put(newSpec.getPowerType(), tariff.getId());
    return true;
  }

//  @Override
//  public List<TariffTransaction> getTransactions ()
//  {
//    return null;
//  }

  private TariffStatus success (TariffUpdate update)
  {
    Broker broker = update.getBroker();
    return new TariffStatus(broker, update.getTariffId(), update.getId(),
                            TariffStatus.Status.success);
  }

  private ValidationResult validateUpdate (TariffUpdate update)
  {
    Broker broker = update.getBroker();
    Tariff tariff = tariffRepo.findTariffById(update.getTariffId());
    if (tariff == null) {
      log.error("update - no such tariff ${update.tariffId}, broker ${update.brokerId}");
      return new ValidationResult(null,
                                  new TariffStatus(broker, update.getTariffId(), update.getId(),
                                                   TariffStatus.Status.noSuchTariff));
    }
    return new ValidationResult(tariff, null);
  }

  @Override
  public TariffStatus processTariff (TariffMessage message)
  {
    log.error("Call to generic processTariff - should not happen");
    return new TariffStatus(null, -1l, -1l, TariffStatus.Status.illegalOperation);
  }

  private class ValidationResult
  {
    Tariff tariff;
    TariffStatus message;
    
    ValidationResult (Tariff tariff, TariffStatus msg)
    {
      super();
      this.tariff = tariff;
      this.message = msg;
    }
  }
}
