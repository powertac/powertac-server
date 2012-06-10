package org.powertac.visualizer.domain.broker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.powertac.common.BalancingTransaction;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.domain.Appearance;
import org.powertac.visualizer.interfaces.DisplayableBroker;
import org.powertac.visualizer.interfaces.TimeslotModelUpdate;
import org.powertac.visualizer.interfaces.VisualBroker;
import org.powertac.visualizer.json.BrokerJSON;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

public class BrokerModel implements VisualBroker, DisplayableBroker, TimeslotModelUpdate {

	Logger log = Logger.getLogger(BrokerModel.class);
	// basic
	private String name;
	private Appearance appearance;
	private String id;
	// customers
	private int customerCount;
	// balance
	private double cashBalance;
	private double energyBalance; // kWh
	private HashMap<Integer, DayState> dayStates = new HashMap<Integer, DayState>();
	//
	private DayState currentDayState = new DayState(this);
	private DayState displayableDayState;

	private List<BalancingTransaction> balancingTransactions;
	// customers
	private Set<CustomerModel> customerModels;

	private int firstTimeslotIndex;

	private BrokerJSON json;
	
	private HashMap<Long, TariffInfo> tariffInfoMaps = new HashMap<Long, TariffInfo>(); 
	private ArrayList<TariffInfo> tariffInfos = new ArrayList<TariffInfo>();

	public BrokerModel(String name, Appearance appearance) {
		this.name = name;
		this.appearance = appearance;
		// collections:
		balancingTransactions = new ArrayList<BalancingTransaction>();
		customerModels = new HashSet<CustomerModel>();

		id = RandomStringUtils.random(7, "abcdefghijklomnopqrstuvxy".toCharArray());

		// JSON:
		JSONObject seriesOptions = new JSONObject();
		try {
			seriesOptions.put("color", appearance.getColorCode()).put("label", name);
		} catch (JSONException e) {
			log.warn("Broker JSON series options fail.");
		}
		json = new BrokerJSON(seriesOptions);

	}

	public void updateCashBalance(double balance) {
		this.cashBalance = Helper.roundNumberTwoDecimal(balance);
	}

	/**
	 * Method for updating energy balance in case broker didn't receive
	 * balancing transaction
	 * 
	 * @param balance
	 */
	public void updateEnergyBalance(double balance) {
		this.energyBalance = Helper.roundNumberTwoDecimal(balance);
	}

	public void addBalancingTransaction(BalancingTransaction balancingTransaction) {
		this.energyBalance = Helper.roundNumberTwoDecimal(balancingTransaction.getKWh());
		balancingTransactions.add(balancingTransaction);
		currentDayState.addBalancingTransaction(balancingTransaction);
	}

	public void addTariffSpecification(TariffSpecification tariffSpecification) {
		TariffInfo info = new TariffInfo(tariffSpecification);
		tariffInfoMaps.put(tariffSpecification.getId(), info);
		tariffInfos.add(info);
		log.info("\n" + name + ": my tariffSpec: +\n" + tariffSpecification.toString());
		currentDayState.addTariffSpecification(tariffSpecification);
	}

	public void addTariffTransaction(TariffTransaction tariffTransaction) {
		log.info("\n" + name + ": my tariffTrans: +\n" + tariffTransaction.toString());
		
		// find customer of this transaction:
		if (tariffTransaction.getCustomerInfo() != null) {
			for (Iterator<CustomerModel> iterator = customerModels.iterator(); iterator.hasNext();) {
				CustomerModel customerModel = (CustomerModel) iterator.next();
				if (customerModel.getCustomerInfo().getId() == tariffTransaction.getCustomerInfo().getId()) {

					// add transaction to a customer
					customerModel.addTariffTransaction(tariffTransaction);
					break;
				}
			}
		}
		// manage customer count
		int customerCount = Helper.getCustomerCount(tariffTransaction);
		this.customerCount += customerCount;

		currentDayState.addTariffTransaction(tariffTransaction);
		
		//tariff info:
		tariffInfoMaps.get(tariffTransaction.getTariffSpec().getId()).addTariffTransaction(tariffTransaction);

	}

	public void setCustomerModels(Set<CustomerModel> customerModels) {
		this.customerModels = customerModels;
		buildCustomerTicks();
	}

	private void buildCustomerTicks() {
		JSONArray customerTicks = new JSONArray();
		for (Iterator iterator = customerModels.iterator(); iterator.hasNext();) {
			CustomerModel customerModel = (CustomerModel) iterator.next();
			customerTicks.put(customerModel.getCustomerInfo().getName());
		}
		json.setCustomerTicks(customerTicks);
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

	/**
	 * Builds customers bubble JSON Array. Visibility is set to public to
	 * enhance testability.
	 */
	public void buildCustomersBubble() {
		// create new customer bubble JSON:
		JSONArray newCustomersBubbleJSONArray = new JSONArray();
		for (Iterator<CustomerModel> iterator = customerModels.iterator(); iterator.hasNext();) {
			JSONArray customerBubbleJson = new JSONArray();
			CustomerModel customer = (CustomerModel) iterator.next();

			customerBubbleJson.put((int) customer.getTotalCash()).put((int) customer.getTotalEnergy())
					.put(customer.getCustomerCount()).put(customer.getCustomerInfo().getName());
			newCustomersBubbleJSONArray.put(customerBubbleJson);
		}
		json.setCustomersBubbleJson(newCustomersBubbleJSONArray);

	}

	public double getCashBalance() {
		return cashBalance;
	}

	public void setCashBalance(double cashBalance) {
		this.cashBalance = cashBalance;
	}

	public double getEnergyBalance() {
		return energyBalance;
	}

	public void setEnergyBalance(double energyBalance) {
		this.energyBalance = energyBalance;
	}

	public DayState getDisplayableDayState() {
		return displayableDayState;
	}

	
	public List<BalancingTransaction> getBalancingTransactions() {
		return balancingTransactions;
	}

	public Set<CustomerModel> getCustomerModels() {
		return customerModels;
	}

	public HashMap<Integer, DayState> getDayStates() {
		return dayStates;
	}

	public String getId() {
		return id;
	}

	public int getCustomerCount() {
		return customerCount;
	}

	public DayState getCurrentDayState() {
		return currentDayState;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.powertac.visualizer.interfaces.TimeslotModelUpdate#update(int)
	 */
	public void update(int timeslotIndex) {
		
		//update tariff Infos:
		for (Iterator iterator = tariffInfos.iterator(); iterator.hasNext();) {
			TariffInfo type = (TariffInfo) iterator.next();
			type.update(timeslotIndex);
		}
		
		final int DAY_FIX = 1; // to avoid array-like displaying of days (day
		// one should be 1, not 0)

		int currentDay = timeslotIndex / 24 + DAY_FIX;

		// start of the game, day is not configured so it is not in the map:
		if (currentDayState.getDay() == -1) {
			currentDayState.setDay(currentDay);
			dayStates.put(currentDay, currentDayState);
			// remember the first timeslot index, it will be useful for
			// calculating end of the day.
			firstTimeslotIndex = timeslotIndex;
		}

		// update:
		int hour = (timeslotIndex-firstTimeslotIndex) % 24;
		currentDayState.addTimeslotValues(hour, cashBalance, energyBalance);
		// JSON:
		// all timeslots:
		json.getCashBalanceJson().put(Helper.pointJSON(timeslotIndex, cashBalance));
		json.getEnergyBalanceJson().put(Helper.pointJSON(timeslotIndex, energyBalance));

		
		
		
		// if timeslot complete is last hour of the day:
		if (hour == 23 && firstTimeslotIndex!=timeslotIndex) {

			// FINISH JSON MODEL for this day:
			// average:
			json.getEnergyBalanceDailyAVGJson().put(
					Helper.pointJSON(currentDay, dayStates.get(currentDay).getAvgEnergyBalance()));
			// end of day values:
			json.getCashBalanceDailyJson().put(Helper.pointJSON(currentDay, cashBalance));
			// SET THE CURRENT DAY AS DISPLAYABLE:
			displayableDayState = currentDayState;

			// CREATE NEW DAY:
			int newDay = currentDay + 1;
			currentDayState = new DayState(newDay, this);
			// add to map:
			dayStates.put(newDay, currentDayState);

		}

		buildCustomersBubble();

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
