package org.powertac.visualizer.domain.broker;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.domain.Appearance;
import org.powertac.visualizer.json.BrokerJSON;
import org.powertac.visualizer.statistical.BalancingCategory;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

public class BrokerModel {//implements TimeslotModelUpdate {

	Logger log = Logger.getLogger(BrokerModel.class);
	// basic
	private String name;
	private Appearance appearance;
	private String id;
	// customers
	private int customerCount;
	// balance
	private double cashBalance;
	private BrokerJSON json;
	private HashMap<Long, TariffInfo> tariffInfoMaps = new HashMap<Long, TariffInfo>(); 
	private ArrayList<TariffInfo> tariffInfos = new ArrayList<TariffInfo>();
	
	private BalancingCategory balancingCategory;

	public BrokerModel(String name, Appearance appearance) {
		this.name = name;
		this.appearance = appearance;
		// collections:
		//customerModels = new HashSet<CustomerModel>();

		id = RandomStringUtils.random(7, "abcdefghijklomnopqrstuvxy".toCharArray());

		// JSON:
		JSONObject seriesOptions = new JSONObject();
		try {
			seriesOptions.put("color", appearance.getColorCode()).put("label", name);
		} catch (JSONException e) {
			log.warn("Broker JSON series options fail.");
		}
		json = new BrokerJSON(seriesOptions);
		
		balancingCategory = new BalancingCategory(this);

	}
	
	public BalancingCategory getBalancingCategory() {
		return balancingCategory;
	}

	public void updateCashBalance(double balance) {
		this.cashBalance = Helper.roundNumberTwoDecimal(balance);
	}

	public void addTariffSpecification(TariffSpecification tariffSpecification) {
		TariffInfo info = new TariffInfo(tariffSpecification);
		tariffInfoMaps.put(tariffSpecification.getId(), info);
		tariffInfos.add(info);
	}

	public void addTariffTransaction(TariffTransaction tariffTransaction) {
		
	}

		

	// getters and setters:

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Appearance getAppearance() {
		return appearance;
	}

	public void setAppereance(Appearance appearance) {
		this.appearance = appearance;
	}

	public double getCashBalance() {
		return cashBalance;
	}

	public void setCashBalance(double cashBalance) {
		this.cashBalance = cashBalance;
	}

	public String getId() {
		return id;
	}

	public int getCustomerCount() {
		return customerCount;
	}

	public BrokerJSON getJson() {
		return json;
	}
	
	public HashMap<Long, TariffInfo> getTariffInfoMaps() {
		return tariffInfoMaps;
	}
	
	public ArrayList<TariffInfo> getTariffInfos() {
		return (ArrayList<TariffInfo>) tariffInfos.clone();
	}

}
