package org.powertac.visualizer.push;

import java.math.BigDecimal;

public class FinancePusher {

	private String name;
	private long millis;
	private double profit;
	private double profitDelta;
	

	public FinancePusher(String name, long millis, double profit, double profitDelta) {
		this.name = name;
		this.millis = millis;
		this.profit = profit;
		this.profitDelta = profitDelta;
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
	
	public double getProfitDelta() {
		return profitDelta;
	}


	
	
	

}
