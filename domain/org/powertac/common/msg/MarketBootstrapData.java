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

import org.powertac.common.IdGenerator;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.*;

/**
 * This message encapsulates net power usage by timeslot for a customer
 * instance over the bootstrap period.
 * @author Anthony Chrysopoulos, John Collins
 */
@Domain
@XStreamAlias("market-bootstrap-data")
public class MarketBootstrapData
{
  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  
  private double[] mwh;
  private double[] marketPrice;

  public MarketBootstrapData (double[] mwh, double[] price)
  {
    super();
    this.mwh = mwh;
    this.marketPrice = price;
  }

  public long getId ()
  {
    return id;
  }

  public double[] getMwh ()
  {
    return mwh;
  }
  
  public double[] getMarketPrice ()
  {
    return marketPrice;
  }
}