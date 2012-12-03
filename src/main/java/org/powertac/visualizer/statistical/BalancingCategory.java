package org.powertac.visualizer.statistical;

import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.statistical.GradingSystem;

/**
 * This is a performance category related to balancing transactions of a broker.
 * 
 * @author Jurica Babic
 * 
 */
public class BalancingCategory extends AbstractPerformanceCategory implements
		PerformanceCategory {

	private ConcurrentHashMap<Double, BalancingData> balancingDataMap = new ConcurrentHashMap<Double, BalancingData>(
			2000, 0.75f, 1);
	private AggregateBalancingData aggregateBalancingData = new AggregateBalancingData();
	private BalancingData lastBalancingData = new BalancingData(0, 0, 0);
		
	public BalancingCategory(BrokerModel broker) {
		super(broker);
	}

	public void addBalancingData(BalancingData data) {
		balancingDataMap.put(data.getTimestamp(), data);
		lastBalancingData = data;
		aggregateBalancingData.processBalancingData(lastBalancingData);
	}
	
	public AggregateBalancingData getAggregateBalancingData() {
		return aggregateBalancingData;
	}
	
	public BalancingData getLastBalancingData() {
		return lastBalancingData;
	}
	
	public ConcurrentHashMap<Double, BalancingData> getBalancingDataMap() {
		return balancingDataMap;
	}

	

}
