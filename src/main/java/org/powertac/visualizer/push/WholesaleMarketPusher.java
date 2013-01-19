package org.powertac.visualizer.push;

import java.util.ArrayList;

public class WholesaleMarketPusher {
	private String name;
	private long millis;
	private double profit;
	private double energy;
	private double profitDelta;
	private double energyDelta;
	private ArrayList<Object> newMarketTxs;

	public WholesaleMarketPusher(String name, long millis, double profitDelta, double energyDelta, ArrayList<Object> newMarketTxs, double profit, double energy ) {
		this.name = name;
		this.millis = millis;
		this.profitDelta = profitDelta;
		this.energyDelta = energyDelta;
		this.newMarketTxs = newMarketTxs;
		this.energy = energy;
		this.profit = profit;
	}

}
