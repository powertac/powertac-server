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

import org.powertac.common.CustomerInfo;
import org.powertac.common.IdGenerator;
import org.powertac.common.state.Domain;
import org.powertac.common.xml.CustomerConverter;
import org.powertac.common.xml.DoubleArrayConverter;

import com.thoughtworks.xstream.annotations.*;

/**
 * This message encapsulates net power usage by timeslot for a customer
 * instance over the bootstrap period.
 * @author Anthony Chrysopoulos, John Collins
 */
@Domain
@XStreamAlias("customer-bootstrap-data")
public class CustomerBootstrapData
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  @XStreamAsAttribute
  @XStreamConverter(CustomerConverter.class)
  private CustomerInfo customer;
  
  @XStreamConverter(DoubleArrayConverter.class)
  private double[] netUsage;

  public CustomerBootstrapData (CustomerInfo customer,
                                double[] netUsage)
  {
    super();
    this.customer = customer;
    this.netUsage = netUsage;
  }

  public long getId ()
  {
    return id;
  }

  public CustomerInfo getCustomer ()
  {
    return customer;
  }

  public double[] getNetUsage ()
  {
    return netUsage;
  }
}