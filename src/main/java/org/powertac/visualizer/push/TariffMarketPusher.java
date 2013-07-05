package org.powertac.visualizer.push;


public class TariffMarketPusher {

	private String name;
	private long millis;
	private double  profit;
	private double  energy;
	private int customerCount;
	private double  profitDelta;
	private double  energyDelta;
	private int customerCountDelta;

	public TariffMarketPusher(String name, long millis, double  profit, double  energy,
			int customerCount, double  profitDelta, double  energyDelta,
			int customerCountDelta) {
		this.name = name;
		this.millis = millis;
		this.profit = profit;
		this.energy = energy;
		this.customerCount = customerCount;
		this.profitDelta = profitDelta;
		this.energyDelta = energyDelta;
		this.customerCountDelta = customerCountDelta;
	}

	public String getName() {
		return name;
	}

	public long getMillis() {
		return millis;
	}

	public double  getProfit() {
		return profit;
	}

	public double  getEnergy() {
		return energy;
	}

	public int getCustomerCount() {
		return customerCount;
	}

	public double  getProfitDelta() {
		return profitDelta;
	}

	public double  getEnergyDelta() {
		return energyDelta;
	}

	public int getCustomerCountDelta() {
		return customerCountDelta;
	}

}
