package org.powertac.visualizer.domain.broker;

import org.powertac.common.CustomerInfo;

/**
 * Tracks broker's tariff transactions for one customer model.
 * 
 * @author Jurica Babic
 * 
 */
public class CustomerTariffData {
	private BrokerModel broker;
	private CustomerInfo customer;
	private double profit;
	private double netKWh;

	public CustomerTariffData(BrokerModel broker, CustomerInfo customer) {
		this.broker = broker;
		this.customer = customer;
	}

	public void addAmounts(double money, double energy) {
		profit += money;
		netKWh += energy;
	}

	public double getNetKWh() {
		return netKWh;
	}

	public double getProfit() {
		return profit;
	}
	
	public BrokerModel getBroker() {
		return broker;
	}
	
	public CustomerInfo getCustomer() {
		return customer;
	}
}
