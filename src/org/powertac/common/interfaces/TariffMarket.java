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

package org.powertac.common.interfaces;

import java.util.List;

import org.powertac.common.Tariff;
import org.powertac.common.AbstractCustomer;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.VariableRateUpdate;

/**
 * Tariff Market Receives, validates, and stores new tariffs, enforces tariff 
 * validity rules. Generates transactions to represent tariff publication fees.
 * Provides convenience methods to find tariffs that might be of interest to Customers.
 * <p>
 * Note that all methods driven by messages from the incoming message channel are
 * polymorphic methods that select by argument type at runtime. They all return a 
 * TariffStatus instance that can be routed back to the originating broker.</p>
 *
 * @author John Collins
 */
public interface TariffMarket {

  // --------------- Broker API ---------------------
  /**
   * Processes incoming {@link TariffSpecification} of a broker, 
   * turns it into a Tariff instance, and validates it. Returns a TariffStatus 
   * instance that can be routed back to the originating broker.
   */
  public TariffStatus processTariff(TariffSpecification spec);

  /**
   * Processes incoming {@link TariffUpdateCmd} from a broker that can be used
   * to revoke a tariff or change its expiration date.
   */
  public TariffStatus processTariff(TariffExpire update);

  /**
   * Processes incoming {@link TariffUpdateCmd} from a broker that can be used
   * to revoke a tariff or change its expiration date.
   */
  public TariffStatus processTariff(TariffRevoke update);

  /**
   * Processes HourlyCharge updates for variable rates.
   */
  public TariffStatus processTariff(VariableRateUpdate update);

  // -------------------- Customer API ------------------------
  /**
   * Subscribes a block of Customers from a single Customer model to
   * the specified Tariff, as long as the Tariff has not expired. If the
   * subscription succeeds, then the TariffSubscription instance is
   * return, otherwise null.
   * <p>
   * Note that you cannot unsubscribe directly from a Tariff -- you have to do
   * that from the TariffSubscription that represents the Tariff you want
   * to unsubscribe from.</p>
   */
  TariffSubscription subscribeToTariff (Tariff tariff,
                                        AbstractCustomer customer, 
                                        int customerCount);
  
  /**
   * Returns the list of currently active tariffs for the given PowerType.
   * The list contains only non-expired tariffs that cover the given type.
   */
  public List<Tariff> getActiveTariffList(PowerType type);
  
  /**
   * Returns the list of tariffs that have been revoked and have
   * active subscriptions. Customers are obligated to process this
   * list by calling handleRevokedTariff() on each such subscription.
   */
  public List<TariffSubscription> getRevokedSubscriptionList(AbstractCustomer customer);
  
  /**
   * Returns the default tariff.
   */
  public Tariff getDefaultTariff (PowerType type);
  
  /**
   * Convenience method to set the default tariff at the beginning of the game.
   * Returns true just in case the tariff was valid and was successfully saved.
   */
  public boolean setDefaultTariff (TariffSpecification newTariff);
  
  /**
   * Registers a listener for publication of new Tariffs.
   */
  public void registerNewTariffListener (NewTariffListener listener);
}
