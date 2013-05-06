package org.powertac.visualizer.push;

public class StatisticsPusher {
	
	private long millis;
	private double averagePrice;
	private double energy;

	public StatisticsPusher(long millis, double price, double energy) {
		
		this.millis = millis;
		this.averagePrice = price;
		this.energy = energy;
		
	}

	public double getEnergy() {
		return energy;
	}

	public void setEnergy(double energy) {
		this.energy = energy;
	}

	public long getMillis() {
		return millis;
	}

	public void setMillis(long millis) {
		this.millis = millis;
	}

	public double getAveragePrice() {
		return averagePrice;
	}

	public void setAveragePrice(double averagePrice) {
		this.averagePrice = averagePrice;
	}

}
