/*
 * Copyright (c) 2013 by the original author
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

import org.powertac.common.Broker;
import org.powertac.common.IdGenerator;
import org.powertac.common.state.ChainedConstructor;
import org.powertac.common.state.Domain;
import org.powertac.common.state.XStreamStateLoggable;
import org.powertac.common.xml.BrokerConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * Represents a response from the server about an Order that could not be
 * processed by the Auctioneer.
 * 
 * @author John Collins
 */
@Domain(fields = {"broker", "orderId", "status"})
@XStreamAlias("order-status")
public class OrderStatus
extends XStreamStateLoggable
{
  public enum Status {timeslotDisabled};

  @XStreamAsAttribute
  protected long id = IdGenerator.createId();

  /** The broker originating this message */
  @XStreamConverter(BrokerConverter.class)
  protected Broker broker;

  @XStreamAsAttribute
  private long orderId;

  @XStreamAsAttribute
  private Status status = Status.timeslotDisabled;

  public OrderStatus (Broker broker, long orderId, Status status)
  {
    this.broker = broker;
    this.orderId = orderId;
    this.status = status;
  }
  
  /**
   * Convenience constructor for timeslotDisabled message
   */
  @ChainedConstructor
  public OrderStatus (Broker broker, long orderId)
  {
    this(broker, orderId, Status.timeslotDisabled);
  }
  
  public long getId ()
  {
    return id;
  }
  
  public Broker getBroker ()
  {
    return broker;
  }
  
  public Status getStatus ()
  {
    return status;
  }
  
  public long getOrderId ()
  {
    return orderId;
  }
}
