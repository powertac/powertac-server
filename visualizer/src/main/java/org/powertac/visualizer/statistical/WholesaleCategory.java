package org.powertac.visualizer.statistical;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.visualizer.domain.broker.BrokerModel;

/**
 * Holds broker's transactions made in current timeslot for available future
 * timeslots (24 of them).
 * */
public class WholesaleCategory extends AbstractPerformanceCategory
{

  public WholesaleCategory (BrokerModel broker)
  {
    super(broker);
    // TODO Auto-generated constructor stub
  }

  /**Total amount of energy bought in wholesale market*/
  private double totalEnergyBought;
  /**Total amount of energy sold in wholesale market*/
  private double totalEnergySold;
  /**Total amount of money payed for energy in wholesale market*/
  private double totalCostFromBuying;
  /**Total amount of money received for selling energy in wholesale market*/
  private double totalRevenueFromSelling;

  private ConcurrentHashMap<Integer, List<MarketTransaction>> marketTxs =
    new ConcurrentHashMap<Integer, List<MarketTransaction>>(24, 0.75f, 1);

  private ConcurrentHashMap<Integer, List<Order>> orders =
    new ConcurrentHashMap<Integer, List<Order>>(24, 0.75f, 1);

  public ConcurrentHashMap<Integer, List<MarketTransaction>> getMarketTxs ()
  {
    return marketTxs;
  }

  public ConcurrentHashMap<Integer, List<Order>> getOrders ()
  {
    return orders;
  }

  @Override
  public void update (int tsIndex, double energy, double cash)
  {
    if (!this.getDynamicDataMap().containsKey(tsIndex)) {
      this.setLastDynamicData(new DynamicData(tsIndex, 0, 0));
      this.getDynamicDataMap().put(tsIndex, this.getLastDynamicData());

    }
    this.getDynamicDataMap().get(tsIndex).update(energy, cash);
  }

  public void updateAccounts (int tsIndex)
  {

    if (getDynamicDataMap().containsKey(tsIndex)) {
      getDynamicDataMap().get(tsIndex).setProfit(profit);
      getDynamicDataMap().get(tsIndex).setEnergy(energy);
      profit += getDynamicDataMap().get(tsIndex).getProfitDelta();
      energy += getDynamicDataMap().get(tsIndex).getEnergyDelta();
    }

  }

  public double getTotalEnergyBought()
  {
    return totalEnergyBought;
  }

  public void addEnergyBought (double energy)
  {
    this.totalEnergyBought += energy;
  }

  public double getTotalEnergySold ()
  {
    return totalEnergySold;
  }

  public void addEnergySold (double energy)
  {
    this.totalEnergySold += energy;
  }

  public double getTotalCostFromBuying ()
  {
    return totalCostFromBuying;
  }

  public void addCostFromBuying (double cost)
  {
    this.totalCostFromBuying += cost;
  }

  public double getTotalRevenueFromSelling ()
  {
    return totalRevenueFromSelling;
  }

  public void addRevenueFromSelling (double revenue)
  {
    this.totalRevenueFromSelling += revenue;
  }
  
  
 
}
