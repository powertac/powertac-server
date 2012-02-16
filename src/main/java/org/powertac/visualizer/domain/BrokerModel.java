package org.powertac.visualizer.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.BalancingTransaction;
import org.powertac.common.CashPosition;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.Helper;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.model.chart.BubbleChartModel;
import org.primefaces.model.chart.BubbleChartSeries;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.MeterGaugeChartModel;

public class BrokerModel implements VisualBroker, DisplayableBroker {

	Logger log = Logger.getLogger(BrokerModel.class);
	// basic
	private String name;
	private Appearance appearance;
	private int currentTimeslotIndex = 0;
	private String id;
	private String fakeChart;
	// customers
	private int customerCount;
	// balance
	private double cashBalance;
	private double energyBalance; // kWh
	private HashMap<Integer, DayState> dayStates = new HashMap<Integer, DayState>();
	//
	private DayState currentDayState;
	private DayState displayableDayState;

	private List<TariffSpecification> tariffSpecifications;
	private List<TariffTransaction> tariffTransactions;
	private List<BalancingTransaction> balancingTransactions;
	// customers
	private Set<CustomerModel> customerModels;
	private JSONArray customersBubbleJson;
	// JSON:
	// hourly values (all timeslots):
	private JSONArray cashBalanceJson;
	private JSONArray energyBalanceJson;
	// daily values (end of day values):
	private JSONArray cashBalanceDailyJson;
	private JSONArray energyBalanceDailyJson;
	// daily average values
	private JSONArray cashBalanceDailyAVGJson;
	private JSONArray energyBalanceDailyAVGJson;
	// currentDay values
	private JSONArray cashBalanceCurrentDayJson;
	private JSONArray energyBalanceCurrentDayJson;

	private JSONArray customerTicks;

	private String seriesOptions;

	// array of names for each customer

	public BrokerModel(String name, Appearance appearance) {
		this.name = name;
		this.appearance = appearance;
		// collections:
		tariffSpecifications = new ArrayList<TariffSpecification>();
		tariffTransactions = new ArrayList<TariffTransaction>();
		balancingTransactions = new ArrayList<BalancingTransaction>();
		customerModels = new HashSet<CustomerModel>();

		id = RandomStringUtils.random(7, "abcdefghijklomnopqrstuvxy".toCharArray());

		// JSON:
		cashBalanceJson = new JSONArray();
		energyBalanceJson = new JSONArray();
		// daily values (end of day values):
		cashBalanceDailyJson = new JSONArray();
		Helper.updateJSON(cashBalanceDailyJson, 0, 0);
		energyBalanceDailyJson = new JSONArray();
		Helper.updateJSON(energyBalanceDailyJson, 0, 0);
		// daily average values
		cashBalanceDailyAVGJson = new JSONArray();
		Helper.updateJSON(cashBalanceDailyAVGJson, 0, 0);
		energyBalanceDailyAVGJson = new JSONArray();
		Helper.updateJSON(energyBalanceDailyAVGJson, 0, 0);

		// currentDay values
		cashBalanceCurrentDayJson = new JSONArray();
		energyBalanceCurrentDayJson = new JSONArray();

		StringBuilder options = new StringBuilder();
		options.append("{color: '").append(appearance.getColorCode()).append("', label: '").append(name).append("'}");
		seriesOptions = options.toString();
		log.info(seriesOptions);
		customerTicks = new JSONArray();

	}

	public void updateCashBalance(double balance) {
		this.cashBalance = new BigDecimal(balance).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	/**
	 * Method for updating energy balance in case broker didn't receive
	 * balancing transaction
	 * 
	 * @param balance
	 */
	public void updateEnergyBalance(double balance) {
		this.energyBalance = new BigDecimal(balance).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	public void addBalancingTransaction(BalancingTransaction balancingTransaction) {
		this.energyBalance = new BigDecimal(balancingTransaction.getKWh()).setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
		balancingTransactions.add(balancingTransaction);
		currentDayState.addBalancingTransaction(balancingTransaction);
	}

	public void addTariffSpecification(TariffSpecification tariffSpecification) {
		tariffSpecifications.add(tariffSpecification);
		log.info("\n" + name + ": my tariffSpec: +\n" + tariffSpecification.toString());
		currentDayState.addTariffSpecification(tariffSpecification);
	}

	public void addTariffTransaction(TariffTransaction tariffTransaction) {
		log.info("\n" + name + ": my tariffTrans: +\n" + tariffTransaction.toString());
		tariffTransactions.add(tariffTransaction);

		// find customer of this transaction:
		for (Iterator<CustomerModel> iterator = customerModels.iterator(); iterator.hasNext();) {
			CustomerModel customerModel = (CustomerModel) iterator.next();
			if (customerModel.getCustomerInfo().getId() == tariffTransaction.getCustomerInfo().getId()) {

				// add transaction to a customer
				customerModel.addTariffTransaction(tariffTransaction);
				break;
			}
		}
		// manage customer count
		int customerCount = Helper.getCustomerCount(tariffTransaction);
		this.customerCount += customerCount;

		currentDayState.addTariffTransaction(tariffTransaction);

	}

	public void setCustomerModels(Set<CustomerModel> customerModels) {
		this.customerModels = customerModels;
		buildCustomerTicks();
	}

	private void buildCustomerTicks() {
		customerTicks = new JSONArray();
		for (Iterator iterator = customerModels.iterator(); iterator.hasNext();) {
			CustomerModel customerModel = (CustomerModel) iterator.next();
			customerTicks.put(customerModel.getCustomerInfo().getName());
		}
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

	public int getCurrentTimeslotIndex() {
		return currentTimeslotIndex;
	}

	public void setCurrentTimeslotIndex(int currentTimeslotIndex) {
		final int DAY_FIX = 1; // to avoid array-like displaying of days (day
								// one should be 1, not 0)

		int newDay = currentTimeslotIndex / 24 + DAY_FIX;
		int oldDay = this.currentTimeslotIndex / 24 + DAY_FIX;
		// if dayState for the specified day not exists:
		if (!dayStates.containsKey(newDay)) {

			// create new daystate:
			currentDayState = new DayState(newDay, this);
			dayStates.put(newDay, currentDayState);

			// setup new currentDay JSON Arrays:
			cashBalanceCurrentDayJson = currentDayState.getDayCashBalancesJson();
			energyBalanceCurrentDayJson = currentDayState.getDayEnergyBalancesJson();
		}
		// add cashbalance and energybalance:
		int hour = currentTimeslotIndex % 24;
		currentDayState.addTimeslotValues(hour, cashBalance, energyBalance);

		// JSON:
		// all timeslots:
		Helper.updateJSON(cashBalanceJson, currentTimeslotIndex, cashBalance);
		Helper.updateJSON(energyBalanceJson, currentTimeslotIndex, energyBalance);
		// daily action:
		if (currentTimeslotIndex % 24 == 0) {
			// average:
			Helper.updateJSON(cashBalanceDailyAVGJson, oldDay, dayStates.get(oldDay).getAvgCashBalance());
			Helper.updateJSON(energyBalanceDailyAVGJson, oldDay, dayStates.get(oldDay).getAvgEnergyBalance());
			// end of day values:
			Helper.updateJSON(cashBalanceDailyJson, oldDay, cashBalance);
			Helper.updateJSON(energyBalanceDailyJson, oldDay, energyBalance);

			// make oldDay state displayable, but only if currentTimeslotIndex
			// is not 0 (because we have nothing to show yet)
			if (currentTimeslotIndex != 0) {
				displayableDayState = dayStates.get(oldDay);
			}
		}

		buildCustomersBubble();

		// now update:
		this.currentTimeslotIndex = currentTimeslotIndex;
	}

	/**
	 * Builds customers bubble JSON Array. Visibility is set to public to enhance testability.
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
		customersBubbleJson = newCustomersBubbleJSONArray;

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

	public long getOfferedTarrifsCount() {
		return tariffSpecifications.size();
	}

	public List<TariffSpecification> getTariffSpecifications() {
		return tariffSpecifications;
	}

	public List<TariffTransaction> getTariffTransactions() {
		return tariffTransactions;
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

	public JSONArray getCashBalanceJson() {
		return cashBalanceJson;
	}

	public String getCashBalanceJSONText() {

		return cashBalanceJson.toString();

	}

	public void setCashBalanceJson(JSONArray cashBalanceJson) {
		this.cashBalanceJson = cashBalanceJson;
	}

	public JSONArray getEnergyBalanceJson() {
		return energyBalanceJson;
	}

	public void setEnergyBalanceJson(JSONArray energyBalanceJson) {
		this.energyBalanceJson = energyBalanceJson;
	}

	public String getEnergyBalanceJSONText() {

		return energyBalanceJson.toString();

	}

	// daily values:
	public JSONArray getCashBalanceDailyJson() {
		return cashBalanceDailyJson;
	}

	public String getCashBalanceDailyJsonText() {
		return cashBalanceDailyJson.toString();
	}

	public JSONArray getEnergyBalanceDailyJson() {
		return energyBalanceDailyJson;
	}

	public String getEnergyBalanceDailyJsonText() {
		return energyBalanceDailyJson.toString();
	}

	// daily average values:
	public JSONArray getCashBalanceDailyAVGJson() {
		return cashBalanceDailyAVGJson;
	}

	public String getCashBalanceDailyAVGJsonText() {
		return cashBalanceDailyAVGJson.toString();
	}

	public void setCashBalanceDailyAVGJson(JSONArray cashBalanceDailyAVGJson) {
		this.cashBalanceDailyAVGJson = cashBalanceDailyAVGJson;
	}

	public JSONArray getEnergyBalanceDailyAVGJson() {
		return energyBalanceDailyAVGJson;
	}

	public String getEnergyBalanceDailyAVGJsonText() {
		return energyBalanceDailyAVGJson.toString();
	}

	public void setEnergyBalanceDailyAVGJson(JSONArray energyBalanceDailyAVGJson) {
		this.energyBalanceDailyAVGJson = energyBalanceDailyAVGJson;
	}

	// current day values:
	public JSONArray getCashBalanceCurrentDayJson() {
		return cashBalanceCurrentDayJson;
	}

	public String getCashBalanceCurrentDayJsonText() {
		return cashBalanceCurrentDayJson.toString();
	}

	public void setCashBalanceCurrentDayJson(JSONArray cashBalanceCurrentDayJson) {
		this.cashBalanceCurrentDayJson = cashBalanceCurrentDayJson;
	}

	public JSONArray getEnergyBalanceCurrentDayJson() {
		return energyBalanceCurrentDayJson;
	}

	public String getEnergyBalanceCurrentDayJsonText() {
		return energyBalanceCurrentDayJson.toString();
	}

	public void setEnergyBalanceCurrentDayJson(JSONArray energyBalanceCurrentDayJson) {
		this.energyBalanceCurrentDayJson = energyBalanceCurrentDayJson;
	}

	public String getSeriesOptions() {
		return seriesOptions;
	}

	public int getCustomerCount() {
		return customerCount;
	}

	public String getCustomerTicksJSONText() {
		return customerTicks.toString();
	}

	public DayState getCurrentDayState() {
		return currentDayState;
	}

	public String getCustomersBubbleJsonText() {
		return customersBubbleJson.toString();
	}

}
