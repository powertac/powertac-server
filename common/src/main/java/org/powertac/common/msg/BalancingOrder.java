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
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents an offer of balancing capacity from a broker to the DU.
 * Applicable only to tariffs that have a RegulationRate or a Rate with
 * non-zero maxCurtailment,  for
 * the current timeslot, and a non-empty set of customer subscriptions.
 * 
 * A BalancingOrder can specify up-regulation by curtailment
 * (0 < exerciseRatio <= 1), or by discharging a battery
 * (1 < exerciseRatio <= 2). A BalancingOrder can specify down-regulation
 * (-1 <= exerciseRatio < 0), which translates to charging a battery (possibly
 * faster than its default charge rate) or to dumping thermal energy into some
 * type of thermal storage device (e.g. making more ice or raising the
 * temperature of a water heater).
 *
 * The Broker specifies that proportion of the remaining curtailable capacity
 * (remaining after possible application of an economic control) that can be
 * curtailed for balancing purposes, but note that the actual curtailment
 * is also constrained by the Rates in force during the timeslot. Once
 * submitted, a BalancingOrder remains in effect until replaced with another
 * BalancingOrder for the same Tariff. If the DU exercises a BalancingOrder,
 * the broker is paid the specified price just to the extent that the
 * balancing is for the benefit of some other broker. In other words, if the
 * curtailment is used exclusively for the benefit of the issuing broker,
 * it simply offsets the payment the broker would otherwise have had to
 * make to the DU for the shortage or surplus.
 * 
 * State log fields for readResolve():<br>
 * new(double exerciseRatio, double price, long tariffId, long brokerId)
 * 
 * @author John Collins
 */
@Domain(fields = { "exerciseRatio", "price", "tariffId", "broker" })
@XStreamAlias("balancing-order")
public class BalancingOrder extends TariffUpdate
{
  // maximum ratio of curtailable usage on this tariff that can be curtailed
  // for up-regulation. 
  @XStreamAsAttribute
  private double exerciseRatio = 0.0;

  // price/kwh for exercising this order. Positive values represent
  // credits to the broker.
  @XStreamAsAttribute
  private double price = 0.0;

  /**
   * Creates a new BalancingOrder for the given tariff. If cleared in the
   * balancing market, the price is the per-kWh value, positive if the broker
   * is paid. The exerciseRatio specifies the allowable amount of regulation
   * covered by this order. Values between 0.0 and 1.0 represent curtailment,
   * or up-regulation. Further up-regulation is possible for storage devices
   * with values between 1.0 and 2.0. Down-regulation (charging a battery or
   * dumping energy into a thermal storage device) is represented by values
   * between -1.0 and 0.0. In most cases, a broker should expect to be paid
   * (positive price) for up-regulation, and to pay (negative price) 
   * for down-regulation.
   */
  public BalancingOrder (Broker broker, TariffSpecification tariff,
                         double exerciseRatio, double price)
  {
    super(broker, tariff);
    this.exerciseRatio = exerciseRatio;
    this.price = price;
  }
  
  public double getExerciseRatio ()
  {
    return exerciseRatio;
  }
  
  public double getPrice ()
  {
    return price;
  }
  
  /**
   * Valid if exerciseRatio between -1 and 2
   */
  @Override
  public boolean isValid()
  {
    if (exerciseRatio < -1.0 || exerciseRatio > 2.0) {
      return false;
    }
    return true;
  }
  
  // protected default constructor to simplify deserialization
  protected BalancingOrder ()
  {
    super();
  }
}
