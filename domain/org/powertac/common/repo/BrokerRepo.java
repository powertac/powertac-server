/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.common.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.powertac.common.Broker;
import org.springframework.stereotype.Repository;

/**
 * Repository for Brokers, including competitors and other market participants.
 * @author John Collins
 */
@Repository
public class BrokerRepo implements DomainRepo
{
  // indexed by username, and by id value
  private HashMap<String, Broker> nameTable;
  private HashMap<Long, Broker> idTable;

  public BrokerRepo ()
  {
    super();
    nameTable = new HashMap<String, Broker>();
    idTable = new HashMap<Long, Broker>();
    instance = this; // testing support
  }
  
  public void add (Broker broker)
  {
    nameTable.put(broker.getUsername(), broker);
    idTable.put(broker.getId(), broker);
  }
  
  public Collection<Broker> list ()
  {
    return nameTable.values();
  }
  
  public Broker findByUsername (String username)
  {
    return nameTable.get(username);
  }
  
  public Broker findById (long id)
  {
    return idTable.get(id);
  }
  
  public List<String> findRetailBrokerNames ()
  {
    ArrayList<String> result = new ArrayList<String>();
    for (Broker broker : nameTable.values()) {
      if (!broker.isWholesale())
        result.add(broker.getUsername());
    }
    return result;
  }
  
  public List<Broker> findRetailBrokers ()
  {
    ArrayList<Broker> result = new ArrayList<Broker>();
    for (Broker broker : nameTable.values()) {
      if (!broker.isWholesale())
        result.add(broker);
    }
    return result;
  }
  
  public List<Broker> findWholesaleBrokers ()
  {
    ArrayList<Broker> result = new ArrayList<Broker>();
    for (Broker broker : nameTable.values()) {
      if (broker.isWholesale())
        result.add(broker);
    }
    return result;
  }
  
  public void recycle ()
  {
    nameTable.clear();
    idTable.clear();
  }

  // testing support - keep a singleton instance around for cases
  // where autowiring does not work.
  private static BrokerRepo instance;
  
  public static BrokerRepo getInstance ()
  {
    return instance;
  }
}
