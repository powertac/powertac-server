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

import java.util.HashMap;

import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

/**
 * A broker instance represents a competition participant.
 * Broker instances are not communicated to other brokers; only usernames are 
 * considered public information and get communicated. Every entity that needs
 * to trade in the wholesale market or post TariffSpecifications must be a
 * broker.
 * <p>
 * Brokers may be local or non-local (remote), and they may be wholesale or
 * non-wholesale (retail) brokers. Remote brokers receive messages through
 * JMS, while local brokers are assumed to reside in the server's process space
 * where they receive messages by calls to their receiveMessage() methods. Local
 * brokers must override receiveMessage() to see these messages, otherwise they
 * will be dropped on the floor. Local brokers can send messages by calling
 * BrokerProxy.routeMessage();</p>
 * <p>
 * Wholesale brokers are not permitted to offer tariffs, but may trade in the
 * wholesale market, and they are not included in the balancing process.</p>
 *
 * @author Carsten Block, David Dauer, John Collins
 */
@Domain
public class Broker 
{
  private long id = IdGenerator.createId();

  /** the broker's login or user name and password */
  private String username;
  private String password;
  
  /** the broker's jms key. Must be copied over from the BrokerAccept message */
  private String key;
  private String queueName = null;
  
  private boolean enabled;

  /** If true, the broker is local to the server and does not receive messages  */
  private boolean local = false;

  /** ID prefix for remote brokers */
  private int idPrefix = 0;

  /** If true, broker is a wholesale market participant, but not a "real" broker */
  private boolean wholesale = false;
  
  /** Broker's current cash position */
  private double cash = 0.0;
  
  private HashMap<Integer, MarketPosition> mktPositions;

  //def testProxy = null // redirect incoming messages for testing

  /**
   * Constructor for username only.
   */
  public Broker (String username)
  {
    super();
    this.username = username;
    mktPositions = new HashMap<Integer, MarketPosition>();
  }
  
  /**
   * Constructor to specify non-standard local/wholesale flags.
   */
  public Broker (String username, boolean local, boolean wholesale)
  {
    super();
    this.username = username;
    mktPositions = new HashMap<Integer, MarketPosition>();
    this.local = local;
    this.wholesale = wholesale;
  }

  /**
   * Returns the unique ID for this broker
   */
  public long getId ()
  {
    return id;
  }
  
  /**
   * Sets the jms key for a remote broker.
   */
  public void setKey (String key)
  {
    this.key = key;
  }
  
  /**
   * Returns the jms ID for this broker.
   */
  public String getKey() 
  {
    return key;
  }

  /**
   * Sets the ID prefix for this broker. Intended to be called by competition
   * control when a remote broker logs in.
   */
  public void setIdPrefix (int prefix)
  {
    idPrefix = prefix;
  }

  /**
   * Returns the ID prefix for this broker. Used in the server to validate
   * incoming messages. Should be non-zero only for remote brokers.
   */
  public int getIdPrefix ()
  {
    return idPrefix;
  }

  /**
   * Updates broker's cash position. Note that this operation does not generate
   * a state log entry. To see the broker's cash balance in the state log,
   * you have to create a new CashPosition.
   */
  public void updateCash (double depositAmount)
  {
    cash += depositAmount;
  }
  
  /**
   * Returns broker's cash balance.
   */
  public double getCashBalance ()
  {
    return cash;
  }

  /**
   * Associates a MarketPosition with a given Timeslot. 
   */
  @StateChange
  public Broker addMarketPosition (MarketPosition posn, int slot)
  {
    mktPositions.put(slot, posn);
    return this;
  }
  
  // backward compatibility
  @Deprecated
  public Broker addMarketPosition (MarketPosition posn, Timeslot slot)
  {
    return addMarketPosition(posn, slot.getSerialNumber());
  }
  
  /**
   * Returns the MarketPosition associated with the given Timeslot.
   * Result is guaranteed to be non-null.
   */
  public MarketPosition findMarketPositionByTimeslot (int slot)
  {
    MarketPosition posn = mktPositions.get(slot);
    if (null == posn) {
      posn = new MarketPosition(this, slot, 0.0);
      mktPositions.put(slot, posn);
    }
    return posn;
  }
  
  // backward compatibility
  @Deprecated
  public MarketPosition findMarketPositionByTimeslot (Timeslot slot)
  {
    return findMarketPositionByTimeslot(slot.getSerialNumber());
  }

  /**
   * Returns the username for this Broker.
   */
  public String getUsername ()
  {
    return username;
  }

  public String getPassword ()
  {
    return password;
  }
  
  public void setPassword (String newPassword)
  {
    password = newPassword;
  }

  /**
   * True just in case either the broker is logged in, or is a local wholesale
   * broker.
   */
  public boolean isEnabled ()
  {
    return (enabled || (isLocal() && isWholesale()));
  }
  
  /**
   * Enables this Broker. Of course, calling this method on a remote broker
   * will have no effect; it must be called on the Broker instance in the
   * server.
   */
  public void setEnabled(boolean enabled) 
  {
    this.enabled = enabled;
  }

  /**
   * True for a Broker that is local to the server. Affects message routing.
   */
  public boolean isLocal ()
  {
    return local;
  }
  
  /**
   * Allows subclasses to set themselves as local brokers. Local brokers
   * must subclass this class, and implement receiveMessage() to receive
   * messages from the server. They send messages by calling
   * BrokerProxy.routeMessage(). 
   */
  @StateChange
  public void setLocal (boolean value)
  {
    local = value;
  }

  /**
   * True for a broker that operates on the wholeside of the wholesale market.
   */
  public boolean isWholesale ()
  {
    return wholesale;
  }
  
  /** Allows subclasses to make themselves wholesale brokers */
  @StateChange
  public void setWholesale (boolean value)
  {
    wholesale = value;
  }

  @Override
  public String toString() 
  {
    return username;
  }

  /**
   * Sets the broker's queue name.
   */
  public void setQueueName (String queueName)
  {
    this.queueName = queueName;
  }
  
  /**
   * Returns the broker's queue name if it's been set, otherwise the default
   * queue name.
   */
  public String toQueueName() 
  {
    if (null != queueName)
      return (queueName);
    else
      return (getUsername());
  }

  /**
   * Default implementation does nothing.
   * Broker subclasses implemented within the server can override this method 
   * to receive messages from BrokerProxy
   */
  public void receiveMessage(Object object) 
  {
  }
}
