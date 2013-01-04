package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.visualizer.display.BrokerSeriesTemplate;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.services.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class TariffMarketBean implements Serializable {

	private String customerCountSeries;

	@Autowired
	public TariffMarketBean(BrokerService brokerService) {

		Gson gson = new Gson();

		// customer number series:
		ArrayList<Object> customerNumberSeriesData = new ArrayList<Object>();

		Collection<BrokerModel> brokers = brokerService.getBrokersMap()
				.values();
		// brokers:
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();

			ArrayList<Object> customerNumberData = new ArrayList<Object>();

			ConcurrentHashMap<Long, TariffDynamicData> tariffDynData = brokerModel
					.getTariffCategory().getTariffDynamicData();

			Set<Long> keysTariffDynData = new TreeSet<Long>(brokerModel
					.getTariffCategory().getTariffDynamicData().keySet());

			// dynamic tariff data:
			for (Iterator iterator2 = keysTariffDynData.iterator(); iterator2
					.hasNext();) {
				long key = (Long) iterator2.next();
				TariffDynamicData dynData = tariffDynData.get(key);
				long[] timeCustomerCount = { key, dynData.getCustomerCount() };
				customerNumberData.add(timeCustomerCount);
			}
			customerNumberSeriesData.add(new BrokerSeriesTemplate(brokerModel
					.getName(), brokerModel.getAppearance().getColorCode(),
					customerNumberData));
		}
		customerCountSeries = gson.toJson(customerNumberSeriesData);
	}
	
	public String getCustomerCountSeries() {
		return customerCountSeries;
	}

}
