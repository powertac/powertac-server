package org.powertac.visualizer.statistical;

import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffSpecification;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.CustomerTariffData;
import org.powertac.visualizer.domain.broker.TariffData;
import org.powertac.visualizer.domain.broker.TariffDynamicData;

/**
 * This performance category holds tariff related info for one broker.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffCategory extends AbstractPerformanceCategory
  implements PerformanceCategory
{

  /** Total sold energy in tariff market */
  private double totalSoldEnergy;
  /** Total bought energy in tariff market */
  private double totalBoughtEnergy;
  /**
   * Total amount of money used in tariff market = sum of absolute charge from
   * TariffTransactions.
   */
  private double totalMoneyFlow;
  /** Number of customers who are/were broker clients */
  private int gainedCustomers;
  /** Number of customers who left broker */
  private int lostCustomers;
  /** Number of times consumers used energy */
  private long consumptionConsumers;
  /** Amount of money received from selling energy in tariff market */
  private double totalMoneyFromSold;

  private int customerCount;
  private TariffDynamicData lastTariffDynamicData;

  // key: postedTime
  private ConcurrentHashMap<Integer, TariffDynamicData> tariffDynamicDataMap;
  private ConcurrentHashMap<CustomerInfo, CustomerTariffData> customerTariffData;
  private ConcurrentHashMap<TariffSpecification, TariffData> tariffData;

  public TariffCategory (BrokerModel broker)
  {
    super(broker);
    lastTariffDynamicData = new TariffDynamicData(0, 0, 0, 0);
    tariffDynamicDataMap =
      new ConcurrentHashMap<Integer, TariffDynamicData>(1000, 0.75f, 1);
    customerTariffData =
      new ConcurrentHashMap<CustomerInfo, CustomerTariffData>(20, 0.75f, 1);
    tariffData =
      new ConcurrentHashMap<TariffSpecification, TariffData>(20, 0.75f, 1);
  }

  public void processTariffSpecification (TariffSpecification ts)
  {
    tariffData.putIfAbsent(ts, new TariffData(ts, this.getBroker()));
  }

  public int getCustomerCount ()
  {
    return customerCount;
  }

  public ConcurrentHashMap<Integer, TariffDynamicData>
    getTariffDynamicDataMap ()
  {
    return tariffDynamicDataMap;
  }

  /**
   * @return Info about broker's transactions related to one customer model.
   */
  public ConcurrentHashMap<CustomerInfo, CustomerTariffData>
    getCustomerTariffData ()
  {
    return customerTariffData;
  }

  /**
   * @return Info about broker's tariff related to one customer model.
   */
  public ConcurrentHashMap<TariffSpecification, TariffData> getTariffData ()
  {
    return tariffData;
  }

  public void
    update (int tsIndex, double energy, double cash, int customerDelta)
  {
    customerCount += customerDelta;
    tariffDynamicDataMap.get(tsIndex).update(cash, energy, customerDelta);
    update(tsIndex, energy, cash);
  }

  public void addTariffDynamicData (TariffDynamicData tdd)
  {
    lastTariffDynamicData = tdd;
    tariffDynamicDataMap.put(tdd.getDynamicData().getTsIndex(), tdd);

  }

  public TariffDynamicData getLastTariffDynamicData ()
  {
    return lastTariffDynamicData;
  }

  public double getTotalMoneyFlow ()
  {
    return totalMoneyFlow;
  }

  public void addCharge (double charge)
  {
    this.totalMoneyFlow += charge;
  }

  public int getGainedCustomers ()
  {
    return gainedCustomers;
  }

  public void addCustomers (int gainedCustomers)
  {
    this.gainedCustomers = +gainedCustomers;
  }

  public int getLostCustomers ()
  {
    return lostCustomers;
  }

  public void addLostCustomers (int lostCustomers)
  {
    this.lostCustomers += lostCustomers;
  }

  public long getConsumptionConsumers ()
  {
    return consumptionConsumers;
  }

  public void addConsumptionConsumers (long consumptionConsumers)
  {
    this.consumptionConsumers += consumptionConsumers;
  }

  public double getTotalSoldEnergy ()
  {
    return totalSoldEnergy;
  }

  public void addSoldEnergy (double soldEnergy)
  {
    this.totalSoldEnergy += soldEnergy;
  }

  public double getTotalBoughtEnergy ()
  {
    return totalBoughtEnergy;
  }

  public void addBoughtEnergy (double boughtEnergy)
  {
    this.totalBoughtEnergy += boughtEnergy;
  }

  public double getTotalMoneyFromSold ()
  {
    return totalMoneyFromSold;
  }

  public void addMoneyFromSold (double moneyFromSold)
  {
    this.totalMoneyFromSold += moneyFromSold;
  }
}
