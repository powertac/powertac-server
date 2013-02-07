package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.ClearedTrade;
import org.powertac.common.MarketTransaction;
import org.powertac.common.OrderbookOrder;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.display.BrokerSeriesTemplate;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.domain.wholesale.VisualizerOrderbookOrder;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.powertac.visualizer.interfaces.WholesaleServiceBeanAccess;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.WholesaleService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.DynamicData;
import org.powertac.visualizer.statistical.SingleTimeslotWholesaleData;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.paginator.PaginatorElementRenderer;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class DistributionBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String distributionDynData;
	private String distributionDynDataOneTimeslot;

	@Autowired
	public DistributionBean(BrokerService brokerService, VisualizerHelperService helper) {

		Gson gson = new Gson();
		createBrokerDistributionTransactions(gson, brokerService, helper);
		
		

	}

	private void createBrokerDistributionTransactions(Gson gson,
			BrokerService brokerService, VisualizerHelperService helper) {
		Collection<BrokerModel> brokers = brokerService.getBrokers();

		ArrayList<Object> distributionTxData = new ArrayList<Object>();
		ArrayList<Object> distributionTxDataOneTimeslot = new ArrayList<Object>();

		// brokers:
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();

			ArrayList<Object> profitData = new ArrayList<Object>();
			ArrayList<Object> netKwhData = new ArrayList<Object>();
			// one timeslot
			ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();
			ArrayList<Object> kwhDataOneTimeslot = new ArrayList<Object>();

			ConcurrentHashMap<Integer, DynamicData> dynDataMap = brokerModel
					.getDistributionCategory().getDynamicDataMap();
			SortedSet<Integer> dynDataSet = new TreeSet<Integer>(dynDataMap.keySet());

		
			// dynamic wholesale data:
			for (Iterator iterator2 = dynDataSet.iterator(); iterator2
					.hasNext();) {
				int key = (Integer) iterator2.next();
				DynamicData dynData = dynDataMap.get(key);
								
				Object[] profit = { helper.getMillisForIndex(key),
						dynData.getProfit()};
				Object[] netMwh = { helper.getMillisForIndex(key), dynData.getEnergy() };

				profitData.add(profit);
				netKwhData.add(netMwh);

				// one timeslot:
				Object[] profitOneTimeslot = { helper.getMillisForIndex(key),
						dynData.getProfitDelta() };
				Object[] kWhOneTimeslot = { helper.getMillisForIndex(key),
						dynData.getEnergyDelta() };
				profitDataOneTimeslot.add(profitOneTimeslot);
				kwhDataOneTimeslot.add(kWhOneTimeslot);
			}

			distributionTxData.add(new BrokerSeriesTemplate(brokerModel.getName()
					+ "_PROFIT", brokerModel.getAppearance().getColorCode(), 0,
					profitData));
			distributionTxData.add(new BrokerSeriesTemplate(brokerModel.getName()
					+ "_KWH", brokerModel.getAppearance().getColorCode(), 1,
					netKwhData));

			// one timeslot:
			distributionTxDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
					.getName() + "_PROFIT", brokerModel.getAppearance()
					.getColorCode(), 0, profitDataOneTimeslot));
			distributionTxDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
					.getName() + "_KWH", brokerModel.getAppearance()
					.getColorCode(), 1, kwhDataOneTimeslot));


		}
		this.distributionDynData = gson.toJson(distributionTxData);
		this.distributionDynDataOneTimeslot = gson
				.toJson(distributionTxDataOneTimeslot);

	}
	
	public String getDistributionDynData() {
		return distributionDynData;
	}
	
	public String getDistributionDynDataOneTimeslot() {
		return distributionDynDataOneTimeslot;
	}



}
