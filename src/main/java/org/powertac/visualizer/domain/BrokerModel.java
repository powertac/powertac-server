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
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.MeterGaugeChartModel;

public class BrokerModel implements VisualBroker {

	Logger log = Logger.getLogger(BrokerModel.class);
	// basic
	private String name;
	private Appearance appearance;
	private int currentTimeslotIndex = -1;
	private String id;
	private String fakeChart;
	// customers
	private int customerCount;
	// balance
	private double cashBalance;
	private double energyBalance; // kWh

	private List<TariffSpecification> tariffSpecifications;
	private List<TariffTransaction> tariffTransactions;
	private List<BalancingTransaction> balancingTransactions;
	// customers
	private Set<CustomerModel> customerModels;
	// JSON:
	private JSONArray cashBalanceJson;
	private JSONArray energyBalanceJson;
	private String seriesOptions;
	// array of names for each customer
	private JSONArray customerTicks;
	// array of broker's share in each customer, in percentages:
	private JSONArray customerCountByTypes;

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
		//
		// JSONArray point = new JSONArray();
		// try {
		// point.put(0).put(0);
		// cashBalanceJson.put(0, point);
		// energyBalanceJson.put(0, point);
		// } catch (JSONException e) {
		// log.warn("Problems with cashBalance JSON for broker:" + name);
		// }
		StringBuilder options = new StringBuilder();
		options.append("{color: '").append(appearance.getColorCode()).append("', label: '").append(name).append("'}");
		seriesOptions = options.toString();
		log.info(seriesOptions);
		customerTicks = new JSONArray();
		customerCountByTypes = new JSONArray();

		// TODO: delete this line (for testing only!)
		// customerCount = (int) (Math.random() * 10000);

	}

	public void updateCashBalance(double balance) {
		this.cashBalance = new BigDecimal(balance).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();;
		// TODO:JSON
		updateJSON(cashBalanceJson, currentTimeslotIndex, cashBalance);

	}

	public void updateEnergyBalance(double balance) {
		updateJSON(energyBalanceJson, currentTimeslotIndex, energyBalance);
	}

	public void addBalancingTransaction(BalancingTransaction balancingTransaction) {
		this.energyBalance = new BigDecimal(balancingTransaction.getKWh()).setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue();

		// TODO:JSON
		updateJSON(energyBalanceJson, currentTimeslotIndex, energyBalance);

		balancingTransactions.add(balancingTransaction);
	}

	private void updateJSON(JSONArray array, int x, double y) {
		int decimal_points=2;
		
		JSONArray point = new JSONArray();
		BigDecimal bd = new BigDecimal(y);
		bd.setScale(decimal_points, BigDecimal.ROUND_HALF_UP);
		try {
			point.put(x).put(bd.doubleValue());
			array.put(x, point);
		} catch (JSONException e) {
			log.warn("Problems with JSON for broker:" + name);
		}

		log.info(array.toString());

	}

	public void addTariffSpecification(TariffSpecification tariffSpecification) {
		tariffSpecifications.add(tariffSpecification);
		log.info("\n" + name + ": my tariffSpec: +\n" + tariffSpecification.toString());
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
		if (customerCount != 0) {
			// broker's portfolio is changed
			buildCustomerCountByTypes();
		}

	}

	private void buildCustomerCountByTypes() {
		customerCountByTypes = new JSONArray();
		for (Iterator iterator = customerModels.iterator(); iterator.hasNext();) {
			CustomerModel customerModel = (CustomerModel) iterator.next();
			
				customerCountByTypes.put(customerModel.getCustomerCount());
		}

	}

	public void setCustomerModels(Set<CustomerModel> customerModels) {
		this.customerModels = customerModels;
		buildCustomerCountByTypes();
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
		this.currentTimeslotIndex = currentTimeslotIndex;
	}

	public double getCashBalance() {
		return cashBalance;
	}

	public double getEnergyBalance() {
		return energyBalance;
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

	public String getSeriesOptions() {
		return seriesOptions;
	}

	public int getCustomerCount() {
		return customerCount;
	}

	public String getCustomerTicksJSONText() {
		return customerTicks.toString();
	}

	public String getCustomerCountByTypesJSONText() {
		return customerCountByTypes.toString();
	}
}
