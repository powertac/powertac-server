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
package org.powertac.distributionutility;

import java.util.ArrayList;
import java.util.List;

import org.powertac.common.Broker;
import org.powertac.common.msg.BalancingOrder;

/**
 * Per-broker data holder for DU settlement processors
 * @author John Collins
 */
class ChargeInfo
{
  private Broker broker = null;
  private double netLoadKWh = 0.0;
  private double balanceCharge = 0.0;
  private List<BalancingOrder> balancingOrders = null;

  ChargeInfo (Broker broker, double netLoad)
  {
    this.broker = broker;
    this.netLoadKWh = netLoad;
  }
  
  // -- getters & setters
  Broker getBroker ()
  {
    return broker;
  }
  
  String getBrokerName ()
  {
    return broker.getUsername();
  }
  
  double getNetLoadKWh ()
  {
    return netLoadKWh;
  }
  
  double getBalanceCharge ()
  {
    return balanceCharge;
  }
  
  void setBalanceCharge (double charge)
  {
    balanceCharge = charge;
  }
  
  List<BalancingOrder> getBalancingOrders ()
  {
    return balancingOrders;
  }
  
  void addBalancingOrder (BalancingOrder order)
  {
    if (null == balancingOrders)
      balancingOrders = new ArrayList<BalancingOrder>();
    balancingOrders.add(order);
  }
}
