package org.powertac.visualizer.statistical;

import org.powertac.visualizer.domain.broker.BrokerModel;

/**
 * This is a performance category related to balancing transactions of a broker.
 * 
 * @author Jurica Babic
 * 
 */
public class BalancingCategory extends AbstractPerformanceCategory
  implements PerformanceCategory
{

  
  public BalancingCategory (BrokerModel broker)
  {
    super(broker);
  }


}
