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

	private String tariffDynData;
	private String tariffDynDataOneTimeslot;

	@Autowired
	public TariffMarketBean(BrokerService brokerService) {

		Gson gson = new Gson();

		// Data Array
		ArrayList<Object> tariffData = new ArrayList<Object>();

		ArrayList<Object> tariffDataOneTimeslot = new ArrayList<Object>();

		Collection<BrokerModel> brokers = brokerService.getBrokersMap()
				.values();
		// brokers:
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();

			ArrayList<Object> customerNumberData = new ArrayList<Object>();
			ArrayList<Object> profitData = new ArrayList<Object>();
			ArrayList<Object> netKWhData = new ArrayList<Object>();

			// one timeslot
			ArrayList<Object> customerNumberDataOneTimeslot = new ArrayList<Object>();
			ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();
			ArrayList<Object> kwhDataOneTimeslot = new ArrayList<Object>();

			ConcurrentHashMap<Integer, TariffDynamicData> tariffDynData = brokerModel
					.getTariffCategory().getTariffDynamicDataMap();

			Set<Integer> keysTariffDynData = new TreeSet<Integer>(brokerModel
					.getTariffCategory().getTariffDynamicDataMap().keySet());

			// dynamic tariff data:
			for (Iterator iterator2 = keysTariffDynData.iterator(); iterator2
					.hasNext();) {
				int key = (Integer) iterator2.next();
				TariffDynamicData dynData = tariffDynData.get(key);
				Object[] timeCustomerCount = { key, dynData.getCustomerCount() };
				Object[] profit = { key, dynData.getDynamicData().getProfit() };
				Object[] netKWh = { key, dynData.getDynamicData().getEnergy() };

				customerNumberData.add(timeCustomerCount);
				profitData.add(profit);
				netKWhData.add(netKWh);

				// one timeslot:
				Object[] customerCountOneTimeslot = { key,
						dynData.getCustomerCountDelta() };
				Object[] profitOneTimeslot = { key,
						dynData.getDynamicData().getProfitDelta() };
				Object[] kWhOneTimeslot = { key,
						dynData.getDynamicData().getEnergyDelta() };

				customerNumberDataOneTimeslot.add(customerCountOneTimeslot);
				profitDataOneTimeslot.add(profitOneTimeslot);
				kwhDataOneTimeslot.add(kWhOneTimeslot);

			}
			tariffData.add(new BrokerSeriesTemplate(brokerModel.getName()
					+ "_PROFIT", brokerModel.getAppearance().getColorCode(), 0,
					profitData));
			tariffData.add(new BrokerSeriesTemplate(brokerModel.getName()
					+ "_KWH", brokerModel.getAppearance().getColorCode(), 1,
					netKWhData));
			tariffData.add(new BrokerSeriesTemplate(brokerModel.getName()
					+ "_CUST", brokerModel.getAppearance().getColorCode(), 2,
					true, customerNumberData));

			// one timeslot:
			tariffDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
					.getName() + "_PROFIT", brokerModel.getAppearance()
					.getColorCode(), 0, profitDataOneTimeslot));
			tariffDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
					.getName() + "_KWH", brokerModel.getAppearance()
					.getColorCode(), 1, kwhDataOneTimeslot));
			tariffDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
					.getName() + "_CUST", brokerModel.getAppearance()
					.getColorCode(), 2, true, customerNumberDataOneTimeslot));
		}
		this.tariffDynData = gson.toJson(tariffData);
		this.tariffDynDataOneTimeslot = gson.toJson(tariffDataOneTimeslot);

	}

	public String getTariffDynData() {
		return tariffDynData;
	}

	public String getTariffDynDataOneTimeslot() {
		return tariffDynDataOneTimeslot;
	}

}
