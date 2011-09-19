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
package org.powertac.common.msg;

import java.util.ArrayList;

import com.thoughtworks.xstream.annotations.*;
import org.powertac.common.CustomerInfo;

/**
 * This message is used to notify the brokers of the competition of the available
 * customers and some of their information.
 * @author Anthony Chrysopoulos
 */
@Deprecated // This information is now in Competition
@XStreamAlias("customer-list")
public class CustomerList 
{
  ArrayList<CustomerInfo> customers;

  public CustomerList ()
  {
    super();
  }
  
  public CustomerList (ArrayList<CustomerInfo> data)
  {
    super();
    customers = data;
  }

  public ArrayList<CustomerInfo> getCustomers ()
  {
    return customers;
  }
  
  public void addCustomer (CustomerInfo customer)
  {
    customers.add(customer);
  }
}
