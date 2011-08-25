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

package org.powertac.common;

//import org.powertac.common.transformer.BrokerConverter
//import org.codehaus.groovy.grails.commons.ApplicationHolder
import java.math.BigDecimal;

import com.thoughtworks.xstream.annotations.*;

/**
 * A {@code CashPosition} domain instance represents the current state of
 * a broker's cash account. An updated CashPosition is sent to brokers
 * during each timeslot. This is not public information.
 *
 * @author Carsten Block, David Dauer
 * @version 1.1 - 02/27/2011
 */
@XStreamAlias("cash")
public class CashPosition //implements Serializable 
{
  /** The broker who owns this cash account  */
  // JEC - back-reference not needed?
  //@XStreamConverter(BrokerConverter)
  //Broker broker;

  /** The new running total for the broker's cash account  */
  @XStreamAsAttribute
  BigDecimal balance = new BigDecimal(0.0);

  public CashPosition (BigDecimal initialBalance)
  {
    balance = initialBalance;
  }
  
  public CashPosition (double initialBalance)
  {
    balance = new BigDecimal(initialBalance);
  }
  
  public String toString() {
    return balance.toString();
  }
  
  /**
   * Updates the balance in this account by the specified amount,
   * returns the resulting balance. A withdrawal is negative,
   * deposit is positive.
   */
  public BigDecimal deposit (BigDecimal amount)
  {
    balance = balance.add(amount);
    return balance;
  }
}
