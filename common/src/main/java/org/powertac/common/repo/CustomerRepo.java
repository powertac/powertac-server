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

import static org.powertac.util.ListTools.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.util.Predicate;
import org.springframework.stereotype.Service;

/**
 * Simple repository for Customer instances.
 * @author John Collins
 */
@Service
public class CustomerRepo implements DomainRepo
{
  @SuppressWarnings("unused")
  static private Logger log = LogManager.getLogger(CustomerRepo.class.getName());

  private HashMap<Long,CustomerInfo> customers;
  
  public CustomerRepo ()
  {
    super();
    customers = new HashMap<Long,CustomerInfo>();
  }
  
  public CustomerInfo createCustomerInfo (String name, int population)
  {
    CustomerInfo result = new CustomerInfo(name, population);
    customers.put(result.getId(), result);
    return result;
  }
  
  public void add (CustomerInfo customer)
  {
    customers.put(customer.getId(), customer);
  }
  
  public Collection<CustomerInfo> list ()
  {
    return customers.values();
  }

  @Deprecated
  public int count ()
  {
    return size();
  }
  
  public int size()
  {
    return customers.values().size();
  }
  
  public CustomerInfo findById (long id)
  {
    return customers.get(id);
  }

  public List<CustomerInfo> findByName (final String name)
  {
    return filter(customers.values(),
                  new Predicate<CustomerInfo>() {
      @Override
      public boolean apply (CustomerInfo item)
      {
        return name.equals(item.getName());
      }  
    });
  }

  public CustomerInfo findByNameAndPowerType (final String name,
                                              final PowerType type)
  {
    List<CustomerInfo> candidates =
            filter(customers.values(),
                   new Predicate<CustomerInfo>() {
      @Override
      public boolean apply (CustomerInfo item)
      {
        return (name.equals(item.getName()) 
                && (type == item.getPowerType()
                    || type == item.getPowerType().getGenericType()));
      }
    });
    
    if (candidates.size() == 0)
      return null;

    if (candidates.size() > 1) {
      // return the first one with an exact match
      for (CustomerInfo customer : candidates)
        if (type == customer.getPowerType())
          return customer;
    }

    return candidates.get(0);
  }

  @Override
  public void recycle ()
  {
    customers.clear();
  }
}
