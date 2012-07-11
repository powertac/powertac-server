package org.powertac.visualizer.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.repo.DomainRepo;
import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.primefaces.component.log.Log;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * Contains data about the customers.
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class CustomerService implements TimeslotCompleteActivation,Recyclable{

	private static Logger log = Logger.getLogger(CustomerService.class);
	
	private HashMap<CustomerInfo, Customer> customerMap;
	private ArrayList<Customer> customerList;
	private ArrayList<Customer> producers;
	private ArrayList<Customer> consumers;
	private ArrayList<Customer> storages;

	public CustomerService() {
		recycle();
	}

	/**
	 * Creates and adds a customer object into the map. Map key will be a given
	 * customerInfo.
	 */
	public void addCustomers(List<CustomerInfo> customerInfos) {
		for (Iterator<CustomerInfo> iterator = customerInfos.iterator(); iterator.hasNext();) {
			CustomerInfo customerInfo = (CustomerInfo) iterator.next();
			
			Customer customer = new Customer(customerInfo);
			customerMap.put(customerInfo, customer);	
			
			PowerType genericType = customerInfo.getPowerType().getGenericType();
			
			if(genericType == PowerType.CONSUMPTION){
				consumers.add(customer);
			} else if (genericType == PowerType.PRODUCTION){
				producers.add(customer);
			} else if (genericType==PowerType.STORAGE){
				storages.add(customer);
			}
			
		}
		//build list:
		customerList=new ArrayList<Customer>(customerMap.values());
		log.info("Customers added: List size:"+customerList.size()+" Map size:"+customerMap.size());
		
	}

	/**
	 * 
	 * @param customerName
	 * @return Customer associated by the given name, or null if the customer cannot be found.
	 */
	public Customer findCustomerByNameAndType(String customerName,PowerType type) {
		for (Entry<CustomerInfo, Customer> entry : customerMap.entrySet()) {
			CustomerInfo key = entry.getKey();
			Customer value = entry.getValue();
			if(key.getName().equals(customerName)&&(key.getPowerType()==type)){
				return value;
			}
		}
		return null;

	}

	public void recycle() {
		customerMap = new HashMap<CustomerInfo, Customer>();
		customerList = new ArrayList<Customer>();
		producers = new ArrayList<Customer>();
		consumers = new ArrayList<Customer>();
		storages = new ArrayList<Customer>();
		
	}
	
	@SuppressWarnings("unchecked")
	public List<Customer> getCustomerList() {
		return (List<Customer>) customerList.clone();
	}

	public void activate(int timeslotIndex, Instant postedTime) {
		//update jsons for all customers:
		
	for (Iterator<Customer> iterator = customerList.iterator(); iterator.hasNext();) {
		Customer type = (Customer) iterator.next();
		type.update(timeslotIndex, postedTime);
		
	}
	log.debug("Customer service activation complete. Timeslotindex:"+timeslotIndex);
		
	}

	public Customer findCustomerByCustomerInfo(CustomerInfo customerInfo) {
		return customerMap.get(customerInfo);
	}
	
	public ArrayList<Customer> getConsumers() {
		return (ArrayList<Customer>) consumers.clone();
	}
	
	public ArrayList<Customer> getProducers() {
		return (ArrayList<Customer>) producers.clone();
	}
	
	public ArrayList<Customer> getStorages() {
		return (ArrayList<Customer>) storages.clone();
	}

	

}
