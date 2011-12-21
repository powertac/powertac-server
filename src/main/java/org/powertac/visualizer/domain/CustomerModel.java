package org.powertac.visualizer.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffTransaction;

/**
 * Represents customer model that belongs to a particular broker.
 * 
 * @author Jurica Babic
 * 
 */
public class CustomerModel {

	Logger log = Logger.getLogger(CustomerModel.class);
	private int customerCount;
	// total cash
	private static double totalCashInflow;
	private static double totalCashOutflow;
	// cash from/to particular broker:
	private double cashInflow;
	private double cashOutflow;
	// total energy in kWh
	private double totalEnergyConsumption;
	private double totalEnergyProduction;
	// energy from/to particular broker;
	private double energyConsumption;
	private double energyProduction;
	// customer info
	private CustomerInfo customerInfo;
	// history
	private List<TariffTransaction> tariffTransactions;

	public CustomerModel(CustomerInfo customerInfo) {
		this.customerInfo = customerInfo;
		tariffTransactions = new ArrayList<TariffTransaction>();
	}

	/**
	 * Adds TarrifTransaction object to history and updates customer model.
	 * 
	 * @param tariffTransaction
	 */
	public void addTariffTransaction(TariffTransaction tariffTransaction) {
		log.info("\n CustomerModel: my tariffTrans: +\n" + tariffTransaction.toString());
		tariffTransactions.add(tariffTransaction);
		update(tariffTransaction);

	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CustomerModel) {
			CustomerModel customerModel = (CustomerModel) obj;
			if (customerModel.getCustomerInfo().getId() == getCustomerInfo().getId()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		// currently: hashcode is an int version of customerInfo ID.
		return (int) getCustomerInfo().getId();
	}

	private void update(TariffTransaction tariffTransaction) {

		// for all txtypes:
		updateCash(tariffTransaction.getCharge());
		updateEnergy(tariffTransaction.getKWh());

		// can't use switch with this enum, so if statements are used:

		// add new customers
		if (tariffTransaction.getTxType().compareTo(TariffTransaction.Type.SIGNUP) == 0) {
			customerCount += tariffTransaction.getCustomerCount();

		}// remove customers that revoke or withdraw
		else if ((tariffTransaction.getTxType().compareTo(TariffTransaction.Type.WITHDRAW) == 0)
				|| (tariffTransaction.getTxType().compareTo(TariffTransaction.Type.WITHDRAW) == 0)) {
			customerCount -= tariffTransaction.getCustomerCount();
		}

		// what does PUBLISH tariffTransaction do?

	}

	private void updateEnergy(double kWh) {
		if (kWh <= 0) { // consumption
			totalEnergyConsumption += (-1) * kWh;
			energyConsumption += (-1) * kWh;
			// to get a positive number (broker "lost" energy to customer)
		} else {
			totalEnergyProduction += kWh;
			energyProduction += kWh;
		}
		log.info("\n energy consumption:" + totalEnergyConsumption + " energy production:" + totalEnergyProduction);
	}

	private void updateCash(double charge) {
		if (charge <= 0) {// from brokers perspective: broker paid to customer
			totalCashInflow += (-1) * charge; // positive cash inflow from
			cashInflow += (-1) * charge; // broker
		} else {
			totalCashOutflow += charge;
			cashOutflow += charge;
		}
		log.info("\n CashInflow:" + totalCashInflow + " CashOutflow:" + totalCashOutflow);
	}

	public int getCustomerCount() {
		return customerCount;
	}

	public double getTotalCashInflow() {
		return totalCashInflow;
	}

	public double getTotalCashOutflow() {
		return totalCashOutflow;
	}

	public double getTotalCashBalance() {
		return totalCashInflow - totalCashOutflow;
	}

	public double getTotalEnergyConsumption() {
		return totalEnergyConsumption;
	}

	public double getTotalEnergyProduction() {
		return totalEnergyProduction;
	}
	public double getTotalEnergyBalance(){
		return totalEnergyProduction-totalEnergyConsumption;
	}

	public CustomerInfo getCustomerInfo() {
		return customerInfo;
	}

	public List<TariffTransaction> getTariffTransactions() {
		return tariffTransactions;
	}

	public double getCashInflow() {
		return cashInflow;
	}

	public double getCashOutflow() {
		return cashOutflow;
	}

	public double getCashBalance() {
		return cashInflow - cashOutflow;
	}

	public double getEnergyConsumption() {
		return energyConsumption;
	}

	public double getEnergyProduction() {
		return energyProduction;
	}

	public double getEnergyBalance() {
		return energyProduction - energyConsumption;
	}
}
