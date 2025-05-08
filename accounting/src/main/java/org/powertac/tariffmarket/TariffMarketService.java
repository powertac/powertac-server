/*
 * Copyright 2011-2020 the original author or authors.
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import java.time.ZoneOffset;
import java.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Broker;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.RegulationRate;
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
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
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
  static private Logger log =
          LogManager.getLogger(TariffMarketService.class.getSimpleName());
  final Level BFAULT = Level.forName("BFAULT", 250);

  @Autowired
  private TimeService timeService;
  
  @Autowired
  private Accounting accountingService;
  
  @Autowired
  private CapacityControl capacityControlService;
  
  @Autowired
  private BrokerProxy brokerProxyService;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
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
  
  // list of tariffs that have been revoked but not processed
  private ArrayList<Tariff> pendingRevokedTariffs = new ArrayList<> ();
  // list of revoked but not yet deleted tariffs
  private List<Tariff> revokedTariffs = null;
  private Instant lastRevokeProcess = Instant.ofEpochMilli(0L);

  // configure tariff publication fees
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

  // configure tariff revocation fees
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
  private boolean subsequentPublication;

  // list of pending subscription events.
  private List<PendingSubscription> pendingSubscriptionEvents =
      new ArrayList<>();

  // list of pending variable-rate updates.
  private List<VariableRateUpdate> pendingVrus = new ArrayList<>();

  // set of already-disabled brokers
  private HashSet<Broker> disabledBrokers = new HashSet<>();

  private Set<NewTariffListener> registrations = new LinkedHashSet<>();

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
  @Override
  public String initialize (Competition competition, List<String> completedInits)
  {
    int index = completedInits.indexOf("AccountingService");
    if (index == -1) {
      return null;
    }

    for (Class<?> messageType: Arrays.asList(TariffSpecification.class,
                                             TariffExpire.class,
                                             TariffRevoke.class,
                                             VariableRateUpdate.class,
                                             EconomicControlEvent.class,
                                             BalancingOrder.class)) {
      brokerProxyService.registerBrokerMessageListener(this, messageType);
    }
    subsequentPublication = false;
    registrations.clear();
    publicationFee = null;
    revocationFee = null;
    
    super.init();
    
    pendingSubscriptionEvents.clear();
    pendingRevokedTariffs.clear();
    pendingVrus.clear();
    disabledBrokers.clear();
    revokedTariffs = null;
    lastRevokeProcess = Instant.ofEpochMilli(0L);

    serverProps.configureMe(this);

    // Register the NewTariffListeners
    List<NewTariffListener> listeners =
        SpringApplicationContext.listBeansOfType(NewTariffListener.class);
    for (NewTariffListener listener : listeners) {
      registerNewTariffListener(listener);
    }

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

  // Test support
  List<NewTariffListener> getRegistrations ()
  {
    return new ArrayList<>(registrations);
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
    if (!(null == tariffRepo.findSpecificationById(spec.getId()) ||
            tariffRepo.isRemoved(spec.getId()))) {
      log.log(BFAULT, "duplicate tariff spec from " + spec.getBroker().getUsername() +
               ", id = " + spec.getId());
      send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                            TariffStatus.Status.invalidTariff)
          .withMessage("duplicate tariff spec " + spec.getId()));
      return;
    }
    if (null == spec.getRates()) {
      log.log(BFAULT, "no rates given for spec " + spec.getId());
      send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                            TariffStatus.Status.invalidTariff)
          .withMessage("missing Rates"));
      return;
    }
    else if (!spec.isValid()) {
      log.log(BFAULT, "invalid spec " + spec.getId());
      send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                            TariffStatus.Status.invalidTariff)
          .withMessage("spec fails validity test"));
      return;
    }
    else if (null != spec.getSupersedes()) {
      for (Long supersede : spec.getSupersedes()) {
        TariffSpecification other = tariffRepo.findSpecificationById(supersede);
        if (null == other) {
          log.log(BFAULT, "attempt to supersede non-existent tariff " + supersede);
          send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                                TariffStatus.Status.invalidTariff)
              .withMessage("non-existent supersede " + supersede));
          return;
        }
        else if (spec.getBroker() != other.getBroker()) {
          log.log(BFAULT, "attempt by " + spec.getBroker().getUsername()
                   + " to supersede tariff of "
                   + other.getBroker().getUsername());
          send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                                TariffStatus.Status.invalidTariff)
              .withMessage("invalid supersede " + supersede));
          return;
        }
      }
    }
    //validate rates
    for (Rate rate : spec.getRates()) {
      if (rate.getDailyBegin() >= 24 || rate.getDailyEnd() >= 24 ||
              rate.getWeeklyBegin() == 0 || rate.getWeeklyBegin() > 7 ||
              rate.getWeeklyEnd() == 0 || rate.getWeeklyEnd() > 7) {
        log.log(BFAULT, "invalid rate for spec " + spec.getId());
        send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                              TariffStatus.Status.invalidTariff)
            .withMessage("spec has invalid Rate"));
        return;
      }
    }
    tariffRepo.addSpecification(spec);
    Tariff tariff = new Tariff(spec);
    if (!tariff.init()) {
      log.log(BFAULT, "incomplete coverage in multi-rate tariff " + spec.getId());
      tariffRepo.removeTariff(tariff);
      send(new TariffStatus(spec.getBroker(), spec.getId(), spec.getId(),
                            TariffStatus.Status.invalidTariff)
          .withMessage("incomplete coverage in multi-rate tariff"));
      return;
    }
    for (RegulationRate regRate : spec.getRegulationRates()) {
      // ignore response time setting -- see Issue #1041
      //if (regRate.getResponse() == RegulationRate.ResponseTime.SECONDS) {
      //  log.warn("discarding fast-response RegulationRate tariff {}",
      //           spec.getId());
      //  continue;
      //}
      if (spec.getPowerType().isInterruptible()
          || spec.getPowerType().isStorage()) {
        // Automatic composition of balancing orders -- see Issue #946
        if (regRate.getUpRegulationPayment() != 0.0) {
          BalancingOrder bo =
              new BalancingOrder(spec.getBroker(), spec,
                                 (spec.getPowerType().isStorage()? 2.0: 1.0),
                                 regRate.getUpRegulationPayment());
          tariffRepo.addBalancingOrder(bo);
        }
        if (regRate.getDownRegulationPayment() != 0.0) {
          BalancingOrder bo =
              new BalancingOrder(spec.getBroker(), spec, -1.0,
                                 regRate.getDownRegulationPayment());
          tariffRepo.addBalancingOrder(bo);
        }
      }
    }
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
        log.log(BFAULT, "attempt to set expiration for tariff " +
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
                 "now expires at " + result.tariff.getExpiration().atZone(ZoneOffset.UTC).toString());
        success(update);
      }
    }
  }

  /**
   * Handles tariff revocation.
   */
  public void handleMessage (TariffRevoke update)
  {
    // basic validation
    ValidationResult result = validateUpdate(update);
    if (result.tariff == null) {
      send(result.message);
      return;
    }
    addPendingRevoke(result.tariff);
    success(update);
  }

  /**
   * Applies a new HourlyCharge to an existing Tariff with a variable Rate.
   */
  public void handleMessage (VariableRateUpdate update)
  {
    ValidationResult result = validateUpdate(update);
    if (result.tariff == null) {
      send(result.message);
      return;
    }
    Rate rate = tariffRepo.findRateById(update.getRateId());
    if (rate == null) {
      send(new TariffStatus(update.getBroker(),
                            update.getTariffId(),
                            update.getId(),
                            TariffStatus.Status.invalidUpdate)
           .withMessage("Non-existent rate in VRU"));
      return;
    }
    if (!result.tariff.getTariffSpecification().getRates().contains(rate)) {
      send(new TariffStatus(update.getBroker(),
                            update.getTariffId(),
                            update.getId(),
                            TariffStatus.Status.invalidUpdate)
           .withMessage("Rate not associated with tariff in VRU"));
      return;
    }
    if (!update.isValid(rate)) {
      send(new TariffStatus(update.getBroker(),
                            update.getTariffId(),
                            update.getId(),
                            TariffStatus.Status.invalidUpdate)
           .withMessage("Invalid charge in VRU"));
      return;
    }
    addVru(update);
  }
  
  /**
   * Processes an incoming ControlEvent from a broker
   */
  public synchronized void handleMessage (EconomicControlEvent msg)
  {
    ValidationResult result = validateUpdate(msg);
    if (result.tariff == null) {
      send(result.message);
      return;
    }
    int currentTimeslot = timeslotRepo.currentTimeslot().getSerialNumber();
    if (currentTimeslot > msg.getTimeslotIndex()) {
      // this is in the past
      log.log(BFAULT, "Curtailment requested in ts " + currentTimeslot +
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
   * Processes an incoming BalancingOrder by storing it in the tariffRepo.
   * Balancing orders can be used for interruptible tariffs, but not in the
   * presence of RegulationRates. For tariffs with RegulationRates, the
   * BalancingOrders are automatically constructed from the RRs. In other words,
   * BalancingOrders can be used to permit up-regulation through curtailment, but 
   * not if the customer expects to be paid for the balancing events.
   */
  public synchronized void handleMessage (BalancingOrder msg)
  {
    ValidationResult result = validateUpdate(msg);
    if (result.tariff == null) {
      send(result.message);
      return;
    }
    else {
      // not allowed if tariff has RegulationRates
      if (result.tariff.hasRegulationRate()) {
        result.message.withMessage("Cannot use BO with RegulationRate")
        .setStatus(TariffStatus.Status.unsupported);
        send(result.message);
        return;
      }
      // not allowed if no rates allow curtailment
      boolean curtailmentAllowed = false;
      for (Rate rate: result.tariff.getTariffSpecification().getRates()) {
        if (rate.getMaxCurtailment() > 0.0) {
          curtailmentAllowed = true;
          break;
        }
      }
      if (!curtailmentAllowed) {
        result.message.withMessage("Cannot use BO without curtailment")
        .setStatus(TariffStatus.Status.unsupported);
        send(result.message);
        return;
      }
      // range check for exercise ratio
      if (msg.getExerciseRatio() <= 0.0 || msg.getExerciseRatio() > 1.0) {
        result.message.withMessage("Exercise ratio "
            + msg.getExerciseRatio() + " out of range")
            .setStatus(TariffStatus.Status.unsupported);
        send(result.message);
        return;
      }
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
      List<Tariff> result = new ArrayList<>(pendingRevokedTariffs);
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
    // nothing happens -- this is deprecated.
  }

  private void revokeTariffsForDisabledBrokers ()
  {
    for (Broker broker : brokerRepo.findDisabledBrokers()) {
      if (!disabledBrokers.contains(broker)) {
        // this is a new one
        disabledBrokers.add(broker);
        for (Tariff tariff : tariffRepo.findTariffsByBroker(broker)) {
          if (Tariff.State.KILLED != tariff.getState()) {
            log.info("Revoking tariff " + tariff.getId()
                     + " from disabled broker " + broker.getUsername());
            addPendingRevoke(tariff);
          }
        }
      }
    }
  }

  private void updateRevokedTariffs ()
  {
    List<Tariff> pending = getPendingRevokes();
    if (pending == null)
      return;
    
    revokedTariffs = pending;
    for (Tariff tariff : pending) {
      tariff.setState(Tariff.State.KILLED);
      log.info("Revoke tariff " + tariff.getId());
      // Notify all brokers - issue #719
      TariffRevoke msg = new TariffRevoke(tariff.getBroker(),
                                          tariff.getTariffSpecification());
      brokerProxyService.broadcastMessage(msg);

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
    processPendingVrus();
    long msec = timeService.getCurrentTime().toEpochMilli();
    if (!subsequentPublication ||
        (msec / TimeService.HOUR) % publicationInterval == publicationOffset) {
      // time to publish or never published
      revokeTariffsForDisabledBrokers();
      updateRevokedTariffs();
      publishTariffs();
      //removeRevokedTariffs();
      processPendingSubscriptions();
      subsequentPublication = true;
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

    List<TariffSpecification> publishedTariffSpecs = new ArrayList<>();
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
    return tariffRepo.getDefaultTariff(type);
  }

  @Override
  public boolean setDefaultTariff (TariffSpecification newSpec)
  {
    tariffRepo.setDefaultTariff(newSpec);
    return true;
  }
  
  // ----------- Subscribe/unsubscribe processing --------------

  /**
   * If customerCount is positive, subscribes a block of Customers
   * from a single Customer model to the specified Tariff, as long
   * as the Tariff is not expired or revoked. If customerCount is negative,
   * unsubscribes a block of customers from the specified tariff.
   * Processing is deferred unless the customer has no subscriptions,
   * which should only be true at the start of a boot or sim session.
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
    if (customerCount < 0 || !(tariff.isExpired() || tariff.isRevoked())) {
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
      else if (pending.count < 0)
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
    ArrayList<VariableRateUpdate> result = new ArrayList<>(pendingVrus);
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
      log.log(BFAULT, "update - no such tariff " + update.getTariffId() +
                ", broker " + update.getBroker().getUsername());
      return new ValidationResult(null,
                                  new TariffStatus(broker, update.getTariffId(), update.getId(),
                                                   TariffStatus.Status.noSuchTariff));
    }
    if (broker != tariff.getBroker()) {
      log.log(BFAULT, "update - attempt by " + broker.getUsername()
                + " to revoke " + tariff.getBroker() + "'s tariff");
      return new ValidationResult(null,
                                  new TariffStatus(broker, update.getTariffId(), update.getId(),
                                                   TariffStatus.Status.invalidTariff));
    }
    return new ValidationResult(tariff,
                                new TariffStatus(broker, update.getTariffId(), update.getId(),
                                                 TariffStatus.Status.success));
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
}
