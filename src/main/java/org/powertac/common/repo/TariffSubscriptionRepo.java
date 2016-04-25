/*
 * Copyright (c) 2011-2015 by John Collins
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
package org.powertac.common.repo;

import static org.powertac.util.ListTools.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.util.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Repository for TariffSubscriptions. This cannot be in common, because
 * TariffSubscription is not in common.
 * 
 * @author John Collins
 */
@Service
public class TariffSubscriptionRepo implements DomainRepo
{
  // subscriptions are indexed by Tariff, CustomerInfo, and Broker
  private HashMap<Tariff, List<TariffSubscription>> tariffMap;
  private HashMap<CustomerInfo, List<TariffSubscription>> customerMap;
  private HashMap<Broker, List<TariffSubscription>> brokerMap;

  @Autowired
  private TariffRepo tariffRepo;

  public TariffSubscriptionRepo ()
  {
    super();
    tariffMap = new HashMap<Tariff, List<TariffSubscription>>();
    customerMap = new HashMap<CustomerInfo, List<TariffSubscription>>();
    brokerMap = new HashMap<Broker, List<TariffSubscription>>();
  }

  /** Adds an existing subscription to the repo. */
  public TariffSubscription add (TariffSubscription subscription)
  {
    storeSubscription(subscription,
                      subscription.getCustomer(),
                      subscription.getTariff());
    return subscription;
  }

  /**
   * Returns the TariffSubscription for the given Tariff/Customer pair, creating
   * it if necessary. Note that as of issue #505, you can get null back if you
   * try to get a subscription for a non-existent tariff, such as one that has
   * been revoked.
   */
  public TariffSubscription getSubscription (CustomerInfo customer,
                                             Tariff tariff)
  {
    TariffSubscription result =
        findSubscriptionForCustomer(tariffMap.get(tariff), customer);
    if (null != result) {
      // subscription exists
      return result;
    }
    result = new TariffSubscription(customer, tariff);
    storeSubscription(result, customer, tariff);
    return result;
  }

  /**
   * Returns the subscription, if any, for the specified tariff and customer.
   * Does not create a subscription in case it cannot be found in the repo.
   */
  public TariffSubscription
  findSubscriptionForTariffAndCustomer (Tariff tariff, CustomerInfo customer)
  {
    List<TariffSubscription> subs = findSubscriptionsForTariff(tariff);
    if (subs == null)
      return null;
    for (TariffSubscription sub : subs) {
      if (sub.getCustomer() == customer)
        return sub;
    }
    return null;
  }

  /** Returns the list of subscriptions for a given tariff. Return value
   * does not share structure with the repo. */
  public List<TariffSubscription> findSubscriptionsForTariff (Tariff tariff)
  {
    // new list allows caller to smash the return value
    List<TariffSubscription> result = tariffMap.get(tariff);
    if (result == null)
      return new ArrayList<TariffSubscription>();
    else
      return new ArrayList<TariffSubscription>(result);
  }

  /** Returns the list of subscriptions for a given customer. Return value
   * does not share structure with the repo. */
  public List<TariffSubscription>
  findSubscriptionsForCustomer (CustomerInfo customer)
  {
    List<TariffSubscription> result = customerMap.get(customer);
    if (null == result)
      return new ArrayList<TariffSubscription>();
    else
      return new ArrayList<TariffSubscription>(result);
  }

  /**
   * Returns the list of active subscriptions for a given customer.
   * These are subscriptions that have non-zero committed-customer counts.
   */
  public List<TariffSubscription>
  findActiveSubscriptionsForCustomer (CustomerInfo customer)
  {
    List<TariffSubscription> result = new ArrayList<TariffSubscription>();
    for (TariffSubscription sub : findSubscriptionsForCustomer(customer)) {
      if (sub.getCustomersCommitted() > 0) {
        result.add(sub);
      }
    }
    return result;
  }

  /**
   * Returns the list of subscriptions for the specified broker. Return
   * value does not share structure with the repo.
   */
  public List<TariffSubscription> findSubscriptionsForBroker(Broker b)
  {
    
    List<TariffSubscription> result = brokerMap.get(b);
    if (null == result)
      return new ArrayList<TariffSubscription>();
    else
      return new ArrayList<TariffSubscription>(result);
  }

  /**
   * Returns the list of active subscriptions for a given broker.
   * These are subscriptions that have non-zero committed-customer counts.
   */
  public List<TariffSubscription>
  findActiveSubscriptionsForBroker (Broker broker)
  {
    List<TariffSubscription> result = new ArrayList<TariffSubscription>();
    for (TariffSubscription sub : findSubscriptionsForBroker(broker)) {
      if (sub.getCustomersCommitted() > 0) {
        result.add(sub);
      }
    }
    return result;
  }

  /**
   * Returns the list of subscriptions for this customer that have been
   * revoked and have non-zero committed customer counts. 
   * Intended to be called in the context of tariff evaluation (typically
   * by the TariffEvaluator).
   */
  public List<TariffSubscription>
  getRevokedSubscriptionList (CustomerInfo customer)
  {
    if (null == customerMap.get(customer))
      // can happen first time...
      return new ArrayList<TariffSubscription>();
    List<TariffSubscription> result = 
        filter(customerMap.get(customer),
               new Predicate<TariffSubscription> () {
          @Override
          public boolean apply (TariffSubscription sub)
          {
            return (null != tariffRepo.findTariffById(sub.getTariff().getId())
                    && sub.getTariff().getState() == Tariff.State.KILLED
                    && sub.getCustomersCommitted() > 0);
          }
        });
    return result;
  }
  
  /**
   * Removes all subscriptions for the given tariff. Presumably this is done
   * when the tariff has been revoked and all revocation processing is complete.
   */
  public void removeSubscriptionsForTariff (Tariff tariff)
  {
    List<TariffSubscription> subs = tariffMap.get(tariff);
    if (null == subs)
      return;
    
    // first, remove the subscriptions from the customer map and broker map
    for (TariffSubscription sub : subs) {
      customerMap.get(sub.getCustomer()).remove(sub);
    }
    for (TariffSubscription sub : subs) {
      brokerMap.get(sub.getTariff().getBroker()).remove(sub);
    }

    // then clear out the tariff entry
    tariffMap.remove(tariff);
  }

  /** Clears out the repo in preparation for another simulation. */
  @Override
  public void recycle ()
  {
    tariffMap.clear();
    customerMap.clear();
    brokerMap.clear();
  }

  // ----- helper methods -----

  private TariffSubscription
  findSubscriptionForCustomer (List<TariffSubscription> subs,
                               CustomerInfo customer)
  {
    if (subs == null)
      return null;
    for (TariffSubscription sub : subs) {
      if (sub.getCustomer() == customer)
        return sub;
    }
    return null;
  }

  private void storeSubscription (TariffSubscription subscription,
                                  CustomerInfo customer,
                                  Tariff tariff)
  {
    if (tariffMap.get(tariff) == null)
      tariffMap.put(tariff, new ArrayList<TariffSubscription>());
    tariffMap.get(tariff).add(subscription);
    if (customerMap.get(customer) == null)
      customerMap.put(customer, new ArrayList<TariffSubscription>());
    customerMap.get(customer).add(subscription);
    Broker broker = tariff.getBroker();
    if (brokerMap.get(broker) == null)
      brokerMap.put(broker, new ArrayList<TariffSubscription>());
    brokerMap.get(broker).add(subscription);
  }
}
