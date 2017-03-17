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

import org.powertac.common.Broker;
import org.powertac.common.TariffSpecification;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Message sent by a broker to the subscribers to a particular tariff, requesting
 * them to curtail usage against that tariff in the specified timeslot. 
 * The curtailmentRatio parameter specifies the portion of total usage that
 * will be curtailed for that timeslot, but the actual curtailment is also
 * constrained by the Rate in effect during that timeslot, so it might be less.
 * Customer models can interpret this ratio in two ways: First, it might be
 * that an individual customer reduces usage by that amount over what would
 * have been used otherwise; second, it might be that the specified proportion
 * of the population represented by the customer model is completely curtailed
 * during that timeslot.
 * @author John Collins
 */
public abstract class ControlEvent extends TariffUpdate
{ 
  @XStreamAsAttribute
  private int timeslotIndex = 0;
  
  /**
   * Creates a new ControlEvent. Used only as a superclass constructor for
   * ControlEvent subtypes.
   */
  ControlEvent (Broker broker, TariffSpecification tariff, int timeslotIndex)
  {
    super(broker, tariff);
    this.timeslotIndex = timeslotIndex;
  }
  
  // protected constructor for simplified deserialization
  protected ControlEvent ()
  {
    super();
  }
  
  public int getTimeslotIndex ()
  {
    return timeslotIndex;
  }
}
