package org.powertac.visualizer.domain.customer;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffTransaction;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.visualizer.domain.broker.CustomerModel;
import org.powertac.visualizer.interfaces.TimeslotModelUpdate;

/**
 * Holds data for a particular customer type.
 * 
 * @author Jurica Babic
 * 
 */
public class Customer implements TimeslotModelUpdate {
	private Logger log = Logger.getLogger(Customer.class);
	private CustomerModel customerModel;

	private CustomerInfo customerInfo;

	private CustomerBootstrapData bootstrapData;

	private double currentKWhProduced;
	private double currentKWhConsumed;
	private double currentInflowCharge;
	private double currentOutflowCharge;


	public Customer(CustomerInfo customerInfo) {
		customerModel = new CustomerModel(customerInfo);
		this.customerInfo = customerInfo;
		// charts = new CustomerCharts();
	}

	public CustomerModel getCustomerModel() {
		return customerModel;
	}

	public void addCustomerBootstrapData(CustomerBootstrapData data, Competition competition) {
		bootstrapData = data;
		

	}

	public void addTariffTransaction(TariffTransaction tx) {
		double kWh = tx.getKWh();
		double charge = (-1.0) * tx.getCharge();
		if (kWh < 0) {
			currentKWhConsumed += kWh;
		} else {
			currentKWhProduced += kWh;
		}
		if (charge > 0) {
			currentInflowCharge += charge;
		} else {
			currentOutflowCharge += charge;
		}

		customerModel.addTariffTransaction(tx);
	}

	public void update(int timeslotIndex, Instant postedTime) {
		// reset variables for the next timeslot;
		currentInflowCharge = 0;
		currentOutflowCharge = 0;
		currentKWhConsumed = 0;
		currentKWhProduced = 0;

	}

	public CustomerInfo getCustomerInfo() {
		return customerInfo;
	}

  @Override
  public String toString ()
  {
    // TODO Auto-generated method stub
    return this.getCustomerInfo().getName();
  }

	

}
