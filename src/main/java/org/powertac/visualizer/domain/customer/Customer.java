package org.powertac.visualizer.domain.customer;

import org.apache.log4j.Logger;
import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffTransaction;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.visualizer.domain.broker.CustomerModel;
import org.powertac.visualizer.interfaces.TimeslotModelUpdate;
import org.powertac.visualizer.json.CustomerJSON;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;

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

	private CustomerJSON customerJson;
	private CustomerBootstrapData bootstrapData;

	private double currentKWhProduced;
	private double currentKWhConsumed;
	private double currentInflowCharge;
	private double currentOutflowCharge;

	public Customer(CustomerInfo customerInfo) {
		customerJson = new CustomerJSON();
		customerModel = new CustomerModel(customerInfo);
		this.customerInfo = customerInfo;
	}

	public CustomerModel getCustomerModel() {
		return customerModel;
	}

	public CustomerJSON getCustomerJson() {
		return customerJson;
	}

	public void addCustomerBootstrapData(CustomerBootstrapData data) {
		bootstrapData = data;
		buildBootstrapDataJSON();

	}

	public void addTariffTransaction(TariffTransaction transaction) {
		double kWh = transaction.getKWh();
		double charge = (-1.0) * transaction.getCharge();
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

		customerModel.addTariffTransaction(transaction);
	}

	private void buildBootstrapDataJSON() {
		try {
			JSONArray array = new JSONArray(bootstrapData.getNetUsage());
			customerJson.setBootstrapLineChartData(array);
		} catch (JSONException e) {
			log.warn("Unable to create JSON Array from bootstrap data");
		}

	}

	public void update(int timeslotIndex) {
		try {
			JSONArray chargeTotal = new JSONArray().put(timeslotIndex).put(currentInflowCharge+currentOutflowCharge);
			customerJson.getTotalChargeLineChartData().put(chargeTotal);
			JSONArray kWhTotal = new JSONArray().put(timeslotIndex).put(currentKWhConsumed+currentKWhProduced);
			customerJson.getTotalkWhLineChartData().put(kWhTotal);
			
			JSONArray chargeInflow = new JSONArray().put(timeslotIndex).put(currentInflowCharge);
			customerJson.getInflowChargeLineChartData().put(chargeInflow);
			JSONArray chargeOutflow = new JSONArray().put(timeslotIndex).put(currentOutflowCharge);
			customerJson.getOutflowChargeLineChartData().put(chargeOutflow);
			
			JSONArray kWhProd = new JSONArray().put(timeslotIndex).put(currentKWhProduced);
			customerJson.getProductionKWhLineChartData().put(kWhProd);
			
			JSONArray kWhCons = new JSONArray().put(timeslotIndex).put(currentKWhConsumed);
			customerJson.getConsumptionKWhLineChartData().put(kWhCons);
		

			

		} catch (JSONException e) {
			log.warn("Problem with customers JSON object update!");
		}

		// reset variables for the next timeslot;
		currentInflowCharge = 0;
		currentOutflowCharge = 0;
		currentKWhConsumed = 0;
		currentKWhProduced = 0;

	}

	public CustomerInfo getCustomerInfo() {
		return customerInfo;
	}

}
