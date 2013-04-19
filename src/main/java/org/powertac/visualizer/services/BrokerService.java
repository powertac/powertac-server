package org.powertac.visualizer.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
//import org.apache.tools.ant.taskdefs.Tstamp;
import org.joda.time.Instant;
import org.powertac.common.ClearedTrade;
import org.powertac.common.MarketTransaction;
import org.powertac.visualizer.beans.VisualizerBean;
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

		NominationPusher np = null;
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {

			BrokerModel b = (BrokerModel) iterator.next();

			// Tariff market push
			TariffCategory tc = b.getTariffCategory();
			TariffDynamicData tdd = tc.getLastTariffDynamicData();
			TariffMarketPusher tp = new TariffMarketPusher(
					b.getName(),
					helper.getMillisForIndex(tdd.getDynamicData().getTsIndex()),
					tdd.getDynamicData().getProfit(), tdd.getDynamicData()
							.getEnergy(), tdd.getCustomerCount(), tdd
							.getDynamicData().getProfitDelta(), tdd
							.getDynamicData().getEnergyDelta(), tdd
							.getCustomerCountDelta());
			tariffMarketPushers.add(tp);

			// Wholesale market push
			int safetyTxIndex = timeslotIndex - 1;
			WholesaleCategory wc = b.getWholesaleCategory();
			wc.updateAccounts(safetyTxIndex);
			ArrayList<Object> newMarketTxs = new ArrayList<Object>();

			if (wc.getMarketTxs().containsKey(safetyTxIndex)) {
				List<MarketTransaction> mtxs = wc.getMarketTxs().get(
						safetyTxIndex);
				for (Iterator iterator2 = mtxs.iterator(); iterator2.hasNext();) {
					MarketTransaction mtx = (MarketTransaction) iterator2
							.next();
					Object[] mtxEntry = { mtx.getPrice(), mtx.getMWh() };
					newMarketTxs.add(mtxEntry);
				}
			}
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
					energyDelta, newMarketTxs, profit, energy);
			wholesaleMarketPushers.add(wmp);
			

			// balancing market pushers:
			BalancingCategory bc = b.getBalancingCategory();
			DynamicData bdd = bc.getLastDynamicData();
			DynDataPusher bddp = new DynDataPusher(b.getName(),
					helper.getMillisForIndex(bdd.getTsIndex()),
					bdd.getProfit(), bdd.getEnergy(), bdd.getProfitDelta(),
					bdd.getEnergyDelta());
			balancingMarketPushers.add(bddp);

			// balancing market pushers:
			DistributionCategory dc = b.getDistributionCategory();
			DynamicData ddd = dc.getLastDynamicData();
			DynDataPusher dddp = new DynDataPusher(b.getName(),
					helper.getMillisForIndex(ddd.getTsIndex()),
					ddd.getProfit(), ddd.getEnergy(), ddd.getProfitDelta(),
					ddd.getEnergyDelta());
			distributionPushers.add(dddp);

			// finance push
			FinanceCategory fc = b.getFinanceCategory();
			FinanceDynamicData fdd = fc.getLastFinanceDynamicData();
			FinancePusher fp = new FinancePusher(b.getName(),
					helper.getMillisForIndex(fdd.getTsIndex()),
					fdd.getProfit(), fdd.getProfitDelta());
			financePushers.add(fp);

			if (np == null) {
				np = new NominationPusher(new NominationCategoryPusher(
						b.getName(), (long) fc.getProfit()),
						new NominationCategoryPusher(b.getName(), (long) Math.abs(bc
								.getEnergy())), new NominationCategoryPusher(
								b.getName(), tc.getCustomerCount()));
			} else {
				long profitAmount = (long) fc.getProfit();
				long balanceAmount = (long) Math.abs(bc.getEnergy());
				long customerAmount = (long) tc.getCustomerCount();
				if(profitAmount>np.getProfit().getAmount()){
					np.setProfit(new NominationCategoryPusher(b.getName(), profitAmount));
				}
				if(balanceAmount<Math.abs(np.getBalance().getAmount())){
					np.setBalance(new NominationCategoryPusher(b.getName(), balanceAmount));
				}
				if(customerAmount>np.getCustomerNumber().getAmount()){
					np.setCustomerNumber(new NominationCategoryPusher(b.getName(), customerAmount));
				}
			}
		}
		
		if(np!=null){
			visualizerBean.setNominationPusher(np);
		}

		pushContext.push("/tariffpush", gson.toJson(tariffMarketPushers));
		pushContext.push("/wholesalemarketpush",
				gson.toJson(wholesaleMarketPushers));
		pushContext.push("/balancingmarketpush",
				gson.toJson(balancingMarketPushers));
		pushContext.push("/distributionpush", gson.toJson(distributionPushers));
		pushContext.push("/financepush", gson.toJson(financePushers));
	}

	public ArrayList<BrokerModel> getBrokers() {
		return brokers;
	}

}
