/*
* Copyright (c) 2011-2014 by John Collins.
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
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.Broker;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.BalancingOrder;
import org.springframework.stereotype.Service;

/**
 * Repository for TariffSpecifications, Tariffs, Rates, and other related types.
 * @author John Collins
 */
@Service
public class TariffRepo implements DomainRepo
{
  static private Logger log = LogManager.getLogger(TariffRepo.class.getName());

  private HashMap<Long, TariffSpecification> specs;
  private HashSet<Long> deletedTariffs;
  private HashMap<PowerType, Tariff> defaultTariffs;
  private HashMap<Long, Tariff> tariffs;
  private HashMap<Long, Rate> rates;
  private TreeMap<Long, BoPair> balancingOrders;
  private TreeMap<String, LinkedList<Tariff>> brokerTariffs;

  public TariffRepo ()
  {
    super();
    specs = new HashMap<>();
    deletedTariffs = new HashSet<>();
    defaultTariffs = new HashMap<>();
    tariffs = new HashMap<>();
    rates = new HashMap<>();
    balancingOrders = new TreeMap<>();
    brokerTariffs = new TreeMap<>();
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

  public void removeSpecification (long id)
  {
    specs.remove(id);
  }

  public void setDefaultTariff (TariffSpecification newSpec)
  {
    addSpecification(newSpec);
    Tariff tariff = new Tariff(newSpec);
    tariff.init();
    defaultTariffs.put(newSpec.getPowerType(), tariff);
  }
  
  public Tariff getDefaultTariff (PowerType type)
  {
    Tariff result = defaultTariffs.get(type);
    if (null == result) {
      result = defaultTariffs.get(type.getGenericType());
    }
    if (null == result) {
      log.error("Cannot find default tariff for PowerType " + type);
    }
    return result;
  }

  public synchronized TariffSpecification findSpecificationById (long id)
  {
    return specs.get(id);
  }

  public synchronized List<TariffSpecification>
  findTariffSpecificationsByBroker (Broker broker)
  {
    List<TariffSpecification> result = new ArrayList<>();
    for (TariffSpecification spec : specs.values()) {
      if (spec.getBroker() == broker) {
        result.add(spec);
      }
    }
    return result;
  }

  public synchronized List<TariffSpecification>
  findTariffSpecificationsByPowerType (PowerType type)
  {
    List<TariffSpecification> result = new ArrayList<>();
    for (TariffSpecification spec : specs.values()) {
      if (spec.getPowerType().canUse(type)) {
        result.add(spec);
      }
    }
    return result;
  }

  public synchronized List<TariffSpecification>
  findTariffSpecificationsByBrokerAndType (Broker broker, PowerType type)
  {
    List<TariffSpecification> result = new ArrayList<>();
    for (TariffSpecification spec : specs.values()) {
      if (spec.getBroker() == broker && spec.getPowerType().canUse(type)) {
        result.add(spec);
      }
    }
    return result;
  }

  public synchronized List<TariffSpecification> findAllTariffSpecifications()
  {
    return new ArrayList<>(specs.values());
  }
  
  public synchronized void addTariff (Tariff tariff)
  {
    // add to the tariffs list
    if (isRemoved(tariff.getId()) || null != tariffs.get(tariff.getId())) {
      log.error("Attempt to insert tariff with duplicate ID " + tariff.getId());
      return;
    }
    tariffs.put(tariff.getId(), tariff);
    
    // add to the brokerTariffs list
    LinkedList<Tariff> tariffList = brokerTariffs.get(tariff.getBroker().getUsername());
    if (null == tariffList) {
      tariffList = new LinkedList<>();
      brokerTariffs.put(tariff.getBroker().getUsername(), tariffList);
    }
    tariffList.push(tariff);
  }
  
  public synchronized Tariff findTariffById (long id)
  {
    return tariffs.get(id);
  }
  
  public synchronized List<Tariff> findAllTariffs ()
  {
    return new ArrayList<>(tariffs.values());
  }

  public synchronized List<Tariff> findTariffsByState (Tariff.State state)
  {
    ArrayList<Tariff> result = new ArrayList<>();
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
    List<Tariff> result = new ArrayList<>();
    for (Tariff tariff : tariffs.values()) {
      if (tariff.getPowerType() == type && tariff.isSubscribable()) {
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
    List<Tariff> result = new ArrayList<>();
    for (Tariff tariff : tariffs.values()) {
      if (type.canUse(tariff.getPowerType()) && tariff.isSubscribable()) {
        result.add(tariff);
      }
    }
    return result;
  }

  /**
   * Returns the n most "recent" active tariffs from each broker
   * that can be used by a customer with the given powerType. 
   */
  public synchronized List<Tariff> findRecentActiveTariffs (int n, PowerType type)
  {
    List<Tariff> result = new ArrayList<Tariff>();
    HashMap<PowerType,Integer> ptCounter = new HashMap<PowerType,Integer>(); 
    for (String userName : brokerTariffs.keySet()) {
      ptCounter.clear();
      for (Tariff tariff : brokerTariffs.get(userName)) {
        PowerType pt = tariff.getPowerType();
        if (tariff.isSubscribable() && type.canUse(pt)) {
          Integer count = ptCounter.get(pt);
          if (null == count)
            count = 0;
          if (count < n) {
            result.add(tariff);
            ptCounter.put(pt, count + 1);
          }
        }
      }
    }
    return result;
  }

  public List<Tariff> findTariffsByBroker (Broker broker)
  {
    List<Tariff> result = brokerTariffs.get(broker.getUsername());
    if (null == result)
      return new LinkedList<>();
    else
      return result;
  }

  /**
   * Removes a tariff and its specification from the repo. remembers that tariff
   * has been removed, prevents re-adding.
   */
  public synchronized void removeTariff (Tariff tariff)
  {
    tariffs.remove(tariff.getId());
    deletedTariffs.add(tariff.getId());
    removeSpecification(tariff.getId());
  }

  /**
   * Deletes a tariff and its specification from the repo, without tracking.
   * Should not be used in the server.
   */
  public synchronized void deleteTariff (Tariff tariff)
  {
    tariffs.remove(tariff.getId());
    List<Tariff> bt = brokerTariffs.get(tariff.getBroker().getUsername());
    bt.remove(tariff);
    removeSpecification(tariff.getId());
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
    long tariffId = order.getTariffId();
    if (null != specs.get(tariffId)) {
      BoPair pair = balancingOrders.get(tariffId);
      if (null == pair) {
        pair = new BoPair();
        balancingOrders.put(tariffId, pair);
      }
      pair.add(order);
    }
  }
  
  /**
   * Retrieves the map of tariff IDs to balancing orders for all tariffs 
   */
  public synchronized Collection<BalancingOrder> getBalancingOrders ()
  {
    ArrayList<BalancingOrder> result = new ArrayList<>();
    for (BoPair pair : balancingOrders.values()) {
      for (BalancingOrder item : pair.getOrders()) {
        result.add(item);
      }
    }
    return result;
  }
  
  @Override
  public synchronized void recycle ()
  {
    specs.clear();
    tariffs.clear();
    defaultTariffs.clear();
    deletedTariffs.clear();
    rates.clear();
    balancingOrders.clear();
    brokerTariffs.clear();
  }

  // Balancing orders come in pairs
  class BoPair
  {
    private BalancingOrder upOrder;
    private BalancingOrder downOrder;

    BoPair ()
    {
      super();
    }

    BalancingOrder getUpOrder ()
    {
      return upOrder;
    }

    void setUpOrder (BalancingOrder order)
    {
      upOrder = order;
    }

    BalancingOrder getDownOrder ()
    {
      return downOrder;
    }

    void setDownOrder (BalancingOrder order)
    {
      downOrder = order;
    }

    // adds a new order of either gender
    void add (BalancingOrder order)
    {
      if (order.getExerciseRatio() >= 0.0)
        upOrder = order;
      else
        downOrder = order;
    }

    // returns contents as a list
    List<BalancingOrder> getOrders ()
    {
      List <BalancingOrder> result = new ArrayList<>();
      if (null != upOrder)
        result.add(upOrder);
      if (null != downOrder)
        result.add(downOrder);
      return result;
    }
  }
}
