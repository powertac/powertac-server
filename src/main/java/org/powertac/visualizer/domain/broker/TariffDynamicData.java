package org.powertac.visualizer.domain.broker;

/**
 * Tariff data for broker's tariffs.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffDynamicData {

	private int customerCount;
	private double profit;
	private double netKWh;

	public TariffDynamicData(double profit, double netKWh, int customerCount) {
		this.profit = profit;
		this.netKWh = netKWh;
		this.customerCount = customerCount;
	}

	public synchronized void addAmounts(double money, double energy, int deltaCustomers) {
		profit += money;
		netKWh += energy;
		this.customerCount += deltaCustomers;
	}

	public double getNetKWh() {
		return netKWh;
	}

	public double getProfit() {
		return profit;
	}

	/**
	 * @return total number of broker's subscribed customers
	 */
	public int getCustomerCount() {
		return customerCount;
	}
	

}
