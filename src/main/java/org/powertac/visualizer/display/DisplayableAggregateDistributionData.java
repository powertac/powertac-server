package org.powertac.visualizer.display;

public class DisplayableAggregateDistributionData {
	private double kWh;
	private double money;
	
	public DisplayableAggregateDistributionData(double kWh, double money) {
		this.kWh = kWh;
		this.money = money;
	}
	public double getkWh() {
		return kWh;
	}
	public double getMoney() {
		return money;
	}

}
