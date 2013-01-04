package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.powertac.visualizer.display.DisplayableBroker;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.services.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;


public class BrokersBean implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private ArrayList<DisplayableBroker> brokerList = new ArrayList<DisplayableBroker>();
	@Autowired
	public BrokersBean(BrokerService brokerService) {
		Enumeration<BrokerModel> brokers = brokerService.getBrokersMap().elements();
		while (brokers.hasMoreElements()) {
			brokerList.add(new DisplayableBroker(brokers.nextElement()));
			}
	}
	
		
	public ArrayList<DisplayableBroker> getBrokerList() {
		return brokerList;
	}
}
