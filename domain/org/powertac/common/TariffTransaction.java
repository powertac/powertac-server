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

package org.powertac.common

import org.joda.time.Instant
import org.powertac.common.enumerations.TariffTransactionType
import org.powertac.common.transformer.BrokerConverter
import org.powertac.common.transformer.CustomerConverter
import org.powertac.common.transformer.TariffConverter

import com.thoughtworks.xstream.annotations.*

/**
 * A {@code TariffTransaction} instance represents the quantity of energy consumed
 * ({@code quantity < 0}) or produced {@code quantity > 0} by some members of a 
 * specific customer model, in a specific timeslot, under a particular tariff.
 * Note that this is an immutable type, and therefore is not auditable. Instances
 * are created by the TariffMarket and communicated to brokers to represent customer
 * interactions with tariffs (subscribe, consume, etc.).
 *
 * @author Carsten Block, John Collins
 */
@XStreamAlias("tariff-tx")
class TariffTransaction //implements Serializable
{
  @XStreamAsAttribute
  Integer id

  /** Whose transaction is this? */
  @XStreamConverter(BrokerConverter)
  Broker broker

  /** Purpose of this transaction */
  @XStreamAsAttribute
  TariffTransactionType txType = TariffTransactionType.CONSUME

  /** The customerInfo or more precisely his meter that is being read */
  @XStreamConverter(CustomerConverter)
  CustomerInfo customerInfo

  /** Number of individual customers involved */
  @XStreamAsAttribute
  Integer customerCount = 0

  /** The timeslot for which this meter reading is generated */
  Instant postedTime

  /** The total quantity of energy consumed (> 0) or produced (< 0) in kWh.
   *  Note that this is not per-individual in a population model, but rather
   *  aggregate usage by customerCount individuals. */
  @XStreamAsAttribute
  double quantity = 0.0

  /** The total charge for this reading, according to the tariff:
   *  positive for credit to broker, negative for debit from broker */
  @XStreamAsAttribute
  double charge = 0.0

  @XStreamConverter(TariffConverter)
  TariffSpecification tariffSpec

  /** The Tariff that applies to this billing */
  //static belongsTo = Tariff

  static constraints = {
    //id (nullable: false, unique: true)
    broker(nullable: false)
    customerInfo (nullable: true) // no customer for publication
    tariffSpec (nullable: false)
    postedTime (nullable: false)
    quantity (scale: Constants.DECIMALS)
    charge (scale: Constants.DECIMALS)
  }

  static mapping = { // id (generator: 'assigned')
    tariff fetch: 'join' }

  public String toString() {
    return "${customerInfo}-${postedTime.millis/TimeService.HOUR}-${txType}-${quantity}"
  }
}
