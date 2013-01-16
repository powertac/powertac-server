package org.powertac.visualizer.display;

import java.io.Serializable;

import org.powertac.visualizer.statistical.WholesaleCategory;

public class DisplayableWholesaleCategory extends
		AbstractDisplayablePerformanceCategory implements Serializable {

	private int noOrders;
	private int noMarketTx;

	public DisplayableWholesaleCategory(WholesaleCategory cat) {
		super(cat.getGrade());
		noOrders += cat.getNoOrders();
		noMarketTx += cat.getNoMarketTransactions();
	}
	
	public int getNoMarketTx() {
		return noMarketTx;
	}
	public int getNoOrders() {
		return noOrders;
	}

}
