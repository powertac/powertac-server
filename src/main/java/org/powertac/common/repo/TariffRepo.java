/*
* Copyright (c) 2011 by the original author
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.BalancingOrder;
import org.springframework.stereotype.Repository;

/**
 * Repository for TariffSpecifications, Tariffs, Rates, and other related types.
 * @author John Collins
 */
@Repository
public class TariffRepo implements DomainRepo
{
  static private Logger log = Logger.getLogger(TariffRepo.class.getName());

  private HashMap<Long, TariffSpecification> specs;
  private HashSet<Long> deletedTariffs;
  private HashMap<Long, Tariff> tariffs;
  private HashMap<Long, Rate> rates;
  private HashMap<Long, BalancingOrder> balancingOrders;
  
  public TariffRepo ()
  {
    super();
    specs = new HashMap<Long, TariffSpecification>();
    deletedTariffs = new HashSet<Long>();
    tariffs = new HashMap<Long, Tariff>();
    rates = new HashMap<Long, Rate>();
    balancingOrders = new HashMap<Long, BalancingOrder>();
  }
  
  /**
   * Adds a TariffSpecification to the repo just in case another spec
   * (or this one) has not already been added sometime in the past.
   */
  public synchronized void addSpecification (TariffSpecification spec)
  {
    if (isRemoved(spec.getId()) || null != specs.get(spec.getId())) {
      log.error("Attempt to insert tariff spec with duplicate ID " + spec.getId());
      return;
    }
    specs.put(spec.getId(), spec);
    for (Rate r : spec.getRates()) {
      rates.put(r.getId(), r);
    }
  }
  
  public synchronized TariffSpecification findSpecificationById (long id)
  {
    return specs.get(id);
  }
  
  public synchronized List<TariffSpecification> findAllTariffSpecifications()
  {
    return new ArrayList<TariffSpecification>(specs.values());
  }
  
  public synchronized void addTariff (Tariff tariff)
  {
    if (isRemoved(tariff.getId()) || null != tariffs.get(tariff.getId())) {
      log.error("Attempt to insert tariff with duplicate ID " + tariff.getId());
      return;
    }
    tariffs.put(tariff.getId(), tariff);
  }
  
  public synchronized Tariff findTariffById (long id)
  {
    return tariffs.get(id);
  }
  
  public synchronized List<Tariff> findAllTariffs ()
  {
    return new ArrayList<Tariff>(tariffs.values());
  }

  public synchronized List<Tariff> findTariffsByState (Tariff.State state)
  {
    ArrayList<Tariff> result = new ArrayList<Tariff>();
    for (Tariff tariff : tariffs.values()) {
      if (state == tariff.getState()) {
        result.add(tariff);
      }
    }
    return result;
  }

  /**
   * Returns the list of active tariffs that exactly match the given
   * PowerType.
   */
  public synchronized List<Tariff> findActiveTariffs (PowerType type)
  {
    List<Tariff> result = new ArrayList<Tariff>();
    for (Tariff tariff : tariffs.values()) {
      if (tariff.getPowerType() == type && tariff.isActive() &&
              !tariff.isExpired() && !tariff.isRevoked()) {
        result.add(tariff);
      }
    }
    return result;
  }

  /**
   * Returns the list of active tariffs that can be used by a customer
   * of the given PowerType, including those that are more generic than
   * the specific type.
   */
  public synchronized List<Tariff> findAllActiveTariffs (PowerType type)
  {
    List<Tariff> result = new ArrayList<Tariff>();
    for (Tariff tariff : tariffs.values()) {
      if (type.canUse(tariff.getPowerType()) && tariff.isActive() &&
              !tariff.isExpired() && !tariff.isRevoked()) {
        result.add(tariff);
      }
    }
    return result;
  }
  
  /**
   * Removes a tariff and its specification from the repo
   */
  public synchronized void removeTariff (Tariff tariff)
  {
    tariffs.remove(tariff.getId());
    deletedTariffs.add(tariff.getId());
    specs.remove(tariff.getId());
  }
  
  /**
   * Tests whether a tariff has been deleted.
   */
  public synchronized boolean isRemoved (long tariffId)
  {
    return deletedTariffs.contains(tariffId);
  }

  public synchronized Rate findRateById (long id)
  {
    return rates.get(id);
  }
  
  /**
   * Adds a balancing order, indexed by its TariffSpec
   */
  public synchronized void addBalancingOrder (BalancingOrder order)
  {
    if (null != specs.get(order.getTariffId())) {
      balancingOrders.put(order.getTariffId(), order);
    }
  }
  
  /**
   * Retrieves the complete set of balancing orders
   */
  public synchronized Collection<BalancingOrder> getBalancingOrders ()
  {
    return balancingOrders.values();
  }
  
  @Override
  public synchronized void recycle ()
  {
    specs.clear();
    tariffs.clear();
    rates.clear();
    balancingOrders.clear();
  }
}
