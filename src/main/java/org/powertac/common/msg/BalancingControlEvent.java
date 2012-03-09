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

import org.powertac.common.IdGenerator;
import org.powertac.common.state.Domain;
import org.powertac.common.state.XStreamStateLoggable;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents a curtailment of energy by the DU as part of its balancing process.
 * A BalancingControlEvent can only be used by the DU in response to a
 * BalancingOrder issued by a Broker, and only if needed to resolve an
 * imbalance.
 * @author John Collins
 */
@Domain(fields = { "tariffId", "kwh" })
@XStreamAlias("balancing-control")
public class BalancingControlEvent extends XStreamStateLoggable
{
  @XStreamAsAttribute
  protected long id = IdGenerator.createId();

  @XStreamAsAttribute
  private long tariffId = 0;

  @XStreamAsAttribute
  private double kwh = 0.0;
  
  /**
   * Creates a new BalancingControlEvent to represent a curtailment in the 
   * current timeslot. Presumably this will be generated AFTER the customer
   * models have run in the timeslot, so the customer models must adapt their
   * behavior as though the curtailment has already happened.
   */
  public BalancingControlEvent (long tariffId,
                                double kwh)
  {
    super();
    this.tariffId = tariffId;
    this.kwh = kwh;
  }
  
  public long getId()
  {
    return id;
  }
  
  public long getTariffId ()
  {
    return tariffId;
  }
  
  public double getKwh()
  {
    return kwh;
  }
}
