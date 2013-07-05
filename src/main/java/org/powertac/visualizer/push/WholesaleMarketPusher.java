package org.powertac.visualizer.push;


public class WholesaleMarketPusher {
	private String name;
	private long millis;
	private double  profit;
	private double  energy;
	private double  profitDelta;
	private double  energyDelta;
	

	public WholesaleMarketPusher(String name, long millis, double  profitDelta, double  energyDelta, double  profit, double  energy ) {
		this.name = name;
		this.millis = millis;
		this.profitDelta = profitDelta;
		this.energyDelta = energyDelta;	
		this.energy = energy;
		this.profit = profit;
	}

}
