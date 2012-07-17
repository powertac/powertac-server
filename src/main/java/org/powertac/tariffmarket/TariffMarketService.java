/*
 * Copyright 2011-2012 the original author or authors.
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

import static org.powertac.util.ListTools.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Broker;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffMessage;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TimeService;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.msg.EconomicControlEvent;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.TariffUpdate;
import org.powertac.common.msg.VariableRateUpdate;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.util.Predicate;
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
  extends TimeslotPhaseProcessor 
  implements TariffMarket, InitializationService
{
  static private Logger log = Logger.getLogger(TariffMarketService.class.getName());

  @Autowired
  private TimeService timeService;
  
  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private CapacityControl capacityControlService;
  
  @Autowired
  private BrokerProxy brokerProxyService;
  
  @Autowired
  private TimeslotRepo timeslotRepo;
  
  @Autowired
  private TariffRepo tariffRepo;
  
  @Autowired
  private TariffSubscriptionRepo tariffSubscriptionRepo;
  
  @Autowired
  private ServerConfiguration serverProps;

  @Autowired
  private RandomSeedRepo randomSeedService;

  // maps power type to id of corresponding default tariff
  private HashMap<PowerType, Long> defaultTariff;
  
  // list of tariffs that have been revoked but not processed
  private ArrayList<Tariff> pendingRevokedTariffs =
      new ArrayList<Tariff> ();
  // list of revoked but not yet deleted tariffs
  private List<Tariff> revokedTariffs = null;
  private Instant lastRevokeProcess = new Instant(0l);

  // configuration
  @ConfigurableValue(valueType = "Double",
      description = "low end of tariff publication fee range")
  private double minPublicationFee = -100.0;

  @ConfigurableValue(valueType = "Double",
      description = "high end of tariff publication fee range")
  private double maxPublicationFee = -500.0;

  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "set publication fee directly to override random selection")
  private Double publicationFee = null;
  
  @ConfigurableValue(valueType = "Double",
      description = "low end of tariff revocation fee range")
  private double minRevocationFee = -100.0;
  
  @ConfigurableValue(valueType = "Double",
      description = "high end of tariff revocation fee range")
  private double maxRevocationFee = -500.0;
  
  @ConfigurableValue(valueType = "Double",
      publish = true,
      description = "Set revocation fee directly to override random selection")
  private Double revocationFee = null;

  // these properties are constrained, so we provide explicit setters for them
  private int publicationInterval = 6;
  private int publicationOffset = 0;
  private boolean firstPublication;

  // list of pending subscription events.
  private List<PendingSubscription> pendingSubscriptionEvents =
          new ArrayList<PendingSubscription>();
  
  // list of pending variable-rate updates.
  private List<VariableRateUpdate> pendingVrus = new ArrayList<VariableRateUpdate>();
  
  /**
   * Default constructor
   */
  public TariffMarketService ()
  {
    super();
  }

  /**
   * Reads configuration parameters, registers for timeslot phase activation.
   */
  @SuppressWarnings("unchecked")
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    int index = completedInits.indexOf("AccountingService");
    if (index == -1) {
      return null;
    }

    defaultTariff = new HashMap<PowerType, Long>();
    //brokerProxyService.registerBrokerMessageListener(this);
    for (Class<?> messageType: Arrays.asList(TariffSpecification.class,
                                             TariffExpire.class,
                                             TariffRevoke.class,
                                             VariableRateUpdate.class,
                                             EconomicControlEvent.class,
                                             BalancingOrder.class)) {
      brokerProxyService.registerBrokerMessageListener(this, messageType);
    }
    firstPublication = false;
    registrations.clear();
    publicationFee = null;
    revocationFee = null;
    
    super.init();
    
    pendingSubscriptionEvents.clear();
    pendingRevokedTariffs.clear();
    pendingVrus.clear();
    revokedTariffs = null;
    lastRevokeProcess = new Instant(0);

    serverProps.configureMe(this);

    // compute the fees
    RandomSeed random =
        randomSeedService.getRandomSeed("TariffMarket",
                                        0l, "fees");
    if (publicationFee == null) {
      // interest will be non-null in case it was overridden in the config
      publicationFee = (minPublicationFee +
                             (random.nextDouble() *
                                 (maxPublicationFee - minPublicationFee)));
      log.info("set publication fee: " + publicationFee);
    }
    if (revocationFee == null) {
      // interest will be non-null in case it was overridden in the config
      revocationFee = (minRevocationFee +
                             (random.nextDouble() *
                                 (maxRevocationFee - minRevocationFee)));
      log.info("set revocation fee: " + revocationFee);
    }
    serverProps.publishConfiguration(this);
    return "TariffMarket";
  }

  // ------------ Data access and configuration support ---------------

  // publication fee
  public double getMinPublicationFee ()
  {
    return minPublicationFee;
  }

  public double getMaxPublicationFee ()
  {
    return maxPublicationFee;
  }

  public Double getPublicationFee ()
  {
    return publicationFee;
  }

  // revocation fee
  public double getMinRevocationFee ()
  {
    return minRevocationFee;
  }

  public double getMaxRevocationFee ()
  {
    return maxRevocationFee;
  }

  public Double getRevocationFee ()
  {
    return revocationFee;
  }

  public int getPublicationInterval ()
  {
    return publicationInterval;
  }
  
  @ConfigurableValue(valueType = "Integer",
      description = "Number of timeslots between tariff publication events. " +
                    "Must be at most 24.")
  public void setPublicationInterval (int interval)
  {
    if (interval > 24) {
      log.error("tariff publication interval " + interval + " > 24 hr");
      interval = 24;
    }
    publicationInterval = interval;
  }
  
  public int getPublicationOffset ()
  {
    return publicationOffset;
  }
  
  @ConfigurableValue(valueType = "Integer",
      description = "Number of timeslots from the first timeslot to delay " +
          "the first publication event. It does not work well " +
          "to make this zero, because brokers do not have an opportunity " +
          "to post tariffs in timeslot 0.")
  public void setPublicationOffset (int offset)
  {
    if (offset >= publicationInterval) {
      log.error("tariff publication offset " + publicationOffset
                + " >= publication interval " + offset);
    }
    else {
      publicationOffset = offset;
    }
  }

  List<NewTariffListener> getRegistrations ()
  {
    return registrations;
  }

  // ----------------- Broker message API --------------------
  // Receive incoming broker messages. We do this
  // synchronously with the incoming message traffic, rather than on
  // the timeslot phase signal, to minimize latency for broker feedback.

  /**
   * Processes a newly-published tariff.
   */
  public void handleMessage (TariffSpecification spec)
  {
    if (null != tariffRepo.findSpecificationById(spec.getId()) &&
            !tariffRepo.isDeleted(spec.getId())) {
      log.warn("duplicate tariff spec from " + spec.getBroker().getUsername() +
               ", id = " + spec.getId());
      send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                            TariffStatus.Status.invalidTariff));
      return;
    }
    if (null == spec.getRates()) {
      log.warn("no rates given for spec " + spec.getId());
      send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                            TariffStatus.Status.invalidTariff));
      return;
    }
    else {
      for (Rate rate : spec.getRates()) {
        if (rate.getDailyBegin() >= 24 || rate.getDailyEnd() >= 24 ||
                rate.getWeeklyBegin() == 0 || rate.getWeeklyBegin() > 7 ||
                rate.getWeeklyEnd() == 0 || rate.getWeeklyEnd() > 7) {
          log.warn("invalid rate for spec " + spec.getId());
          send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                                TariffStatus.Status.invalidTariff));
          return;
        }
      }
    }
    tariffRepo.addSpecification(spec);
    Tariff tariff = new Tariff(spec);
    tariffRepo.addTariff(tariff);
    tariff.init();
    log.info("new tariff " + spec.getId());
    accountingService.addTariffTransaction(TariffTransaction.Type.PUBLISH,
                                           tariff, null, 0, 0.0, publicationFee);
    send(new TariffStatus(spec.getBroker(),
                          spec.getId(),
                          spec.getId(),
                          TariffStatus.Status.success));
  }

  /**
   * Handles changes in tariff expiration date.
   */
  public void handleMessage (TariffExpire update)
  {
    ValidationResult result = validateUpdate(update);
    if (result.tariff == null)
      send(result.message);
    else {
      Instant newExp = update.getNewExpiration();
      if (newExp != null && newExp.isBefore(timeService.getCurrentTime())) {
        // new expiration date in the past
        log.warn("attempt to set expiration for tariff " +
                 result.tariff.getId() + " in the past:" +
                 newExp.toString());
        send(new TariffStatus(update.getBroker(),
                              update.getTariffId(),
                              update.getId(),
                              TariffStatus.Status.invalidUpdate)
             .withMessage("attempt to set expiration in the past"));
      }
      else {
        // update expiration date
        result.tariff.setExpiration(newExp);
        log.info("Tariff " + update.getTariffId() + 
                 "now expires at " + new DateTime(result.tariff.getExpiration(), DateTimeZone.UTC).toString());
        success(update);
      }
    }
  }

  /**
   * Handles tariff revocation.
   */
  public void handleMessage (TariffRevoke update)
  {
    ValidationResult result = validateUpdate(update);
    if (result.tariff == null)
      send(result.message);
    else {
      addPendingRevoke(result.tariff);
    }
    success(update);
  }

  /**
   * Applies a new HourlyCharge to an existing Tariff with a variable Rate.
   */
  public void handleMessage (VariableRateUpdate update)
  {
    ValidationResult result = validateUpdate(update);
    if (result.tariff == null)
      send(result.message);
    else 
      addVru(update);
  }
  
  /**
   * Processes an incoming ControlEvent from a broker
   */
  public void handleMessage (EconomicControlEvent msg)
  {
    ValidationResult result = validateUpdate(msg);
    if (result.tariff == null) {
      send(result.message);
      return;
    }
    int currentTimeslot = timeslotRepo.currentTimeslot().getSerialNumber();
    if (currentTimeslot > msg.getTimeslotIndex()) {
      // this is in the past
      log.warn("Curtailment requested in ts " + currentTimeslot +
               " for past timeslot " + msg.getTimeslotIndex());
      // send error?
      send(new TariffStatus(msg.getBroker(),
                            msg.getTariffId(),
                            msg.getId(),
                            TariffStatus.Status.invalidUpdate)
          .withMessage("control: specified timeslot in the past"));
      return;
    }
    capacityControlService.postEconomicControl(msg);
  }
  
  /**
   * Processes an incoming BalancingOrder by storing it in the tariffRepo
   */
  public void handleMessage (BalancingOrder msg)
  {
    ValidationResult result = validateUpdate(msg);
    if (result.tariff == null) {
      send(result.message);
      return;
    }
    tariffRepo.addBalancingOrder(msg);
  }

  // ----------------------- Customer API --------------------------

  private synchronized void addPendingRevoke (Tariff tariff)
  {
    pendingRevokedTariffs.add(tariff);
  }
  
  private synchronized List<Tariff> getPendingRevokes ()
  {
    Instant now = timeService.getCurrentTime();
    if (now.isAfter(lastRevokeProcess)) {
      lastRevokeProcess = now;
      List<Tariff> result = new ArrayList<Tariff>(pendingRevokedTariffs);
      pendingRevokedTariffs.clear();
      return result;
    }
    return null; // only get non-null result once/timeslot
  }
  
  /**
   * Runs through the list of pending tariff revocations, marking the tariffs
   * and their subscriptions.
   */
  @Override
  public void processRevokedTariffs ()
  {
    List<Tariff> pending = getPendingRevokes();
    if (pending == null)
      return;
    
    revokedTariffs = pending;
    for (Tariff tariff : pending) {
      tariff.setState(Tariff.State.KILLED);
      log.info("Revoke tariff " + tariff.getId());
      // The actual revocation processing is delegated to the Customer,
      // who is obligated to call getRevokedSubscriptions periodically.

      // If there are active subscriptions, then we have to charge a fee.
      List<TariffSubscription> activeSubscriptions =
          filter(tariffSubscriptionRepo.findSubscriptionsForTariff(tariff),
                 new Predicate<TariffSubscription> () {
            @Override
            public boolean apply (TariffSubscription sub) {
              return (sub.getCustomersCommitted() > 0);
            }
          });
      if (activeSubscriptions.size() > 0) {
        log.info("Revoked tariff " + tariff.getId() +
                 " has " + activeSubscriptions.size() + 
                 " active subscriptions");
        accountingService.addTariffTransaction(TariffTransaction.Type.REVOKE,
                                               tariff, null, 0, 0.0,
                                               revocationFee);
      }
    }
  }
  
  // Removes revoked tariffs and their subscriptions from their respective
  // repos.
  void removeRevokedTariffs ()
  {
    if (null == revokedTariffs)
      return;

    for (Tariff tariff : revokedTariffs) {
      // remove all subscriptions
      tariffSubscriptionRepo.removeSubscriptionsForTariff(tariff);
      
      // then remove the tariff and the tariffSpec
      tariffRepo.removeTariff(tariff);
    }
    revokedTariffs = null;
  }

  private List<NewTariffListener> registrations = new ArrayList<NewTariffListener>();

  @Override
  public void registerNewTariffListener (NewTariffListener listener)
  {
    registrations.add(listener);
  }

  // Process queued messages, then
  // handle distribution of new tariffs to customers
  @Override
  public void activate (Instant time, int phase)
  {
    log.info("Activate");
    processPendingSubscriptions();
    removeRevokedTariffs();
    processPendingVrus();
    long msec = timeService.getCurrentTime().getMillis();
    if (!firstPublication ||
        (msec / TimeService.HOUR) % publicationInterval == publicationOffset) {
      // time to publish or never published
      publishTariffs();
      firstPublication = true;
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

  @Override
  public List<Tariff> getActiveTariffList(PowerType type)
  {
    return tariffRepo.findActiveTariffs(type);
  }

  /**
   * Returns the default tariff
   */
  @Override
  public Tariff getDefaultTariff (PowerType type)
  {
    Long defaultId = defaultTariff.get(type);
    if (defaultId == null)
      return null;
    return tariffRepo.findTariffById(defaultId);
  }

  @Override
  public boolean setDefaultTariff (TariffSpecification newSpec)
  {
    tariffRepo.addSpecification(newSpec);
    Tariff tariff = new Tariff(newSpec);
    tariff.init();
    tariffRepo.addTariff(tariff);
    defaultTariff.put(newSpec.getPowerType(), tariff.getId());
    return true;
  }
  
  // ----------- Subscribe/unsubscribe processing --------------

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
  public void subscribeToTariff (Tariff tariff,
                                 CustomerInfo customer,
                                 int customerCount)
  {
    if (!(tariff.isExpired() || tariff.isRevoked())) {
      postPendingSubscriptionEvent(tariff, customer, customerCount);
      List<TariffSubscription> existingSubscriptions =
              tariffSubscriptionRepo.findSubscriptionsForCustomer(customer);
      if (0 == existingSubscriptions.size()) {
        // immediate processing of initial subscriptions
        processPendingSubscriptions();
      }
    }
    else
      log.warn("Attempt to subscribe to " +
               (tariff.isRevoked() ? "revoked" : "expired") +
               " tariff");
  }
  
  /**
   * Adds a pending subscribe/unsubscribe for later processing
   */
  private synchronized void postPendingSubscriptionEvent (Tariff tariff,
                                                          CustomerInfo customer,
                                                          int customerCount)
  {
    
    PendingSubscription event =
            new PendingSubscription(tariff, customer, customerCount);
    pendingSubscriptionEvents.add(event);
  }
  
  /**
   * Handles pending subscription/unsubscription events
   */
  private synchronized void processPendingSubscriptions()
  {
    for (PendingSubscription pending : pendingSubscriptionEvents) {
      TariffSubscription sub =
              tariffSubscriptionRepo.getSubscription(pending.customer,
                                                     pending.tariff);
      if (pending.count > 0)
        sub.subscribe(pending.count);
      else
        sub.deferredUnsubscribe(-pending.count);
    }
    pendingSubscriptionEvents.clear();
  }
  
  /**
   * Handles pending vru messages
   */
  private void processPendingVrus ()
  {
    for (VariableRateUpdate vru: getVruList()) {
      Tariff tariff = tariffRepo.findTariffById(vru.getTariffId());
      if (tariff.addHourlyCharge(vru.getHourlyCharge(), vru.getRateId())) {
        success(vru);
      }
      else {
        // failed to add hourly charge
        send(new TariffStatus(vru.getBroker(),
                              vru.getTariffId(),
                              vru.getId(),
                              TariffStatus.Status.invalidUpdate)
          .withMessage("update: could not add hourly charge"));
      }
    }
  }
  
  // adds a VariableRateUpdate to the shared list
  private synchronized void addVru (VariableRateUpdate newVru)
  {
    pendingVrus.add(newVru);
  }
  
  // transfers the contents of the pending VRU list to the caller
  private synchronized List<VariableRateUpdate> getVruList()
  {
    ArrayList<VariableRateUpdate> result = new ArrayList<VariableRateUpdate>(pendingVrus);
    pendingVrus.clear();
    return result;
  }
  
  // ------------------ Helper stuff ---------------

  private void success (TariffUpdate update)
  {
    Broker broker = update.getBroker();
    send(new TariffStatus(broker,
                          update.getTariffId(),
                          update.getId(),
                          TariffStatus.Status.success));
  }
  
  // sends a message to the broker
  private void send (TariffMessage msg)
  {
    if (null == msg) {
      log.debug("null outgoing message");
    }
    else {
      brokerProxyService.sendMessage(msg.getBroker(), msg);
    }
  }

  private ValidationResult validateUpdate (TariffUpdate update)
  {
    Broker broker = update.getBroker();
    Tariff tariff = tariffRepo.findTariffById(update.getTariffId());
    if (tariff == null) {
      log.error("update - no such tariff " + update.getTariffId() +
                ", broker " + update.getBroker().getUsername());
      return new ValidationResult(null,
                                  new TariffStatus(broker, update.getTariffId(), update.getId(),
                                                   TariffStatus.Status.noSuchTariff));
    }
    return new ValidationResult(tariff, null);
  }

  // data structure for message validation result
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
  
  // data structure for pending subscription events
  private class PendingSubscription
  {
    Tariff tariff;
    CustomerInfo customer;
    int count;
    
    PendingSubscription (Tariff tariff, CustomerInfo customer, int count)
    {
      super();
      this.tariff = tariff;
      this.customer = customer;
      this.count = count;
    }
  }

  @Override
  public void setDefaults ()
  {
  }
}
