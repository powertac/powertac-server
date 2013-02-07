package org.powertac.visualizer.statistical;

import java.util.concurrent.ConcurrentHashMap;

import org.powertac.visualizer.domain.broker.BrokerModel;

public class FinanceCategory {

	private BrokerModel broker;
	private double profit;
	private ConcurrentHashMap<Integer, FinanceDynamicData> financeDynamicDataMap = new ConcurrentHashMap<Integer, FinanceDynamicData>(
			1500, 0.75f, 1);
	private FinanceDynamicData lastFinanceDynamicData;

	public FinanceCategory(BrokerModel broker) {
		this.broker = broker;
	}

	public ConcurrentHashMap<Integer, FinanceDynamicData> getFinanceDynamicDataMap() {
		return financeDynamicDataMap;
	}

	public BrokerModel getBroker() {
		return broker;
	}

	public double getProfit() {
		return profit;
	}

	public void setProfit(double profit) {
		this.profit = profit;
	}

	public FinanceDynamicData getLastFinanceDynamicData() {
		return lastFinanceDynamicData;
	}

	public void setLastFinanceDynamicData(
			FinanceDynamicData lastFinanceDynamicData) {
		this.lastFinanceDynamicData = lastFinanceDynamicData;
	}
}
