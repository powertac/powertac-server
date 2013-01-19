package org.powertac.visualizer.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.Tstamp;
import org.joda.time.Instant;
import org.powertac.common.MarketTransaction;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.json.BrokersJSON;
import org.powertac.visualizer.push.DynDataPusher;
import org.powertac.visualizer.push.TariffMarketPusher;
import org.powertac.visualizer.push.WholesaleMarketPusher;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.BalancingCategory;
import org.powertac.visualizer.statistical.DistributionCategory;
import org.powertac.visualizer.statistical.DynamicData;
import org.powertac.visualizer.statistical.TariffCategory;
import org.powertac.visualizer.statistical.WholesaleCategory;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;
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
	private double energy;
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
			DynamicData lastWholesaledd = wc.getDynamicDataMap().get(
					safetyTxIndex);
			if (lastWholesaledd != null) {
				List<MarketTransaction> mtxs = wc.getMarketTxs().get(
						safetyTxIndex);
				for (Iterator iterator2 = mtxs.iterator(); iterator2.hasNext();) {
					MarketTransaction mtx = (MarketTransaction) iterator2
							.next();
					Object[] mtxEntry = { mtx.getPrice(), mtx.getMWh() };
					newMarketTxs.add(mtxEntry);
				}

				WholesaleMarketPusher wmp = new WholesaleMarketPusher(
						b.getName(), helper.getMillisForIndex(lastWholesaledd
								.getTsIndex()),
						lastWholesaledd.getProfitDelta(),
						lastWholesaledd.getEnergyDelta(), newMarketTxs,
						lastWholesaledd.getProfit(),
						lastWholesaledd.getEnergy());
				wholesaleMarketPushers.add(wmp);
			}

			// balancing market pushers:
			BalancingCategory bc = b.getBalancingCategory();
			DynamicData bdd = bc.getLastDynamicData();
			DynDataPusher bddp = new DynDataPusher(
					b.getName(),
					helper.getMillisForIndex(bdd.getTsIndex()),
					bdd.getProfit(), bdd.getEnergy(), bdd.getProfitDelta(), bdd
							.getEnergyDelta());
			balancingMarketPushers.add(bddp);

			// balancing market pushers:
			DistributionCategory dc = b.getDistributionCategory();
			DynamicData ddd = dc.getLastDynamicData();
			DynDataPusher dddp = new DynDataPusher(
					b.getName(),
					helper.getMillisForIndex(ddd.getTsIndex()),
					ddd.getProfit(), ddd.getEnergy(), ddd.getProfitDelta(), ddd
							.getEnergyDelta());
			distributionPushers.add(dddp);

		}

		pushContext.push("/tariffpush", gson.toJson(tariffMarketPushers));
		pushContext.push("/wholesalemarketpush",
				gson.toJson(wholesaleMarketPushers));
		pushContext.push("/balancingmarketpush",
				gson.toJson(balancingMarketPushers));
		pushContext.push("/distributionpush",
				gson.toJson(distributionPushers));
	}

	public List<BrokerModel> getBrokerList() {
		return brokers;
	}

	public ArrayList<BrokerModel> getBrokers() {
		return new ArrayList<BrokerModel>(brokers);
	}

}
