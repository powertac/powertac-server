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
import org.powertac.visualizer.domain.wholesale.VisualizerOrderbookOrder;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.powertac.visualizer.interfaces.WholesaleServiceBeanAccess;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.WholesaleService;
import org.powertac.visualizer.statistical.SingleTimeslotWholesaleData;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.paginator.PaginatorElementRenderer;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class WholesaleBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String energyMostRecentClearingsJson;
	private String cashMostRecentClearingsJson;

	private String clearedTradesDataJson;
	private String marketTransactionsJson;

	@Autowired
	public WholesaleBean(BrokerService brokerService,
			WholesaleService wholesaleService) {

		Gson gson = new Gson();

		createMostRecentClearings(gson, wholesaleService);
		createAllClearings(gson, wholesaleService);
		createBrokerWholesaleTransactions(gson, brokerService);

	}

	private void createBrokerWholesaleTransactions(Gson gson,
			BrokerService brokerService) {
		Collection<BrokerModel> brokers = brokerService.getBrokersMap()
				.values();

		ArrayList<Object> allMarketTxList = new ArrayList<Object>();

		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {

			ArrayList<Object> brokerTxs = new ArrayList<Object>();

			BrokerModel broker = (BrokerModel) iterator.next();
			Collection<SingleTimeslotWholesaleData> singleTSData = broker
					.getWholesaleCategory()
					.getClosedSingleTimeslotWholesaleMap().values();
			for (Iterator iterator2 = singleTSData.iterator(); iterator2
					.hasNext();) {
				SingleTimeslotWholesaleData singleData = (SingleTimeslotWholesaleData) iterator2
						.next();
				Collection<MarketTransaction> marketTxs = singleData
						.getMarketTransactions().values();
				for (Iterator iterator3 = marketTxs.iterator(); iterator3
						.hasNext();) {
					MarketTransaction marketTx = (MarketTransaction) iterator3
							.next();
					Object[] marketTxEntry = { marketTx.getPrice(),
							marketTx.getMWh() };
					brokerTxs.add(marketTxEntry);
				}

			}
			allMarketTxList
					.add(new BrokerSeriesTemplate(broker.getName(), broker
							.getAppearance().getColorCodeRGBShading(),
							brokerTxs));
		}
		marketTransactionsJson = gson.toJson(allMarketTxList);

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
	public String getMarketTransactionsJson() {
		return marketTransactionsJson;
	}

}
