/*
 * Copyright (c) 2025 by the original author or authors.
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

import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Message type that is guaranteed to be sent by brokers as the last
 * outgoing message in a timeslot. It carries the timeslot index, so the
 * server can use it to check whether the broker is in sync. 
 * Note that this is not a "standard" domain type, since it does not
 * have an ID and is not logged in the state log. It is just a message.
 * @author John Collins
 */
@Domain
@XStreamAlias("br-done")
public class BrokerComplete
{
  @XStreamAsAttribute
  private int timeslotIndex = 0;

  public BrokerComplete (int timeslotIndex)
  {
    super();
    this.timeslotIndex = timeslotIndex;
  }

  public int getTimeslotIndex ()
  {
    return timeslotIndex;
  }
  
  @Override
  public String toString ()
  {
    return ("BrokerComplete#" + timeslotIndex);
  }
}