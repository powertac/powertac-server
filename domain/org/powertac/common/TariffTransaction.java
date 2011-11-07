/*
 * Copyright 2009-2010 the original author or authors.
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

package org.powertac.common;


import org.joda.time.Instant;
import org.powertac.common.state.Domain;
import org.powertac.common.xml.CustomerConverter;
import org.powertac.common.xml.TariffSpecificationConverter;

import com.thoughtworks.xstream.annotations.*;

/**
 * A {@code TariffTransaction} instance represents the kWh of energy consumed
 * ({@code kWh < 0}) or produced {@code kWh > 0} by some members of a 
 * specific customer model, in a specific timeslot, under a particular tariff.
 * Note that this is an immutable type, and therefore is not auditable. Instances
 * are created by the TariffMarket and communicated to brokers to represent customer
 * interactions with tariffs (subscribe, consume, etc.).
 *
 * @author Carsten Block, John Collins
 */
@Domain
@XStreamAlias("tariff-tx")
public class TariffTransaction extends BrokerTransaction
{
  public enum Type { PUBLISH, PRODUCE, CONSUME, PERIODIC, SIGNUP, WITHDRAW, REVOKE }
  
  /** Purpose of this transaction */
  @XStreamAsAttribute
  private Type txType = Type.CONSUME;

  /** The customerInfo or more precisely his meter that is being read */
  @XStreamConverter(CustomerConverter.class)
  private CustomerInfo customerInfo;

  /** Number of individual customers involved */
  @XStreamAsAttribute
  private int customerCount = 0;

  /** The total kWh of energy consumed (< 0) or produced (> 0) in kWh.
   *  Note that this is not per-individual in a population model, but rather
   *  aggregate usage by customerCount individuals. */
  @XStreamAsAttribute
  private double kWh = 0.0;

  /** The total charge for this reading, according to the tariff:
   *  positive for credit to broker, negative for debit from broker */
  @XStreamAsAttribute
  private double charge = 0.0;

  @XStreamConverter(TariffSpecificationConverter.class)
  private TariffSpecification tariffSpec;
  
  /**
   * Creates a new TariffTransaction for broker of type txType against
   * a particular tariff spec and customer. Energy quantity and charge
   * is specified from the Broker's viewpoint, so a consumption transaction
   * would have kwh < 0 and charge > 0.
   */
  public TariffTransaction (Broker broker, Instant when, 
                            Type txType,
                            TariffSpecification spec, 
                            CustomerInfo customer,
                            int customerCount,
                            double kWh, double charge)
  {
    super(when, broker);
    this.txType = txType;
    this.tariffSpec = spec;
    this.customerInfo = customer;
    this.customerCount = customerCount;
    this.kWh = kWh;
    this.charge = charge;
  }

  public Type getTxType ()
  {
    return txType;
  }

  public CustomerInfo getCustomerInfo ()
  {
    return customerInfo;
  }

  /**
   * Number of individual customers within the customer model represented
   * by this transaction. The value will always be less than or equal to 
   * the population represented by the customerInfo.
   */
  public int getCustomerCount ()
  {
    return customerCount;
  }

  /**
   * Returns the debit (negative) or credit (positive) to the broker's
   * energy account in the current timeslot represented by this transaction.
   */
  public double getKWh ()
  {
    return kWh;
  }

  /**
   * Returns the debit (negative) or credit (positive) to the broker's
   * money account represented by this transaction.
   */
  public double getCharge ()
  {
    return charge;
  }

  public TariffSpecification getTariffSpec ()
  {
    return tariffSpec;
  }

  public String toString() {
    return("TariffTx: customer" + customerInfo.getId() + " at " +
           Competition.currentCompetition().computeTimeslotIndex(postedTime) +
           ", " + txType + ": " + kWh + "@" + charge);
  }
}
