package org.powertac.visualizer.services;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
//import org.apache.tools.ant.taskdefs.Tstamp;
import org.joda.time.Instant;
import org.powertac.common.ClearedTrade;
import org.powertac.common.MarketTransaction;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.display.BrokerSeriesTemplate;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.push.DynDataPusher;
import org.powertac.visualizer.push.FinancePusher;
import org.powertac.visualizer.push.NominationCategoryPusher;
import org.powertac.visualizer.push.NominationPusher;
import org.powertac.visualizer.push.StatisticsPusher;
import org.powertac.visualizer.push.TariffMarketPusher;
import org.powertac.visualizer.push.WholesaleMarketPusher;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.BalancingCategory;
import org.powertac.visualizer.statistical.DistributionCategory;
import org.powertac.visualizer.statistical.DynamicData;
import org.powertac.visualizer.statistical.FinanceCategory;
import org.powertac.visualizer.statistical.FinanceDynamicData;
import org.powertac.visualizer.statistical.TariffCategory;
import org.powertac.visualizer.statistical.WholesaleCategory;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;

/**
 * 
 * @author Jurica Babic
 * 
 */

@Service
public class BrokerService implements TimeslotCompleteActivation, Recyclable,
		Serializable {

	private Logger log = Logger.getLogger(BrokerService.class);
	private static final long serialVersionUID = 15L;
	private ConcurrentHashMap<String, BrokerModel> brokersMap;
	private ArrayList<BrokerModel> brokers;
	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private VisualizerHelperService helper;
	
	private final int TIMESLOTS_TO_DISPLAY = 2;
	 

	public BrokerService() {
		recycle();
	}

	public ConcurrentHashMap<String, BrokerModel> getBrokersMap() {
		return brokersMap;
	}

	public void setBrokers(ArrayList<BrokerModel> brokers) {
		this.brokers = brokers;
	}

	public void setBrokersMap(ConcurrentHashMap<String, BrokerModel> brokersMap) {
		this.brokersMap = brokersMap;
	}

	/**
	 * @param name
	 * @return Broker model associated with a specified name, or null if the
	 *         broker cannot be found.
	 */
	public BrokerModel findBrokerByName(String name) {
		return brokersMap.get(name);

	}

	public void recycle() {
		brokersMap = new ConcurrentHashMap<String, BrokerModel>();
		brokers = new ArrayList<BrokerModel>();
	}

	public void activate(int timeslotIndex, Instant postedTime) {

		// // do the push:
		PushContext pushContext = PushContextFactory.getDefault()
				.getPushContext();

		Gson gson = new Gson();
		ArrayList<TariffMarketPusher> tariffMarketPushers = new ArrayList<TariffMarketPusher>();
		ArrayList<WholesaleMarketPusher> wholesaleMarketPushers = new ArrayList<WholesaleMarketPusher>();
		ArrayList<DynDataPusher> balancingMarketPushers = new ArrayList<DynDataPusher>();
		ArrayList<DynDataPusher> distributionPushers = new ArrayList<DynDataPusher>();
		ArrayList<FinancePusher> financePushers = new ArrayList<FinancePusher>();
		ArrayList allWholesaleData = new ArrayList();
		ArrayList<ArrayList<Double>> brokersOverview = new ArrayList<ArrayList<Double>>();

		NominationPusher np = null;
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {

			BrokerModel b = (BrokerModel) iterator.next();
			
			ArrayList<Object> wholesaleTxBrokerData = new ArrayList<Object>();
			ArrayList<Double> brokerOverview = new ArrayList<Double>();

			// Tariff market push
			TariffCategory tc = b.getTariffCategory();
			TariffDynamicData tdd = tc.getLastTariffDynamicData();
			TariffMarketPusher tp = new TariffMarketPusher(
					b.getName(),
					helper.getMillisForIndex(tdd.getDynamicData().getTsIndex()),
					tdd.getDynamicData().getProfit(), 
					tdd.getDynamicData().getEnergy(), 
					tdd.getCustomerCount(), 
					tdd.getDynamicData().getProfitDelta(), 
					tdd.getDynamicData().getEnergyDelta(), 
					tdd.getCustomerCountDelta());
			tariffMarketPushers.add(tp);
			

			// Wholesale market push
			int safetyTxIndex = timeslotIndex - 1;
			WholesaleCategory wc = b.getWholesaleCategory();
			wc.updateAccounts(safetyTxIndex);

			ConcurrentHashMap<Integer, List<MarketTransaction>> mtxMap = wc.getMarketTxs();
			SortedSet<Integer> mtxSortedSet = new TreeSet<Integer>(wc
					.getMarketTxs().keySet()).headSet(safetyTxIndex, true);
			SortedSet<Integer> mtxSortedSetSubset = mtxSortedSet
					.subSet(((safetyTxIndex - TIMESLOTS_TO_DISPLAY) < 0) ? 0
							: safetyTxIndex - TIMESLOTS_TO_DISPLAY, safetyTxIndex);// tom
			
			for (Iterator iterator2 = mtxSortedSetSubset.iterator(); iterator2
					.hasNext();) {
				int key = (Integer) iterator2.next();

				List<MarketTransaction> mtxList = mtxMap.get(key);
				for (Iterator iterator3 = mtxList.iterator(); iterator3
						.hasNext();) {
					MarketTransaction marketTransaction = (MarketTransaction) iterator3
							.next();
					ArrayList<Double> transaction = new ArrayList<Double>();
					transaction.add(marketTransaction.getPrice());
					transaction.add(marketTransaction.getMWh());
					//Object[] mtxEntry = { marketTransaction.getPrice(),	marketTransaction.getMWh() };
					wholesaleTxBrokerData.add(transaction);
				}

			}
			
			allWholesaleData.add(wholesaleTxBrokerData);

			double profitDelta = 0, profit = 0, energy = 0, energyDelta = 0;

			NavigableSet<Integer> safeKeys = new TreeSet<Integer>(wc
					.getDynamicDataMap().keySet()).headSet(safetyTxIndex, true);
			if (!safeKeys.isEmpty()) {
				DynamicData lastWholesaledd = wc.getDynamicDataMap().get(
						safeKeys.last());

				profitDelta = lastWholesaledd.getProfitDelta();
				energyDelta = lastWholesaledd.getEnergyDelta();
				profit = lastWholesaledd.getProfit();
				energy = lastWholesaledd.getEnergy();
			}

			WholesaleMarketPusher wmp = new WholesaleMarketPusher(b.getName(),
					helper.getMillisForIndex(safetyTxIndex), profitDelta,
					energyDelta, profit, energy);
			wholesaleMarketPushers.add(wmp);

			// balancing market pushers:
			BalancingCategory bc = b.getBalancingCategory();
			DynamicData bdd = bc.getLastDynamicData();
			DynDataPusher bddp = new DynDataPusher(
					b.getName(),
					helper.getMillisForIndex(bdd.getTsIndex()),
					bdd.getProfit(), 
					bdd.getEnergy(), 
					bdd.getProfitDelta(),
					bdd.getEnergyDelta());
			balancingMarketPushers.add(bddp);

			// balancing market pushers:
			DistributionCategory dc = b.getDistributionCategory();
			DynamicData ddd = dc.getLastDynamicData();
			DynDataPusher dddp = new DynDataPusher(
					b.getName(),
					helper.getMillisForIndex(ddd.getTsIndex()),
					ddd.getProfit(), 
					ddd.getEnergy(), 
					ddd.getProfitDelta(),
					ddd.getEnergyDelta());
			distributionPushers.add(dddp);

			// finance push
			FinanceCategory fc = b.getFinanceCategory();
			FinanceDynamicData fdd = fc.getLastFinanceDynamicData();
			FinancePusher fp = new FinancePusher(
					b.getName(),
					helper.getMillisForIndex(fdd.getTsIndex()),
					fdd.getProfit(), 
					fdd.getProfitDelta());
			financePushers.add(fp);
			
			//game overview push
			brokerOverview.add(fdd.getProfit());
			brokerOverview.add(tdd.getDynamicData().getProfit());
			brokerOverview.add(profit);
			brokerOverview.add(bdd.getProfit());
			brokerOverview.add(ddd.getProfit());
			brokersOverview.add(brokerOverview);

			if (np == null) {
				np = new NominationPusher(new NominationCategoryPusher(
						b.getName(), (long) fc.getProfit()),
						new NominationCategoryPusher(b.getName(), (long) Math
								.abs(bc.getEnergy())),
						new NominationCategoryPusher(b.getName(), tc
								.getCustomerCount()));
			} else {
				long profitAmount = (long) fc.getProfit();
				long balanceAmount = (long) Math.abs(bc.getEnergy());
				long customerAmount = (long) tc.getCustomerCount();
				if (profitAmount > np.getProfit().getAmount()) {
					np.setProfit(new NominationCategoryPusher(b.getName(),
							profitAmount));
				}
				if (balanceAmount < Math.abs(np.getBalance().getAmount())) {
					np.setBalance(new NominationCategoryPusher(b.getName(),
							balanceAmount));
				}
				if (customerAmount > np.getCustomerNumber().getAmount()) {
					np.setCustomerNumber(new NominationCategoryPusher(b
							.getName(), customerAmount));
				}
			}
		}
		
		if (np != null) {
			visualizerBean.setNominationPusher(np);
		}

		pushContext.push("/tariffpush", gson.toJson(tariffMarketPushers));
		pushContext.push("/wholesalemarketpush",
				gson.toJson(wholesaleMarketPushers));
		pushContext.push("/balancingmarketpush",
				gson.toJson(balancingMarketPushers));
		pushContext.push("/distributionpush", gson.toJson(distributionPushers));
		pushContext.push("/financepush", gson.toJson(financePushers));
		pushContext.push("/markettransactionepush", gson.toJson(allWholesaleData));
		pushContext.push("/gameoverview", gson.toJson(brokersOverview));
		
	}

	public ArrayList<BrokerModel> getBrokers() {
		return brokers;
	}

}
