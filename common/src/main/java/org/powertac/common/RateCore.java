/*
 * Copyright (c) 2013 by the original author
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

import org.powertac.common.state.StateChange;
import org.powertac.common.state.XStreamStateLoggable;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Common supertype of Rate classes, holds tariff ID for this rate.
 * 
 * @author John Collins
 */
public class RateCore extends XStreamStateLoggable
{
  @XStreamAsAttribute
  private long id;
  @XStreamAsAttribute
  private long tariffId;

  /**
   * Returns the id of this Rate
   */
  public long getId ()
  {
    if (id == 0l) {
      // handle creation from configuration
      id = IdGenerator.createId();
    }
    return id;
  }

  /**
   * Sets the backpointer to the tariff. This is a non-fluent
   * setter, intended to be called by TariffSpecification.
   * It is public to better support state logging
   */
  @StateChange
  public void setTariffId (long id)
  {
    tariffId = id;
  }

  /**
   * Returns the id of the TariffSpecification to which this Rate is
   * attached.
   */
  public long getTariffId ()
  {
    return tariffId;
  }
}
