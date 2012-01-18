package org.powertac.visualizer.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.visualizer.domain.*;

import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffSpecification;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.TimeslotUpdate;
import org.primefaces.json.JSONArray;
import org.primefaces.model.chart.CartesianChartModel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Holds properties of the Visualizer such as number of received messages from
 * the simulator, number of Visualizer runs etc.
 * 
 * @author Jurica Babic
 */
public class VisualizerBean implements Serializable {

	/**
     *
     */
	private static final long serialVersionUID = 1L;
	private Logger log = Logger.getLogger(VisualizerBean.class);

	private int messageCounter;
	private int visualizerRunCount;

	private Competition competition;
	private List<CustomerInfo> customers;
	private List<BrokerModel> brokers;
	private TimeslotUpdate timeslotUpdate;
	private String simulationStatus;
	private WeatherReport weatherReport;
	private WeatherForecast weatherForecast;
	private int timeslotIndex;
	private List<GencoModel> gencos;
	private int relativeTimeslotIndex;
	private int firstTimeslotIndex;
	// JSON stuff
	private String brokerSeriesOptions;
	private String brokerSeriesColors;
	// convinient variable for holding the sum of all customers:
	private CustomerModel customerModel;
	@Autowired
	private AppearanceListBean appearanceList;

	public VisualizerBean() {
		messageCounter = 0;
		visualizerRunCount = 0;

	}

	public void incrementMessageCounter() {
		messageCounter++;
	}

	public int getMessageCount() {
		return messageCounter;
	}

	/**
	 * Configures visualizer bean for the new competition instance. Resets its
	 * properties and increments Visualizer counter.
	 */
	public void newRun() {
		messageCounter = 0;
		visualizerRunCount++;
		competition = null;
		brokers = null;
		appearanceList.resetAvailableList(); // so broker appearances will be
												// free for the next competition
		customers = null;
		timeslotUpdate = null;
		simulationStatus = null;
		weatherReport = null;
		weatherForecast = null;
		timeslotIndex = 0;
		gencos = new ArrayList<GencoModel>();
		relativeTimeslotIndex = -1;
		firstTimeslotIndex = -1;
		brokerSeriesOptions = "";
		brokerSeriesColors = "";
		customerModel = new CustomerModel();

	}

	public int getVisualizerRunCount() {
		return visualizerRunCount;
	}

	public Competition getCompetition() {
		return competition;
	}

	public void setCompetition(Competition competition) {
		this.competition = competition;
	}

	public void setBrokers(List<BrokerModel> brokers) {
		JSONArray brokerSeriesColors = new JSONArray();
		StringBuilder seriesOptions = new StringBuilder();
		String prefix = "";
		String stringDebug = "";
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			stringDebug += "Name: " + brokerModel.getName() + " Color code: "
					+ brokerModel.getAppearance().getColorCode() + " Icon:"
					+ brokerModel.getAppearance().getIconLocation() + "\n";
			// build broker series options:
			seriesOptions.append(prefix);
			prefix = ",";
			seriesOptions.append(brokerModel.getSeriesOptions());
			// build colors:
			brokerSeriesColors.put(brokerModel.getAppearance().getColorCode());
		}
		this.brokerSeriesOptions = seriesOptions.toString();
		this.brokerSeriesColors = brokerSeriesColors.toString();
		this.brokers = brokers;
		log.info("Broker list:\n" + stringDebug + " series options:" + brokerSeriesOptions + "\n JSON colors array:"
				+ brokerSeriesColors.toString());

	}

	public List<BrokerModel> getBrokers() {
		return brokers;
	}

	public List<CustomerInfo> getCustomers() {
		return customers;
	}

	public void setCustomers(List<CustomerInfo> customers) {
		this.customers = customers;
		StringBuilder builder = new StringBuilder();

		for (Iterator iterator = customers.iterator(); iterator.hasNext();) {
			CustomerInfo customerInfo = (CustomerInfo) iterator.next();
			builder.append("ID:" + customerInfo.getId()).append(" NAME:" + customerInfo.getName())
					.append(" POPULATION:" + customerInfo.getPopulation());

		}
		log.info("Customer list:\n" + builder.toString());

	}

	public TimeslotUpdate getTimeslotUpdate() {
		return timeslotUpdate;
	}

	public void setTimeslotUpdate(TimeslotUpdate timeslotUpdate) {
		this.timeslotUpdate = timeslotUpdate;
	}

	public String getSimulationStatus() {
		return simulationStatus;
	}

	public void setSimulationStatus(String simulationStatus) {
		this.simulationStatus = simulationStatus;
	}

	public WeatherReport getWeatherReport() {
		return weatherReport;
	}

	public void setWeatherReport(WeatherReport weatherReport) {
		this.weatherReport = weatherReport;
	}

	public WeatherForecast getWeatherForecast() {
		return weatherForecast;
	}

	public void setWeatherForecast(WeatherForecast weatherForecast) {
		this.weatherForecast = weatherForecast;
	}

	public int getTimeslotIndex() {
		return timeslotIndex;
	}

	public void setTimeslotIndex(int timeslotIndex) {
		if (firstTimeslotIndex == -1) {// first time: set firstTimeslotIndex
			firstTimeslotIndex = timeslotIndex;
		}
		this.timeslotIndex = timeslotIndex;
		// update relative timeslot index
		relativeTimeslotIndex = timeslotIndex - firstTimeslotIndex;
	}

	public List<GencoModel> getGencos() {
		return gencos;
	}

	public int getRelativeTimeslotIndex() {
		return relativeTimeslotIndex;
	}

	public int getFirstTimeslotIndex() {
		return firstTimeslotIndex;
	}

	public CustomerModel getCustomerModel() {
		return customerModel;
	}

	public String getBrokerSeriesOptions() {
		return brokerSeriesOptions;

	}

	public String getBrokerCashBalancesJSONText() {
		StringBuilder cash = new StringBuilder();
		String prefix = "";
		if (brokers != null) {
			for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
				BrokerModel broker = (BrokerModel) iterator.next();
				cash.append(prefix);
				prefix = ",";
				cash.append(broker.getCashBalanceJSONText());

			}

			return cash.toString();
		} else
			return "";
	}

	public String getSubscriptionsPieChartJSONText() {
		JSONArray marketShare = new JSONArray();
		if (brokers != null) {
			for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
				BrokerModel broker = (BrokerModel) iterator.next();
				marketShare.put(broker.getCustomerCount());
			}
			return marketShare.toString();
		} else
			return null;
	}

	public String getBrokerSeriesColors() {
		return brokerSeriesColors;
	}
}
