/*
 * Copyright (c) 2011-2013 by the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.time.Instant;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents a Tariff offered by a Broker to customers. A TariffSpecification
 * may specify upfront and periodic payments, and
 * aggregates a set of Rate instances that specify
 * prices for energy in various circumstances. This class is a value type -- a 
 * serializable, immutable data structure. You need to initialize a 
 * Tariff from it to make serious use of it. New tariffs and their Rates
 * are communicated to Customers and to Brokers when tariffs are published.
 * <p>
 * TariffSpecifications and their associated Rates and HourlyCharges are
 * specified from the Customer's viewpoint in terms of quantities and charges.
 * In other words, a positive charge means the broker pays the customer, while
 * a positive energy quantity means the broker sends energy to the customer.</p>
 * <p>
 * <strong>Note:</strong> Must be serialized "deep" to gather up the Rates and
 * associated HourlyCharge instances.</p>
 * <p>
 * State log entry format for new instance:<br/>
 * <code>long brokerId, PowerType powerType</code><br/><br/>
 * State log fields for readResolve():<br>
 * <code>long brokerId, PowerType powerType, long minDuration,<br>
 * &nbsp;&nbsp;double signupPayment, double earlyWithdrawPayment,<br>
 * &nbsp;&nbsp;double periodicPayment, List<tariffId> supersedes</code>
 * 
 * @author John Collins
 */
@Domain(fields = {"broker", "powerType", "expiration", "minDuration",
                  "signupPayment", "earlyWithdrawPayment", "periodicPayment",
                  "supersedes"})
@XStreamAlias("tariff-spec")
public class TariffSpecification extends TariffMessage
{
  static private Logger log = LogManager.getLogger(TariffSpecification.class);

  /** Last date, in msec past epoch, that new subscriptions will be accepted.
   *  Zero means never expire. */
  @XStreamAsAttribute
  private long expiration = 0l;

  /** Minimum contract duration (in milliseconds) */
  @XStreamAsAttribute
  private long minDuration = 0l;

  /** Type of power covered by this tariff */
  @XStreamAsAttribute
  private PowerType powerType = PowerType.CONSUMPTION;

  /** One-time payment for subscribing to tariff, positive for payment
   *  to customer, negative for payment from customer. */
  @XStreamAsAttribute
  private double signupPayment = 0.0;

  /** Payment from customer to broker for canceling subscription before
   *  minDuration has elapsed. */
  @XStreamAsAttribute
  private double earlyWithdrawPayment = 0.0;

  /** Flat payment per period for two-part tariffs */
  @XStreamAsAttribute
  private double periodicPayment = 0.0;

  private List<RateCore> rates;

  private List<Long> supersedes;
  
  /**
   * Creates a new TariffSpecification for a broker and a specific powerType.
   * Attributes are provided by the fluent {@code withX()} methods.
   */
  public TariffSpecification (Broker broker, PowerType powerType)
  {
    super(broker);
    this.broker = broker;
    this.powerType = powerType;
    this.rates = new ArrayList<RateCore>();
  }

  public PowerType getPowerType ()
  {
    return powerType;
  }
  
  public Instant getExpiration ()
  {
    if (expiration > 0)
      return Instant.ofEpochMilli(expiration);
    else
      return null;
  }

  /**
   * Sets expiration date as msec since epoch. See Issue #1016.
   */
  @StateChange
  public TariffSpecification withExpiration (long expiration)
  {
    this.expiration = expiration;
    return this;
  }

  /**
   * Sets the expiration date for this tariff. After this date, customers 
   * will no longer be allowed to subscribe.
   */
  public TariffSpecification withExpiration (Instant expiration)
  {
    return this.withExpiration(expiration.toEpochMilli());
  }

  public long getMinDuration ()
  {
    return minDuration;
  }

  /**
   * Sets the minimum duration of a subscription for this tariff. If a customer
   * wishes to withdraw earlier than the minimum duration, it may be required
   * to pay a fee as specified by the withdrawPayment.
   */
  @StateChange
  public TariffSpecification withMinDuration (long minDuration)
  {
    this.minDuration = minDuration;
    return this;
  }

  public double getSignupPayment ()
  {
    return signupPayment;
  }

  /**
   * Sets the signup payment for new subscriptions. This is a positive
   * number if the broker pays the customer.
   */
  @StateChange
  public TariffSpecification withSignupPayment (double signupPayment)
  {
    this.signupPayment = signupPayment;
    return this;
  }

  public double getEarlyWithdrawPayment ()
  {
    return earlyWithdrawPayment;
  }

  /**
   * Sets the payment for a customer who withdraws from a subscription to
   * this tariff before the minimumDuration has expired. A negative number
   * indicates that the customer pays the broker.
   */
  @StateChange
  public TariffSpecification withEarlyWithdrawPayment (double earlyWithdrawPayment)
  {
    this.earlyWithdrawPayment = earlyWithdrawPayment;
    return this;
  }

  public double getPeriodicPayment ()
  {
    return periodicPayment;
  }

  /**
   * Sets the daily payment per customer for subscriptions to this tariff.
   * A negative number indicates that the customer pays the broker.
   */
  @StateChange
  public TariffSpecification withPeriodicPayment (double periodicPayment)
  {
    this.periodicPayment = periodicPayment;
    return this;
  }

//  @Override
//  public long getId ()
//  {
//    return id;
//  }

  @Override
  public Broker getBroker ()
  {
    return broker;
  }

  /**
   * Returns the Rate instances from among the rates in this tariff spec.
   */
  public List<Rate> getRates ()
  {
    List<Rate> result = new ArrayList<Rate>();
    if (null == rates) {
      log.error("Null rates tariff {}", this.getId());
      return result;
    }
    for (RateCore rate : rates) {
      if (rate instanceof Rate) {
        result.add((Rate)rate);
      }
    }
    return result;
  }

  /**
   * Returns the RegulationRate instances from among the rates
   * in this tariff spec.
   */
  public List<RegulationRate> getRegulationRates ()
  {
    List<RegulationRate> result = new ArrayList<RegulationRate>();
    for (RateCore rate : rates) {
      if (rate instanceof RegulationRate) {
        result.add((RegulationRate)rate);
      }
    }
    return result;
  }

  /**
   * Returns true just in case this tariff has one or more RegulationRates.
   */
  public boolean hasRegulationRate ()
  {
    for (RateCore rate : rates) {
      if (rate instanceof RegulationRate) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a new RateCore (Rate, RegulationRate, etc.) to this tariff.
   */
  @StateChange
  public TariffSpecification addRate (RateCore rate)
  {
    if (null == rates)
      // readResolve does not create the list
      rates = new ArrayList<RateCore>();
    rates.add(rate);
    rate.setTariffId(id);
    return this;
  }

  public List<Long> getSupersedes ()
  {
    return supersedes;
  }

  /**
   * Indicates that this tariff supersedes the tariff specified by the
   * specId, the id of the superseded tariff. Setting this value has no
   * effect unless the superseded tariff is revoked by sending a TariffRevoke
   * message for that tariff.
   */
  @StateChange
  public TariffSpecification addSupersedes (long specId)
  {
    if (supersedes == null)
      supersedes = new ArrayList<Long>();
    supersedes.add(specId);
    return this;
  }

  /**
   * Modify read-resolve args when reading state log -- see Issue #1056
   */
  public static void modifyLogArgs (String[] args)
  {
    // if expiration is null, make it zero
    if ("null".equals(args[2]))
      args[2] = "0";
    else // otherwise if it parses as an Instant, change to long
    {
      Instant exp = Instant.parse(args[2]);
      args[2] = Long.toString(exp.toEpochMilli());
    }

  }
  
  /**
   * A TariffSpecification is valid if
   * <ul>
   *   <li>it has a least one Rate, and all its Rates are valid;</li>
   *   <li>minDuration is non-negative;</li>
   * </ul>
   */
  @Override
  public boolean isValid ()
  {
    if (getRates().size() == 0) {
      log.warn("invalid: no rates");
      return false;
    }
    for (Rate rate : getRates()) {
      if (!rate.isValid(this)) {
        log.warn("invalid rate");
        return false;
      }
    }
    if (minDuration < 0l) {
      log.warn("invalid: negative minDuration");
      return false;
    }
    return true;
  }
  
  /**
   * Returns a String giving the id, broker username, and powertype
   */
  @Override
  public String toString ()
  {
    return ("TariffSpecification " + getId() + " "
            + getBroker().getUsername() + "." + getPowerType());
  }
  
  // protected default constructor to simplify deserialization
  protected TariffSpecification ()
  {
    super();
  }
}
