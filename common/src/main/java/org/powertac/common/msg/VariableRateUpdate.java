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
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Conveys an HourlyCharge instance, labeled by its Tariff and
 * Rate. When received by the server, the HourlyCharge simply
 * needs to be added to its Rate.
 * 
 * State log fields for readResolve():<br>
 * new(long brokerId, long tariffId, long hourlyChargeId, long rateId)
 * 
 * @author John Collins
 */
@Domain (fields = {"broker", "tariffId", "payload", "rateId"})
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
  
  /**
   * conventional getter to satisfy beanutils
   */
  public HourlyCharge getPayload ()
  {
    return payload;
  }
  
  public long getHourlyChargeId ()
  {
    return payload.getId();
  }

  public long getRateId ()
  {
    return rateId;
  }
  
  /**
   * By default, these are invalid. You have to supply the Rate to
   * test validity.
   */
  @Override
  public boolean isValid ()
  {
    return false;
  }
  
  /**
   * Given a Rate, a VRU is valid if it has the correct ID, if the Rate
   * is not fixed, and if the HourlyCharge specifies a value 
   * between the minValue and maxValue of the Rate.
   */
  public boolean isValid (Rate rate)
  {
    if (rate.getId() != rateId)
      return false;
    if (rate.isFixed()) 
      return false;
    double value = payload.getValue();
    
    if (0.0 == rate.getMinValue() && 0.0 == rate.getMaxValue())
      return (0.0 == value);
    
    double sgn = 1.0;
    if (rate.getMaxValue() < 0.0)
      sgn = -1.0;
    if (sgn * value < sgn * rate.getMinValue() || sgn * value > sgn * rate.getMaxValue())
      return false;
    return true;
  }
  
  // protected default constructor for reflection
  protected VariableRateUpdate ()
  {
    super();
  }
}
