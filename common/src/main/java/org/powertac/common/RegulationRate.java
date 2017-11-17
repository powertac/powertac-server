/*
 * Copyright (c) 2013 by the original author or authors.
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
package org.powertac.common;

import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Tariffs are composed of Rates, including RegulationRates.
 * A RegulationRate specifies payments for
 * up-regulation and down-regulation
 * that might be used for balancing and might be different from the payments
 * for ordinary consumption and/or production.
 * Typically, the payment for up-regulation is positive (the customer is paid
 * for energy supplied), and the payment for down-regulation is negative
 * (the customer pays for energy delivered).
 * The response of the regulated capacity is either fast (ResponseTime.SECONDS)
 * or slow (ResponseTime.MINUTES). 
 * Examples of fast devices include resistance heaters and battery chargers. 
 * A heat pump is an example of a slow device --
 * once it's running, it must be kept running for several minutes; if it is
 * stopped, it must be left off for several minutes before re-starting it.
 * 
 * Typically, fast-response capacity is more valuable, and in the real world
 * would be completely automated, perhaps by sensing grid frequency and
 * up-regulating when the frequency drops, or down-regulating when frequency
 * rises.
 * Most tariffs that wish to pay for regulation capacity will include two
 * RegulationRate instances, one for fast response and one for slower response.
 * The determination of which one applies is delegated to the interaction
 * between the balancing market and the customer models that are subscribed
 * to the tariff.
 * 
 * Note that
 * (1) Up to two RegulationRates can be attached to a TariffSpecification --
 * one for fast response, and one for slow response; and
 * (2) the inclusion of a RegulationRate overrides the maxCurtailment
 * attribute of any Rates attached to the same TariffSpecification.
 * In addition, any Customer model that subscribes to RegulationRates must
 * post their available regulation capacity to their applicable
 * TariffSubscriptions at the end of their per-timeslot activation processing.
 * 
 * @author John Collins
 */
@Domain (fields = {"tariffId","response","upRegulationPayment",
                   "downRegulationPayment"})
@XStreamAlias("regulation-rate")
public class RegulationRate extends RateCore
{
  //static private Logger log = Logger.getLogger(RegulationRate.class.getName());

  public enum ResponseTime {SECONDS, MINUTES};

  @XStreamAsAttribute
  private ResponseTime response = ResponseTime.MINUTES;
  @XStreamAsAttribute
  private double upRegulationPayment = 0.0;
  @XStreamAsAttribute
  private double downRegulationPayment = 0.0;

  /**
   * Default constructor only. You create one of these with the
   * constructor and the fluent-style setter methods. The tariff ID gets
   * set when you attach one of these to a TariffSpecification.
   */
  public RegulationRate ()
  {
    super();
  }

  /**
   * Sets the response time
   */
  @StateChange
  public RegulationRate withResponse (ResponseTime time)
  {
    response = time;
    return this;
  }

  public ResponseTime getResponse ()
  {
    return response;
  }

  /**
   * Sets the payment for up-regulation
   */
  @StateChange
  public RegulationRate withUpRegulationPayment (double payment)
  {
    upRegulationPayment = payment;
    return this;
  }

  public double getUpRegulationPayment ()
  {
    return upRegulationPayment;
  }

  /**
   * Sets the payment for down-regulation
   */
  @StateChange
  public RegulationRate withDownRegulationPayment (double payment)
  {
    downRegulationPayment = payment;
    return this;
  }

  public double getDownRegulationPayment ()
  {
    return downRegulationPayment;
  }

  /**
   * Returns true just in case this Rate is internally valid, and valid
   * with respect to the given TariffSpecification. 
   */
  public boolean isValid(TariffSpecification spec)
  {
    // numeric sanity test
    return true;
  }

  @Override
  public String toString ()
  {
    String result = "RegulationRate." + IdGenerator.getString(getId()) + ":";
    return result;
  }
}
