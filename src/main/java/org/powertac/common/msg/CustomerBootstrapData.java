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
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.state.Domain;
import org.powertac.common.xml.DoubleArrayConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

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

  // cannot use Customer here, because identity (and id value) is not
  // preserved across process boundaries
  @XStreamAsAttribute
  private String customerName;

  @XStreamAsAttribute
  private PowerType powerType;

  @XStreamConverter(DoubleArrayConverter.class)
  private double[] netUsage;

  public CustomerBootstrapData (CustomerInfo customer, PowerType powerType,
                                double[] netUsage)
  {
    super();
    this.customerName = customer.getName();
    this.powerType = powerType;
    this.netUsage = netUsage;
  }

  public long getId ()
  {
    return id;
  }

  public String getCustomerName ()
  {
    return customerName;
  }

  public PowerType getPowerType ()
  {
    return powerType;
  }

  public double[] getNetUsage ()
  {
    return netUsage;
  }
}