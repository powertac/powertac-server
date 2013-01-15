package org.powertac.visualizer.domain.broker;

import javax.swing.text.DefaultEditorKit.CutAction;

/**
 * Tariff data for broker's tariffs.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffDynamicData {

	private int customerCount;
	private double profit;
	private double netKwh;

	private double kwhOneTimeslot;
	private double profitOneTimeslot;
	private int customerCountOneTimeslot;

	public TariffDynamicData(double profit, double netKWh, int customerCount) {
		this.profit = profit;
		this.netKwh = netKWh;
		this.customerCount = customerCount;
	}

	public synchronized void addAmounts(double money, double energy,
			int deltaCustomers) {
		kwhOneTimeslot += energy;
		profitOneTimeslot += money;
		customerCountOneTimeslot += deltaCustomers;

		profit += profitOneTimeslot;
		netKwh += kwhOneTimeslot;
		this.customerCount += customerCountOneTimeslot;
	}

	public double getNetKWh() {
		return netKwh;
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

	public int getCustomerCountOneTimeslot() {
		return customerCountOneTimeslot;
	}

	public double getKwhOneTimeslot() {
		return kwhOneTimeslot;
	}

	public double getNetKwh() {
		return netKwh;
	}

	public double getProfitOneTimeslot() {
		return profitOneTimeslot;
	}

}
