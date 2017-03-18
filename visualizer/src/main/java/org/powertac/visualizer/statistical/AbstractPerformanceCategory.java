package org.powertac.visualizer.statistical;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.powertac.visualizer.domain.broker.BrokerModel;

/**
 * This abstract class is used as a template to build more detailed performance
 * category for a broker.
 * 
 * @author Jurica Babic
 * 
 */
public abstract class AbstractPerformanceCategory
{
  private BrokerModel broker;
  int grade = 0;
  double energy;
  double profit;
  DynamicData lastDynamicData = new DynamicData(0, 0, 0);
  private ConcurrentHashMap<Integer, DynamicData> dynamicDataMap =
    new ConcurrentHashMap<Integer, DynamicData>(1500, 0.75f, 1);

  public AbstractPerformanceCategory ()
  {
    dynamicDataMap.put(0, lastDynamicData);
  }

  public ConcurrentHashMap<Integer, DynamicData> getDynamicDataMap ()
  {
    return dynamicDataMap;
  }

  /**
   * @return kWh
   */
  public double getEnergy ()
  {
    return energy;
  }

  public double getProfit ()
  {
    return profit;
  }

  public void update (int tsIndex, double energy, double cash)
  {
    if (!dynamicDataMap.containsKey(tsIndex)) {
      lastDynamicData = new DynamicData(tsIndex, this.energy, this.profit);
      dynamicDataMap.put(tsIndex, lastDynamicData);

    }
    dynamicDataMap.get(tsIndex).update(energy, cash);
    this.energy += energy;
    this.profit += cash;

  }

  public AbstractPerformanceCategory (BrokerModel broker)
  {
    this.broker = broker;
  }

  public double getGrade ()
  {
    return grade;
  }

  public void setGrade (int d)
  {
    this.grade = d;
  }

  public BrokerModel getBroker ()
  {
    return broker;
  }

  public DynamicData getLastDynamicData ()
  {
    return lastDynamicData;
  }

  public void setLastDynamicData (DynamicData lastDynamicData)
  {
    this.lastDynamicData = lastDynamicData;
  }

}
