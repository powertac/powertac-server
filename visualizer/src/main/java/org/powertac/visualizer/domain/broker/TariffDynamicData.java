package org.powertac.visualizer.domain.broker;

import org.powertac.visualizer.statistical.DynamicData;

/**
 * Tariff data for broker's tariffs.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffDynamicData {

	private DynamicData dynamicData;
	private int customerCount;
	private int customerCountDelta;

	public TariffDynamicData(int tsIndex, double profit, double energy,
			int customerCount) {
		dynamicData = new DynamicData(tsIndex, energy, profit);
		this.customerCount = customerCount;
	}

	public synchronized void update(double money, double energy,
			int deltaCustomers) {
		dynamicData.update(energy, money);
		customerCount += deltaCustomers;
		customerCountDelta += deltaCustomers;
	}

	public DynamicData getDynamicData() {
		return dynamicData;
	}

	public int getCustomerCount() {
		return customerCount;
	}

	public int getCustomerCountDelta() {
		return customerCountDelta;
	}

}
