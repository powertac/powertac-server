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

import org.apache.log4j.Logger;
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
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.WholesaleService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.DynamicData;
import org.powertac.visualizer.statistical.FinanceDynamicData;
import org.powertac.visualizer.statistical.SingleTimeslotWholesaleData;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.paginator.PaginatorElementRenderer;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class FinanceBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private Logger log = Logger.getLogger(FinanceBean.class);
	private String financeDynData;
	private String financeDynDataOneTimeslot;

	@Autowired
	public FinanceBean(BrokerService brokerService, VisualizerHelperService helper) {
		Collection<BrokerModel> brokers = brokerService.getBrokers();
		Gson gson = new Gson();
		int safetyTsIndex = helper.getSafetyTimeslotIndex();
		ArrayList<Object> financeTxData = new ArrayList<Object>();
		ArrayList<Object> financeTxDataOneTimeslot = new ArrayList<Object>();

		// brokers:
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();

			ArrayList<Object> profitData = new ArrayList<Object>();
			// one timeslot
			ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();

			ConcurrentHashMap<Integer, FinanceDynamicData> dynDataMap = brokerModel
					.getFinanceCategory().getFinanceDynamicDataMap();
			SortedSet<Integer> dynDataSet = new TreeSet<Integer>(
					dynDataMap.keySet()).headSet(safetyTsIndex,true);

			// dynamic wholesale data:
			for (Iterator iterator2 = dynDataSet.iterator(); iterator2
					.hasNext();) {
				int key = (Integer) iterator2.next();
				FinanceDynamicData dynData = dynDataMap.get(key);

				Object[] profit = { helper.getMillisForIndex(key),
					dynData.getProfit() };
				profitData.add(profit);

				// one timeslot:
				Object[] profitOneTimeslot = { helper.getMillisForIndex(key),
						dynData.getProfitDelta() };
				profitDataOneTimeslot.add(profitOneTimeslot);
				log.info("***LOG***" + key);
		
			}
			if(dynDataSet.size()==0){
				//dummy:
				double[] dummy = { helper.getMillisForIndex(0), 0};
				profitData.add(dummy);
				profitDataOneTimeslot.add(dummy);
				
			}

			financeTxData.add(new BrokerSeriesTemplate(brokerModel
					.getName(), brokerModel.getAppearance()
					.getColorCode(), 0, profitData));

			// one timeslot:
			financeTxDataOneTimeslot.add(new BrokerSeriesTemplate(
					brokerModel.getName() + " PROFIT", brokerModel
							.getAppearance().getColorCode(), 0,
					profitDataOneTimeslot));
			

		}
		this.financeDynData = gson.toJson(financeTxData);
		this.financeDynDataOneTimeslot = gson
				.toJson(financeTxDataOneTimeslot);

	}
	public String getFinanceDynData() {
		return financeDynData;
	}
	public String getFinanceDynDataOneTimeslot() {
		return financeDynDataOneTimeslot;
	}

}
