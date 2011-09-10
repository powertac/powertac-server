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
import org.powertac.common.HourlyCharge;
import org.powertac.common.Rate;

import com.thoughtworks.xstream.annotations.*;

/**
 * Conveys an HourlyCharge instance, labeled by its Tariff and
 * Rate. When received by the server, the HourlyCharge simply
 * needs to be added to its Rate.
 * @author jcollins
 */
@XStreamAlias("tariff-vru")
public class VariableRateUpdate extends TariffUpdate
{
  private HourlyCharge payload;
  
  @XStreamAsAttribute
  private long rateId;
  
  public VariableRateUpdate (Broker broker, Rate rate, HourlyCharge hourlyCharge)
  {
    super(broker, rate.getTariffId());
    rateId = rate.getId();
    payload = hourlyCharge;
  }

  public HourlyCharge getHourlyCharge ()
  {
    return payload;
  }

  public long getRateId ()
  {
    return rateId;
  }
}
