package org.powertac.visualizer.services.handlers;

import java.util.Arrays;

import org.powertac.common.Competition;
import org.powertac.common.TariffTransaction;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerMessageHandler implements Initializable {

	@Autowired
	private MessageDispatcher router;
	@Autowired
	private CustomerService customerService;

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(Competition.class, TariffTransaction.class, CustomerBootstrapData.class)) {
			router.registerMessageHandler(this, clazz);
		}
	}

	public void handleMessage(Competition competition) {
		customerService.addCustomers(competition.getCustomers());
	}

	public void handleMessage(TariffTransaction transaction) {
		Customer customer = customerService.findCustomerByCustomerInfo(transaction.getCustomerInfo());
		customer.addTariffTransaction(transaction);
	}

	public void handleMessage(CustomerBootstrapData data) {
		Customer customer = customerService.findCustomerByName(data.getCustomerName());
		if (customer != null) {
			customer.addCustomerBootstrapData(data);
		}

	}

}
