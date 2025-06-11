/*
 * Copyright (c) 2012 by the original author
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

import org.powertac.common.TariffSpecification;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents up-regulation or down-regulation by the DU as part of its
 * balancing process.
 * A BalancingControlEvent can only be used by the DU in response to a
 * BalancingOrder issued by a Broker, and only if needed to resolve an
 * imbalance.
 * 
 * @author John Collins
 */
@Domain(fields = { "tariffId", "kwh", "payment", "timeslotIndex" })
@XStreamAlias("balancing-control")
public class BalancingControlEvent extends ControlEvent
{
  @XStreamAsAttribute
  private double kwh = 0.0;

  @XStreamAsAttribute
  private double payment = 0.0;
  
  /**
   * Creates a new BalancingControlEvent to represent regulation in the 
   * current timeslot. Presumably this will be generated AFTER the customer
   * models have run in the timeslot, so the customer models must adapt their
   * behavior in the following timeslot to accommodate a curtailment that has 
   * already happened. A positive value for kwh indicates curtailment of
   * consumption, which is equivalent to additional energy to the broker's
   * account. However, this value must not be used to keep accounts, because
   * every BalancingControlEvent should be accompanied by a TariffTransaction
   * specifying the same amount of energy.
   */
  public BalancingControlEvent (TariffSpecification spec,
                                double kwh, double payment, int timeslotIndex)
  {
    super(spec.getBroker(), spec, timeslotIndex);
    this.kwh = kwh;
    this.payment = payment;
  }
  
  public double getKwh()
  {
    return kwh;
  }
  
  public double getPayment()
  {
    return payment;
  }
  
  // private constructor for simplified deserialization
  protected BalancingControlEvent ()
  {
    super();
  }
}
