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
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.json.BrokersJSON;
import org.powertac.visualizer.statistical.DynamicData;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
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
	
	public BrokerService() {
		recycle();
	}

	public ConcurrentHashMap<String, BrokerModel> getBrokersMap() {
		return brokersMap;
	}

	public void addBroker(BrokerModel broker) {
		brokersMap.put(broker.getName(), broker);
		brokers.add(broker);
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

		JSONObject brokersJsonObject = new JSONObject();
		
		Collection<BrokerModel> brokers = brokersMap.values();
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			
			BrokerModel b = (BrokerModel) iterator.next();
			
			b.getWholesaleCategory();//ZATVORIT WHOLESALE za timeslot prije
			
			b.grade(); 
 
			JSONObject balancingObject = new JSONObject();
//			DynamicData balancingData = b.getBalancingCategory()
//					.getLastBalancingData();
//			double[] kWhImbalanceArray = { balancingData.getTimestamp(),
//					balancingData.getkWhImbalance() };
//			double[] priceImbalanceArray = { balancingData.getTimestamp(),
//					balancingData.getPriceImbalance() };
//			double[] unitPriceImbalanceArray = { balancingData.getTimestamp(),
//					balancingData.getUnitPrice() };

			
			try {
//				balancingObject.put("lastKwhImbalance", kWhImbalanceArray);
//				balancingObject.put("lastMoneyImbalance", priceImbalanceArray);
//				balancingObject.put("lastUnitPriceImbalance",unitPriceImbalanceArray);

				JSONObject jsonData = new JSONObject();
				jsonData.put("balancing", balancingObject);
				
				brokersJsonObject.put(b.getId(), jsonData);
				

			} catch (JSONException e) {
				log.error("Error with JSON," + e.getMessage());
			}
		}

		pushContext.push("/brokers",
				String.valueOf(brokersJsonObject.toString()));
	}

	public List<BrokerModel> getBrokerList() {
		return brokers;
	}

	public ArrayList<BrokerModel> getBrokers() {
		return new ArrayList<BrokerModel>(brokers);
	}

}
