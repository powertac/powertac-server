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

	public void update(double energy, double cash) {
		this.energy += energy;
		profit += cash;
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
