package org.powertac.visualizer.domain.broker;

import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffSpecification;

/**
 * Tracks tariff transactions for one tariff and one customer model. The object
 * belongs to a particular broker.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffCustomerStats {
	private CustomerInfo customerInfo;
	private TariffSpecification tariffSpec;
	private double profit;
	private double netKWh;

	public TariffCustomerStats(CustomerInfo customer, TariffSpecification spec) {
		customerInfo = customer;
		tariffSpec = spec;
	}

	public CustomerInfo getCustomerInfo() {
		return customerInfo;
	}

	public double getNetKWh() {
		return netKWh;
	}

	public double getProfit() {
		return profit;
	}

	public TariffSpecification getTariffSpec() {
		return tariffSpec;
	}

	public void addAmounts(double charge, double kWh) {
		profit += charge;
		netKWh += kWh;

	}
	
	public String toString(){
		return "" + customerInfo.getName() + " - " + customerInfo.getPopulation();
	}
	

}
