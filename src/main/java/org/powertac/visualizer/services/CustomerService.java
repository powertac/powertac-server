package org.powertac.visualizer.services;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/**
 * Contains data about the customers.
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class CustomerService implements TimeslotCompleteActivation, Recyclable {

	private static Logger log = Logger.getLogger(CustomerService.class);

	private HashMap<CustomerInfo, Customer> customerMap;

	public CustomerService() {
		recycle();
	}

	/**
	 * Creates and adds a customer object into the map. Map key will be a given
	 * customerInfo.
	 */
	public void addCustomers(List<CustomerInfo> customerInfos) {
    customerMap = new HashMap<CustomerInfo, Customer>();
		for (CustomerInfo customerInfo : customerInfos) {
			Customer customer = new Customer(customerInfo);
      customerMap.put(customerInfo, customer);
		}
		
		// build list:
		log.info("Customers added: Map size:" + customerMap.size());
	}

	/**
	 * 
	 * @param customerName
	 * @return Customer associated by the given name, or null if the customer
	 *         cannot be found.
	 */
	public Customer findCustomerByNameAndType(String customerName,
			PowerType type) {
		for (Entry<CustomerInfo, Customer> entry : customerMap.entrySet()) {
			CustomerInfo key = entry.getKey();
			Customer value = entry.getValue();
			if (key.getName().equals(customerName)
					&& (key.getPowerType() == type)) {
				return value;
			}
		}
		return null;
	}

	public void recycle() {
		customerMap = new HashMap<CustomerInfo, Customer>();
	}

	public void activate(int timeslotIndex, Instant postedTime) {
		// update jsons for all customers:
		for (Customer type : customerMap.values()) {
			type.update(timeslotIndex, postedTime);

		}
		log.debug("Customer service activation complete. Timeslotindex:"
				+ timeslotIndex);
	}

	public Customer findCustomerByCustomerInfo(CustomerInfo customerInfo) {
		return customerMap.get(customerInfo);
	}

}
