/*
 * Copyright 2009-2011 the original author or authors.
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
package org.powertac.common.msg;

import java.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.TariffSpecification;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Request to change expiration date on an existing tariff. There are no
 * constraints on the new expiration date, but dates in the past will not
 * have retroactive effect. Created by brokers.
 * 
 * State log fields for readResolve():<br>
 * new(long brokerId, long tariffId, Instant expiration)
 * 
 * @author John Collins
 */
@Domain (fields = {"broker", "tariffId", "newExpiration"})
@XStreamAlias("tariff-exp")
public class TariffExpire extends TariffUpdate
{
  @XStreamAsAttribute
  private Instant newExpiration;
  
  public TariffExpire (Broker broker, TariffSpecification tariff,
                       Instant expiration)
  {
    super(broker, tariff);
    newExpiration = expiration;
  }

  public Instant getNewExpiration ()
  {
    return newExpiration;
  }
}
