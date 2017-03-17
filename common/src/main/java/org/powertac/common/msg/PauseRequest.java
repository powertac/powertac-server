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

import org.powertac.common.Broker;
import org.powertac.common.xml.BrokerConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * This message is used by a broker to request a pause in the simulation. 
 * If the server is configured to allow broker-initiated pause, then the
 * clock will be paused, either in the current timeslot if either the clock
 * is already paused or if the watchdog timer has not run out, or in the
 * succeeding timeslot. In the last two instances, the server will send out
 * a sim-pause message in response. Since this is a tool for experimentation
 * and debugging, there is no time limit on the pause. To release a requested
 * pause, the same broker must submit a pause-release message, which will in
 * turn result in a sim-resume message giving the updated start time.
 * @author John Collins
 */
@XStreamAlias("pause-request")
public class PauseRequest
{
  /** The broker who is requesting this pause  */
  @XStreamConverter(BrokerConverter.class)
  private Broker broker;
  
  public PauseRequest (Broker broker)
  {
    super();
    this.broker = broker;
  }

  public Broker getBroker ()
  {
    return broker;
  }
}
