/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.common;

import org.powertac.common.state.XStreamStateLoggable;
import org.powertac.common.xml.BrokerConverter;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * Supertype of all tariff-related messages that can be processed by
 * a TariffMessageProcessor.
 * @author John Collins
 */
public abstract class TariffMessage
extends XStreamStateLoggable
implements ValidatableMessage
{
  @XStreamAsAttribute
  protected long id = IdGenerator.createId();
  
  /** The broker originating this message */
  @XStreamConverter(BrokerConverter.class)
  protected Broker broker;
  
  public TariffMessage (Broker broker)
  {
    super();
    this.broker = broker;
  }

  public long getId ()
  {
    // handle config cases that don't set ID
    if (0l == id)
      id = IdGenerator.createId();
    return id;
  }

  public Broker getBroker ()
  {
    return broker;
  }
  
  @Override
  public boolean isValid ()
  {
    return true;
  }
  
  // protected constructor for simplified deserialization
  protected TariffMessage()
  {
    super();
  }
}
