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
import org.joda.time.Instant;
import org.powertac.visualizer.display.TariffPusher;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.json.BrokersJSON;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.DynamicData;
import org.powertac.visualizer.statistical.TariffCategory;
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
		ArrayList<TariffPusher> tariffPushers = new ArrayList<TariffPusher>();

		
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {

			BrokerModel b = (BrokerModel) iterator.next();

			TariffCategory tc = b.getTariffCategory();
			TariffDynamicData tdd = tc.getLastTariffDynamicData();
			TariffPusher tp = new TariffPusher(
					b.getName(),
					helper.getMillisForIndex(tdd.getDynamicData().getTsIndex()),
					tdd.getDynamicData().getProfit(), tdd.getDynamicData()
							.getEnergy(), tdd.getCustomerCount(), tdd
							.getDynamicData().getProfitDelta(), tdd
							.getDynamicData().getProfitDelta(), tdd
							.getCustomerCountDelta());
			tariffPushers.add(tp);
		}

		pushContext.push("/tariffpush", gson.toJson(tariffPushers));
	}

	public List<BrokerModel> getBrokerList() {
		return brokers;
	}

	public ArrayList<BrokerModel> getBrokers() {
		return new ArrayList<BrokerModel>(brokers);
	}

}
