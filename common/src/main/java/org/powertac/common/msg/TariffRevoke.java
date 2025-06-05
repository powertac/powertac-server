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

import org.powertac.common.Broker;
import org.powertac.common.TariffSpecification;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Message created by a broker to request revocation of an existing tariff.
 * 
 * State log fields for readResolve():<br>
 * new(long brokerId, long tariffId)
 * 
 * @author John Collins
 */
@Domain (fields = {"broker", "tariffId"})
@XStreamAlias("tariff-rev")
public class TariffRevoke extends TariffUpdate
{
  public TariffRevoke (Broker broker, TariffSpecification tariff)
  {
    super(broker, tariff);
  }
  
  // protected default constructor for reflection
  protected TariffRevoke ()
  {
    super();
  }
}
