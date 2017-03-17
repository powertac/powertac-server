/*
 * Copyright (c) 2012-2014 by the original author
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.TariffSpecification;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Message sent by a broker to the subscribers to a particular tariff, requesting
 * them to adjust usage against that tariff in the specified timeslot. 
 * The curtailmentRatio parameter specifies the portion of total usage that
 * will be curtailed for that timeslot, but the actual curtailment is also
 * constrained by the Rate in effect during that timeslot, so it might be less.
 * Customer models can interpret this ratio in two ways: First, it might be
 * that an individual customer reduces usage by that amount over what would
 * have been used otherwise; second, it might be that the specified proportion
 * of the population represented by the customer model is completely curtailed
 * during that timeslot.
 * 
 * If the applicable tariff has one or more RegulationRates, then the
 * curtailmentRatio value can be used to withdraw energy from storage
 * (1.0 < ratio <= 2.0), or to deposit energy in storage, thereby
 * increasing the load (0.0 > ratio >= -1.0). Of course, giving ratio > 1.0
 * for anything other than a battery-like load will act like ratio = 1.0.
 * 
 * State log fields for readResolve():<br>
 * new(long tariffId, double curtailmentRatio, long timeslotIndex)
 * 
 * @author John Collins
 */
@Domain(fields = { "tariffId", "curtailmentRatio", "timeslotIndex" })
@XStreamAlias("economic-control")
public class EconomicControlEvent extends ControlEvent
{
  static private Logger log = LogManager.getLogger(EconomicControlEvent.class.getName());

  @XStreamAsAttribute
  private double curtailmentRatio = 0.0;
  
  /**
   * Creates a new EconomicControlEvent to take effect in the following 
   * timeslot. Package visibility reflects the fact that this is intended 
   * to be called by the factory method in ControlEvent.
   */
  public EconomicControlEvent (TariffSpecification tariff,
                               double curtailmentRatio,
                               int timeslotIndex)
  {
    super(tariff.getBroker(), tariff, timeslotIndex);
    if (tariff.hasRegulationRate()) {
      if (-1.0 > curtailmentRatio || 2.0 < curtailmentRatio) {
        log.error("Illegal curtailmentRatio: " + curtailmentRatio);
        curtailmentRatio = 0.0;
      }
    }
    else {
      if (0.0 > curtailmentRatio || 1.0 < curtailmentRatio) {
        log.error("Illegal curtailmentRatio: " + curtailmentRatio);
        curtailmentRatio = 0.0;
      }
    }
    this.curtailmentRatio = curtailmentRatio;
  }
  
  public double getCurtailmentRatio ()
  {
    return curtailmentRatio;
  }
  
  // protected default constructor to simplify deserialization
  protected EconomicControlEvent ()
  {
    super();
  }
}
