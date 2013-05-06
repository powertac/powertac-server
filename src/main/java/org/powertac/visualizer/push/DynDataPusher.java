package org.powertac.visualizer.push;

import java.math.BigDecimal;

public class DynDataPusher {

	private String name;
	private long millis;
	private double  profit;
	private double  energy;
	private double  profitDelta;
	private double  energyDelta;

	public DynDataPusher(String name, long millis, double  profit, double  energy, double  profitDelta, double  energyDelta) {
		this.name = name;
		this.millis = millis;
		this.profit = profit;
		this.energy = energy;
		this.profitDelta = profitDelta;
		this.energyDelta = energyDelta;
	}

	public String getName() {
		return name;
	}

	public long getMillis() {
		return millis;
	}

	public double getProfit() {
		return profit;
	}

	public double getEnergy() {
		return energy;
	}

	public double getProfitDelta() {
		return profitDelta;
	}

	public double getEnergyDelta() {
		return energyDelta;
	}


	
	
	

}
