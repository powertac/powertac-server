package org.powertac.visualizer.domain;

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
	private static int CHART_BUFFER = 40;
	private String id;
	private String fakeChart;
	// balance
	private double cashBalance;
	private double energyBalance; // kWh
	// history
	private Map<Object, Number> cashHistory;
	private Map<Object, Number> energyHistory;
	private List<TariffSpecification> tariffSpecifications;
	private List<TariffTransaction> tariffTransactions;
	private List<BalancingTransaction> balancingTransactions;
	// customers
	private Set<CustomerModel> customerModels;
	// // C H A R T S:
	// // CASH:
	// // Cartesian:
	// private CartesianChartModel cashBalanceChart;
	// private ChartSeries cashBalanceChartSeries;
	// // ENERGY:
	// // Cartesian:
	// private CartesianChartModel energyBalanceChart;
	// private ChartSeries energyBalanceChartSeries;
	// JSON:
	JSONArray cashBalanceJson;
	JSONArray energyBalanceJson;
	String seriesOptions;

	public BrokerModel(String name, Appearance appearance) {
		this.name = name;
		this.appearance = appearance;
		// collections:
		tariffSpecifications = new ArrayList<TariffSpecification>();
		tariffTransactions = new ArrayList<TariffTransaction>();
		balancingTransactions = new ArrayList<BalancingTransaction>();
		customerModels = new HashSet<CustomerModel>();

		// loadCashCartesianChart();
		//
		// loadEnergyCartesianChart();

		energyHistory = new HashMap<Object, Number>();
		cashHistory = new HashMap<Object, Number>();

		id = RandomStringUtils.random(7, "abcdefghijklomnopqrstuvxy".toCharArray());
		// TODO: energy chart

		// JSON:
		cashBalanceJson = new JSONArray();
		energyBalanceJson = new JSONArray();
		// TODO:JSON
		JSONArray point = new JSONArray();
		try {
			point.put(0).put(0);
			cashBalanceJson.put(0, point);
			energyBalanceJson.put(0, point);
		} catch (JSONException e) {
			log.warn("Problems with cashBalance JSON for broker:" + name);
		}
		StringBuilder options = new StringBuilder();
		options.append("{color: '").append(appearance.getColorCode()).append("', label: '").append(name).append("'}");
		seriesOptions = options.toString();
		log.info(seriesOptions);

	}

	public void updateCashBalance(double balance) {
		this.cashBalance = balance;
		cashHistory.put(currentTimeslotIndex, balance);

		// TODO:JSON
		updateJSON(cashBalanceJson, currentTimeslotIndex, cashBalance);

	}

	public void addBalancingTransaction(BalancingTransaction balancingTransaction) {
		this.energyBalance = balancingTransaction.getKWh();

		// TODO:JSON
		updateJSON(energyBalanceJson, currentTimeslotIndex, energyBalance);

		balancingTransactions.add(balancingTransaction);
		// linear chart:
		energyHistory.put(currentTimeslotIndex, balancingTransaction.getKWh());
	}

	private void updateJSON(JSONArray array, int x, double y) {
		JSONArray point = new JSONArray();
		try {
			point.put(x).put(y);
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
			}
		}

	}

	public void addCustomerModel(CustomerModel customerModel) {
		customerModels.add(customerModel);
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

	public Map<Object, Number> getCashHistory() {
		return cashHistory;
	}

	public Map<Object, Number> getEnergyHistory() {
		return energyHistory;
	}

	// public CartesianChartModel getCashBalanceChart() {
	// return cashBalanceChart;
	// }
	//
	// public ChartSeries getCashBalanceChartSeries() {
	// return cashBalanceChartSeries;
	// }
	//
	// public CartesianChartModel getEnergyBalanceChart() {
	// return energyBalanceChart;
	// }
	//
	// public ChartSeries getEnergyBalanceChartSeries() {
	// return energyBalanceChartSeries;
	// }

	public int getMaxX() {
		return currentTimeslotIndex;
	}

	public int getMinX() {
		return currentTimeslotIndex - CHART_BUFFER;
	}

	// TODO: getters

	// private void loadEnergyCartesianChart() {
	// // energy chart:
	// energyHistory = new HashMap<Object, Number>();
	// energyBalanceChart = new CartesianChartModel();
	// energyBalanceChartSeries = new ChartSeries(name);
	// energyBalanceChartSeries.setData(energyHistory);
	// energyBalanceChart.addSeries(energyBalanceChartSeries);
	// // add dummy value for testing
	// energyHistory.put(-1, (-100)*Math.random());
	//
	// }
	//
	//
	// private void loadCashCartesianChart() {
	// // cash chart:
	// cashHistory = new HashMap<Object, Number>();
	// cashBalanceChart = new CartesianChartModel();
	// cashBalanceChartSeries = new ChartSeries(name);
	// cashBalanceChartSeries.setData(cashHistory);
	// cashBalanceChart.addSeries(cashBalanceChartSeries);
	// // add dummy value for testing
	// cashHistory.put(-1, (-100)*Math.random());
	//
	// }
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

	public String getSeriesOptions(){
		return seriesOptions;
	}

	public String getFakeChart() {
		double x = Math.random() * 10;
		double y = Math.random() * 10;
		fakeChart = "[[[" + x + ", " + y + "]]]";
		return fakeChart;
	}
}
