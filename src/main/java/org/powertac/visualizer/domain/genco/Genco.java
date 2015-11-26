package org.powertac.visualizer.domain.genco;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.CashPosition;
import org.powertac.common.Timeslot;
import org.powertac.visualizer.interfaces.TimeslotModelUpdate;
import org.powertac.visualizer.json.GencoJSON;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Domain object that represents a single genco in the game.
 * 
 * @author Jurica Babic
 * 
 */
public class Genco implements TimeslotModelUpdate {

	private static Logger log = LogManager.getLogger(Genco.class);
	private Broker broker;
	private double cash;
	private GencoJSON json;
	private String id;

	private Map<Integer, WholesaleDataGenco> wholesaleDatas;

	public Genco(Broker broker) {
		this.broker = broker;

		id = "genco"
				+ RandomStringUtils.random(7,
						"abcdefghijklomnopqrstuvxy".toCharArray());
		wholesaleDatas = new ConcurrentSkipListMap<Integer, WholesaleDataGenco>();
		json = new GencoJSON();

		log.info(this.toString() + " created.");

	}

	/**
	 * Finds (or creates one if cannot be found) wholesaleData by the specified
	 * timeslot
	 * 
	 * @param timeslot
	 * @return
	 */
	public WholesaleDataGenco findWholesaleDataByTimeslot(Timeslot timeslot) {
		int serialNumber = timeslot.getSerialNumber();
		WholesaleDataGenco data = wholesaleDatas.get(serialNumber);

		if (data == null) {
			data = new WholesaleDataGenco(timeslot);
			wholesaleDatas.put(serialNumber, data);
		}

		return data;

	}

	public Broker getBroker() {
		return broker;
	}

	public void addCashPosition(CashPosition position) {
		cash = position.getBalance();
	}

	public double getCash() {
		return cash;
	}

	public void update(int timeslotIndex, Instant postedTime) {

		try {
			JSONArray cashPoint = new JSONArray();
			cashPoint.put(timeslotIndex).put(cash);
			json.getCashPositions().put(cashPoint);
		} catch (JSONException e) {
			log.warn("JSON update for Genco is not working.");
		}

	}

	public ArrayList<WholesaleDataGenco> getWholesaleDatasList() {
		return new ArrayList<WholesaleDataGenco>(wholesaleDatas.values());
	}

	public GencoJSON getJson() {
		return json;
	}

	@Override
	public String toString() {
		return "Genco : " + this.broker.getUsername() + ", apiKey : "
				+ this.broker.getKey();
	}

	public String getId() {
		return id;
	}
}
