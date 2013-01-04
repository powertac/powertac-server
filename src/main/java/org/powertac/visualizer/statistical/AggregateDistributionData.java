package org.powertac.visualizer.statistical;


/**
 * Holds the aggregate data for transport of energy for one broker.
 * 
 * @author Jurica Babic
 * 
 */
public class AggregateDistributionData {
	private double netKWh;
	private double profit;

	public void addValues(double energy, double charge) {
		netKWh += energy;
		profit += charge; 
	}

	public double getNetKWh() {
		return netKWh;
	}
	public double getProfit() {
		return profit;
	}

}
