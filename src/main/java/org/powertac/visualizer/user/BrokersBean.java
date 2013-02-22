package org.powertac.visualizer.user;

import java.io.Serializable;

import org.powertac.visualizer.services.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;


public class BrokersBean implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	@Autowired
	public BrokersBean(BrokerService brokerService) {
//		Enumeration<BrokerModel> brokers = brokerService.getBrokersMap().elements();
//		while (brokers.hasMoreElements()) {
//			brokerList.add(new DisplayableBroker(brokers.nextElement()));
//			}
	}
	
		
	
}
