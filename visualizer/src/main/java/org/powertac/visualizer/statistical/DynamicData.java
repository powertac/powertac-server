package org.powertac.visualizer.statistical;

/**
 * Data for one timeslot from broker's perspective.
 * 
 * @author Jurica Babic
 * 
 */
public class DynamicData {

	private double energy;
	private double profit;
	private double energyDelta;
	private double profitDelta;
	private int tsIndex;

	public DynamicData(int tsIndex, double energy, double profit) {
		this.energy = energy;
		this.profit = profit;
		this.tsIndex = tsIndex;
	}

	public void update(double energy, double cash) {
		energyDelta += energy;
		profitDelta += cash;
	}

	public double getEnergy() {
		return energy;
	}
	
	public double getEnergyDelta() {
		return energyDelta;
	}

	public double getProfit() {
		return profit;
	}

	public double getProfitDelta() {
		return profitDelta;
	}

	public int getTsIndex() {
		return tsIndex;
	}
	
	public void setEnergy(double energy) {
		this.energy = energy;
	}
	
	public void setProfit(double profit) {
		this.profit = profit;
	}

}
