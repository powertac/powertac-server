package org.powertac.visualizer.statistical;

import org.powertac.visualizer.domain.broker.BrokerModel;

/**
 * Holds the aggregate data for transport of energy for one broker.
 * 
 * @author Jurica Babic
 * 
 */
public class AggregateDistributionData {
	private double kWh;
	private double money;

	public void addValues(double energy, double charge) {
		kWh += energy;
		money += charge; 
	}

	public double getkWh() {
		return kWh;
	}

	public double getMoney() {
		return money;
	}

}
