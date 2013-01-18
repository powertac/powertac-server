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
public abstract class AbstractPerformanceCategory {
	BrokerModel broker;
	double grade = 0;
	private double energy;
	private double profit;

	private ConcurrentHashMap<Integer, DynamicData> dynamicDataMap = new ConcurrentHashMap<Integer, DynamicData>(
			1500, 0.75f, 1);

	public ConcurrentHashMap<Integer, DynamicData> getDynamicDataMap() {
		return dynamicDataMap;
	}

	/**
	 * @return kWh
	 */
	public double getEnergy() {
		return energy;
	}

	public double getProfit() {
		return profit;
	}

	public void update(int tsIndex, double energy, double cash) {
		dynamicDataMap.putIfAbsent(tsIndex, new DynamicData(tsIndex,
				this.energy, this.profit));
		dynamicDataMap.get(tsIndex).update(energy, cash);
		this.energy += energy;
		this.profit += cash;

		if (tsIndex == 3) {
			System.out.println("Tx: " + energy + "MWh " + cash + "euros");
			System.out.println("TotalTS3: "
					+ dynamicDataMap.get(tsIndex).getEnergyDelta() + "MWh "
					+ dynamicDataMap.get(tsIndex).getProfitDelta() + "euros");
			System.out.println("TotalALL: "
					+ dynamicDataMap.get(tsIndex).getEnergy() + "MWh "
					+ dynamicDataMap.get(tsIndex).getProfit() + "euros\n");
		}

	}

	public AbstractPerformanceCategory(BrokerModel broker) {
		this.broker = broker;
	}

	public double getGrade() {
		return grade;
	}

	public void setGrade(double d) {
		this.grade = d;
	}

}
