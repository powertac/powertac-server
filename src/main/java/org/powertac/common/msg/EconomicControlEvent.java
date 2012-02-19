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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @author John Collins
 */
@XStreamAlias("economic-control")
public class EconomicControlEvent extends TariffUpdate
{
  @XStreamAsAttribute
  private double curtailment = 0.0;
  
  /**
   * 
   */
  public EconomicControlEvent (Broker broker,
                               TariffSpecification tariff,
                               double curtailment)
  {
    super(broker, tariff);
    this.curtailment = curtailment;
  }

  /**
   * 
   */
  public EconomicControlEvent (Broker broker,
                               long tariffId,
                               double curtailment)
  {
    super(broker, tariffId);
    this.curtailment = curtailment;
  }

}
