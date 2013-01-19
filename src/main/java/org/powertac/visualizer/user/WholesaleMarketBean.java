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

public class WholesaleMarketBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String energyMostRecentClearingsJson;
	private String cashMostRecentClearingsJson;

	private String clearedTradesDataJson;
	private String allMarketTransactionsData;

	private String wholesaleDynData;
	private String wholesaleDynDataOneTimeslot;

	@Autowired
	public WholesaleMarketBean(BrokerService brokerService,
			WholesaleService wholesaleService, VisualizerHelperService helper) {

		Gson gson = new Gson();

		createMostRecentClearings(gson, wholesaleService);
		createAllClearings(gson, wholesaleService);
		createBrokerWholesaleTransactions(gson, brokerService, helper);
		
		

	}

	private void createBrokerWholesaleTransactions(Gson gson,
			BrokerService brokerService, VisualizerHelperService helper) {
		Collection<BrokerModel> brokers = brokerService.getBrokers();

		int safetyTsIndex = helper.getSafetyTimeslotIndex();
		ArrayList<Object> allWholesaleData = new ArrayList<Object>();
		ArrayList<Object> wholesaleTxData = new ArrayList<Object>();
		ArrayList<Object> wholesaleTxDataOneTimeslot = new ArrayList<Object>();

		// brokers:
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();

			ArrayList<Object> profitData = new ArrayList<Object>();
			ArrayList<Object> netMwhData = new ArrayList<Object>();
			// one timeslot
			ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();
			ArrayList<Object> mwhDataOneTimeslot = new ArrayList<Object>();
			// market tx data
			ArrayList<Object> wholesaleTxBrokerData = new ArrayList<Object>();

			ConcurrentHashMap<Integer, DynamicData> dynDataMap = brokerModel
					.getWholesaleCategory().getDynamicDataMap();
			SortedSet<Integer> dynDataSet = new TreeSet<Integer>(brokerModel
					.getWholesaleCategory().getDynamicDataMap().keySet())
					.headSet(safetyTsIndex, true);

			double totalProfit = 0;
			double totalEnergy = 0;
			// dynamic wholesale data:
			for (Iterator iterator2 = dynDataSet.iterator(); iterator2
					.hasNext();) {
				int key = (Integer) iterator2.next();
				DynamicData dynData = dynDataMap.get(key);
				
				totalEnergy+=dynData.getEnergyDelta();
				totalProfit+=dynData.getProfitDelta();
				
				Object[] profit = { helper.getMillisForIndex(key),
						totalProfit};
				Object[] netMwh = { helper.getMillisForIndex(key),
						totalEnergy };

				profitData.add(profit);
				netMwhData.add(netMwh);

				// one timeslot:
				Object[] profitOneTimeslot = { helper.getMillisForIndex(key),
						dynData.getProfitDelta() };
				Object[] kWhOneTimeslot = { helper.getMillisForIndex(key),
						dynData.getEnergyDelta() };
				profitDataOneTimeslot.add(profitOneTimeslot);
				mwhDataOneTimeslot.add(kWhOneTimeslot);
			}

			ConcurrentHashMap<Integer, List<MarketTransaction>> mtxMap = brokerModel
					.getWholesaleCategory().getMarketTxs();
			SortedSet<Integer> mtxSortedSet = new TreeSet<Integer>(brokerModel
					.getWholesaleCategory().getMarketTxs().keySet()).headSet(
					safetyTsIndex, true);

			for (Iterator iterator2 = mtxSortedSet.iterator(); iterator2
					.hasNext();) {
				int key = (Integer) iterator2.next();

				List<MarketTransaction> mtxList = mtxMap.get(key);
				for (Iterator iterator3 = mtxList.iterator(); iterator3
						.hasNext();) {
					MarketTransaction marketTransaction = (MarketTransaction) iterator3
							.next();
					Object[] mtxEntry = { marketTransaction.getPrice(),
							marketTransaction.getMWh() };
					wholesaleTxBrokerData.add(mtxEntry);
				}

			}

			wholesaleTxData.add(new BrokerSeriesTemplate(brokerModel.getName()
					+ "_PROFIT", brokerModel.getAppearance().getColorCode(), 0,
					profitData));
			wholesaleTxData.add(new BrokerSeriesTemplate(brokerModel.getName()
					+ "_MWH", brokerModel.getAppearance().getColorCode(), 1,
					netMwhData));

			// one timeslot:
			wholesaleTxDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
					.getName() + "_PROFIT", brokerModel.getAppearance()
					.getColorCode(), 0, profitDataOneTimeslot));
			wholesaleTxDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
					.getName() + "_MWH", brokerModel.getAppearance()
					.getColorCode(), 1, mwhDataOneTimeslot));
			allWholesaleData.add(new BrokerSeriesTemplate(
					brokerModel.getName(), brokerModel.getAppearance()
							.getColorCodeRGBShading(), wholesaleTxBrokerData));

		}
		this.wholesaleDynData = gson.toJson(wholesaleTxData);
		this.wholesaleDynDataOneTimeslot = gson
				.toJson(wholesaleTxDataOneTimeslot);
		this.allMarketTransactionsData = gson.toJson(allWholesaleData);

	}

	private void createAllClearings(Gson gson, WholesaleService wholesaleService) {
		ArrayList<Object> allClearedTrades = new ArrayList<Object>();
		// maps within the map
		Collection<ConcurrentHashMap<Long, ClearedTrade>> allTrades = wholesaleService
				.getClearedTrades().values();
		for (Iterator iterator = allTrades.iterator(); iterator.hasNext();) {
			ConcurrentHashMap<Long, ClearedTrade> concurrentHashMap = (ConcurrentHashMap<Long, ClearedTrade>) iterator
					.next();
			// collection:
			Collection<ClearedTrade> trades = concurrentHashMap.values();
			for (Iterator iterator2 = trades.iterator(); iterator2.hasNext();) {
				ClearedTrade ct = (ClearedTrade) iterator2.next();

				Object[] entry = { ct.getExecutionMWh(), ct.getExecutionPrice() };
				allClearedTrades.add(entry);
			}
		}
		clearedTradesDataJson = gson.toJson(allClearedTrades);

	}

	private void createMostRecentClearings(Gson gson,
			WholesaleService wholesaleService) {
		// most recent clearings for each timeslot.
		ArrayList<Object> energyMostRecentClearings = new ArrayList<Object>();
		ArrayList<Object> cashMostRecentClearings = new ArrayList<Object>();

		ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> mapFinalTrades = wholesaleService
				.getFinalClearedTrades();

		SortedSet<Long> keys = new TreeSet<Long>(mapFinalTrades.keySet());

		for (Long key : keys) {
			ConcurrentHashMap<Long, ClearedTrade> clearedTrades = mapFinalTrades
					.get(key);
			if (clearedTrades != null) {

				Long lastKey = new TreeSet<Long>(clearedTrades.keySet()).last();

				ClearedTrade mostRecentClearing = clearedTrades.get(lastKey);
				Object[] energy = { key, mostRecentClearing.getExecutionMWh() };
				Object[] cash = { key, mostRecentClearing.getExecutionPrice() };
				energyMostRecentClearings.add(energy);
				cashMostRecentClearings.add(cash);
			}
		}
		energyMostRecentClearingsJson = gson.toJson(energyMostRecentClearings);
		cashMostRecentClearingsJson = gson.toJson(cashMostRecentClearings);

	}

	public String getCashMostRecentClearingsJson() {
		return cashMostRecentClearingsJson;
	}

	public String getEnergyMostRecentClearingsJson() {
		return energyMostRecentClearingsJson;
	}

	public String getClearedTradesDataJson() {
		return clearedTradesDataJson;
	}

	public String getAllMarketTransactionsData() {
		return allMarketTransactionsData;
	}

	public String getWholesaleDynData() {
		return wholesaleDynData;
	}

	public String getWholesaleDynDataOneTimeslot() {
		return wholesaleDynDataOneTimeslot;
	}

}
