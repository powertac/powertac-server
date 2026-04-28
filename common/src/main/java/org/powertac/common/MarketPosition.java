/*
 * Copyright 2009-2013 the original author or authors.
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

import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.ChainedConstructor;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.xml.BrokerConverter;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * A {@code MarketPosition} domain instance represents the current position of a
 * single broker for wholesale power in a given timeslot. The evolution of this
 * position over time is represented by the sequence of MarketTransaction instances
 * for this broker and timeslot. These are created by the AccountingService and
 * communicated to individual brokers after the market clears in each timeslot.
 *
 * @author Carsten Block, David Dauer, John Collins
 */
@Domain(fields = {"broker", "timeslot", "balance"})
@XStreamAlias("market-posn")
public class MarketPosition //implements Serializable 
{
  @XStreamAsAttribute
  protected long id = IdGenerator.createId();
  
  /** the broker this position update belongs to */
  @XStreamConverter(BrokerConverter.class)
  private Broker broker;
  
  /** the timeslot this position belongs to */
  @XStreamAsAttribute
  private int timeslot;
  
  /** The running total position in mWh the broker owns (> 0) / owes (< 0)
   * of the specified product in the specified timeslot */
  @XStreamAlias("bal")
  @XStreamAsAttribute
  double overallBalance = 0.0;

  public MarketPosition (Broker broker, int timeslot,
                         double balance)
  {
    super();
    this.broker = broker;
    this.timeslot = timeslot;
    this.overallBalance = balance;
  }

  @ChainedConstructor
  public MarketPosition (Broker broker, Timeslot timeslot,
                         double balance)
  {
    this(broker, timeslot.getSerialNumber(), balance);
  }
  
  public long getId()
  {
    return id;
  }
  
  public Broker getBroker ()
  {
    return broker;
  }

  public int getTimeslotIndex ()
  {
    return timeslot;
  }
  
  public Timeslot getTimeslot ()
  {
    return getTimeslotRepo().findBySerialNumber(timeslot);
  }

  public double getOverallBalance ()
  {
    return overallBalance;
  }

  @Override
  public String toString() {
    return ("MktPosn-" + broker.getId() + "-" +
            timeslot + "-" + overallBalance);
  }
  
  /**
   * Adds a quantity to the current balance. Positive numbers signify
   * purchased power, negative numbers signify sold power. Returns the
   * resulting total balance
   */
  @StateChange
  public double updateBalance (double mWh)
  {
    overallBalance += mWh;
    return overallBalance;
  }
  
  // access to TimeslotRepo
  private static TimeslotRepo timeslotRepo;
  
  private static TimeslotRepo getTimeslotRepo()
  {
    if (null == timeslotRepo) {
      timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
    }
    return timeslotRepo;
  }
}
