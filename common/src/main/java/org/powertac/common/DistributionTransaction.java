/*
 * Copyright 2011-2015 the original author or authors.
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

import org.powertac.common.state.ChainedConstructor;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents the fee assessed
 * by the Distribution Utility for transport of energy over its facilities
 * during the current timeslot. The kWh is the total energy delivered,
 * which is the sum of the positive net load of the broker's customers, and 
 * the positive net export of energy through the wholesale market. Negative
 * values are ignored.
 *
 * @author John Collins
 */
@Domain(fields = {"postedTimeslot", "NSmall", "NLarge", "KWh", "charge"})
@XStreamAlias("distribution-tx")
public class DistributionTransaction extends BrokerTransaction
{
  /** The total positive amount of energy transported in kWh.
   */
  @XStreamAsAttribute
  private double kWh = 0.0;

  /** Number of small-customer meter charges
   */
  @XStreamAsAttribute
  private int nSmall = 0;

  /** Number of large-customer meter charges
   */
  @XStreamAsAttribute
  private int nLarge = 0;

  /** The total charge imposed by the DU for this transport. Since this
   * is a debit, it will always be negative. */
  @XStreamAsAttribute
  private double charge = 0.0;

  @ChainedConstructor
  public DistributionTransaction (Broker broker, int when, 
                                  double kwh, double charge)
  {
    this(broker, when, 0, 0, kwh, charge);
  }

  public DistributionTransaction (Broker broker, int when,
                                  int nSmall, int nLarge,
                                  double kwh, double charge)
  {
    super(when, broker);
    this.nSmall = nSmall;
    this.nLarge = nLarge;
    this.kWh = kwh;
    this.charge = charge;
  }

  @Deprecated
  // may be in use by older brokers
  public double getQuantity ()
  {
    return kWh;
  }

  /**
   * Returns the transported energy quantity represented by this transaction.
   * Will be non-zero only if transport fees are being assessed.
   */
  public double getKWh ()
  {
    return kWh;
  }

  /**
   * Returns the number of small customer subscriptions for which meter fees
   * are assessed.
   */
  public int getNSmall ()
  {
    return nSmall;
  }

  /**
   * Returns the number of large customer subscriptions for which meter fees
   * are assessed.
   */
  public int getNLarge ()
  {
    return nLarge;
  }

  /**
   * Returns the total fee assessed for transport and customer connections.
   */
  public double getCharge ()
  {
    return charge;
  }

  @Override
  public String toString() {
    return (String.format("Distribution tx %d-%s-%d-%d-%.3f-%.3f",
                          postedTimeslot, broker.getUsername(),
                          nSmall, nLarge, kWh, charge));
  }
}
