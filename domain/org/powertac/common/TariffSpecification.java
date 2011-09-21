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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.TariffMessageProcessor;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.xml.BrokerConverter;

import com.thoughtworks.xstream.annotations.*;

/**
 * Represents a Tariff offered by a Broker to customers. A Tariff specifies
 * prices for energy in various circumstances, along with upfront and periodic
 * payments and basic constraints. This class is a value type -- a 
 * serializable, immutable data structure. You need to initialize a 
 * Tariff from it to make serious use of it. New tariffs and their Rates
 * are communicated to Customers and to Brokers when tariffs are published.
 * <p>
 * <strong>Note:</strong> Must be serialized "deep" to gather up the Rates and
 * associated HourlyCharge instances.</p>
 * @author John Collins
 */
@Domain
@XStreamAlias("tariff-spec")
public class TariffSpecification extends TariffMessage
{
  static private Logger log = Logger.getLogger(Rate.class.getName());

  /** Last date new subscriptions will be accepted. Null means never expire. */
  private Instant expiration = null;

  /** Minimum contract duration (in milliseconds) */
  @XStreamAsAttribute
  private long minDuration = 0l;

  /** Type of power covered by this tariff */
  @XStreamAsAttribute
  private PowerType powerType = PowerType.CONSUMPTION;

  /** One-time payment for subscribing to tariff, positive for payment
   *  from customer, negative for payment to customer. */
  @XStreamAsAttribute
  private double signupPayment = 0.0;

  /** Payment from customer to broker for canceling subscription before
   *  minDuration has elapsed. */
  @XStreamAsAttribute
  private double earlyWithdrawPayment = 0.0;

  /** Flat payment per period for two-part tariffs */
  @XStreamAsAttribute
  private double periodicPayment = 0.0;

  private List<Rate> rates;

  private List<Long> supersedes;
  
  public TariffSpecification (Broker broker, PowerType powerType)
  {
    super(broker);
    this.broker = broker;
    this.powerType = powerType;
    this.rates = new ArrayList<Rate>();
  }

  public PowerType getPowerType ()
  {
    return powerType;
  }
  
  public Instant getExpiration ()
  {
    return expiration;
  }

  @StateChange
  public TariffSpecification withExpiration (Instant expiration)
  {
    this.expiration = expiration;
    return this;
  }

  public long getMinDuration ()
  {
    return minDuration;
  }

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

  @StateChange
  public TariffSpecification withPeriodicPayment (double periodicPayment)
  {
    this.periodicPayment = periodicPayment;
    return this;
  }

  public long getId ()
  {
    return id;
  }

  public Broker getBroker ()
  {
    return broker;
  }

  public List<Rate> getRates ()
  {
    return rates;
  }

  @StateChange
  public TariffSpecification addRate (Rate rate)
  {
    rates.add(rate);
    rate.setTariffId(id);
    return this;
  }

  public List<Long> getSupersedes ()
  {
    return supersedes;
  }
  
  @StateChange
  public TariffSpecification addSupersedes (long specId)
  {
    if (supersedes == null)
      supersedes = new ArrayList<Long>();
    supersedes.add(specId);
    return this;
  }

  @Override
  public TariffStatus process (TariffMessageProcessor svc)
  {
    return svc.processTariff(this);
  }
}
