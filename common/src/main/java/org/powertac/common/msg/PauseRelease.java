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
 * This message is used by a broker to release a pause in the simulation that
 * was previously requested by the same broker. If valid (the simulation is
 * actually paused as a result of a request by the same broker), it will in
 * turn result in a broadcast sim-resume message giving the updated start time.
 * @author John Collins
 */
@XStreamAlias("pause-release")
public class PauseRelease
{
  /** The broker who is requesting the pause release. */
  @XStreamConverter(BrokerConverter.class)
  private Broker broker;
  
  public PauseRelease (Broker broker)
  {
    super();
    this.broker = broker;
  }

  public Broker getBroker ()
  {
    return broker;
  }
}
