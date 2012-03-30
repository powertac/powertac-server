package org.powertac.visualizer.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.visualizer.customers.CustomerService;
import org.powertac.visualizer.domain.*;
import org.powertac.visualizer.wholesale.WholesaleModel;
import org.powertac.visualizer.wholesale.WholesaleSnapshot;

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
import org.springframework.stereotype.Service;

/**
 * Holds properties of the Visualizer such as number of received messages from
 * the simulator, number of Visualizer runs etc.
 * 
 * @author Jurica Babic
 */
@Service
public class VisualizerBean implements Serializable {

	/**
     *
     */
	private static final long serialVersionUID = 1L;
	private Logger log = Logger.getLogger(VisualizerBean.class);

	private int messageCounter;
	private int visualizerRunCount;

	private DayOverview dayOverview;

	// wholesale:
	private WholesaleModel wholesaleModel;
	private WholesaleSnapshot currentWholesaleSnapshot;

	private Competition competition;
	private Instant firstTimeslotInstant;
	private List<BrokerModel> brokers;
	private TimeslotUpdate timeslotUpdate;
	private String simulationStatus;
	private WeatherReport weatherReport;
	private WeatherForecast weatherForecast;
	private int timeslotIndex;
	private List<GencoModel> gencos;
	private int relativeTimeslotIndex;
	// JSON stuff
	private String brokerSeriesOptions;
	private String brokerSeriesColors;
	private JSONArray subscriptionsPieChartJSON;
	private JSONArray brokerCashBalancesJSON;
	@Autowired
	private AppearanceListBean appearanceList;
	@Autowired
	private CustomerService customers;

	public VisualizerBean() {
		init();

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
		init();
		appearanceList.resetAvailableList(); // so broker appearances will be
		customers.recycle(); // free for the next competition
	}

	public void init() {
		messageCounter = 0;
		visualizerRunCount++;

		dayOverview = null;

		competition = null;
		brokers = new ArrayList<BrokerModel>();

		firstTimeslotInstant = null;

		timeslotUpdate = null;
		simulationStatus = null;
		weatherReport = null;
		weatherForecast = null;
		timeslotIndex = 0;
		gencos = new ArrayList<GencoModel>();
		relativeTimeslotIndex = -1;
		brokerSeriesOptions = "";
		brokerSeriesColors = "";
		subscriptionsPieChartJSON = new JSONArray();
		brokerCashBalancesJSON = new JSONArray();
		wholesaleModel = new WholesaleModel();
		currentWholesaleSnapshot = null;

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
			stringDebug += "Name: " + brokerModel.getName() + " Color code: " + brokerModel.getAppearance().getColorCode() + " Icon:" + brokerModel.getAppearance().getIconLocation() + "\n";
			// build broker series options:
			seriesOptions.append(prefix);
			prefix = ",";
			seriesOptions.append(brokerModel.getSeriesOptions());
			// build colors:
			brokerSeriesColors.put(brokerModel.getAppearance().getColorCode());
			this.brokers.add(brokerModel);
		}
		this.brokerSeriesOptions = seriesOptions.toString();
		this.brokerSeriesColors = brokerSeriesColors.toString();
		// this.brokers = brokers;
		log.debug("Broker list:\n" + stringDebug + " series options:" + brokerSeriesOptions + "\n JSON colors array:" + brokerSeriesColors.toString());

	}

	public List<BrokerModel> getBrokers() {
		return brokers;
	}

	public CustomerService getCustomers() {
		return customers;
	}

	public TimeslotUpdate getTimeslotUpdate() {
		return timeslotUpdate;
	}

	public void setTimeslotUpdate(TimeslotUpdate timeslotUpdate) {
		this.timeslotUpdate = timeslotUpdate;
	}

	public int getCurrentFirstEnabledTimeslotSerialNumber() {
		if (timeslotUpdate != null)
			return timeslotUpdate.getFirstEnabled();
		else {
			return -1;
		}
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

	public List<GencoModel> getGencos() {
		return gencos;
	}

	/**
	 * Should be called
	 * 
	 * @param relativeTimeslotIndex
	 */
	public void setRelativeTimeslotIndex(int relativeTimeslotIndex) {
		this.relativeTimeslotIndex = relativeTimeslotIndex;

	}

	public int getRelativeTimeslotIndex() {
		return relativeTimeslotIndex;
	}

	public String getBrokerSeriesOptions() {
		return brokerSeriesOptions;

	}

	public String getBrokerCashBalancesJSONText() {
		return brokerCashBalancesJSON.toString();
	}

	public void setBrokerCashBalancesJSON(JSONArray brokerCashBalancesJSON) {
		this.brokerCashBalancesJSON = brokerCashBalancesJSON;
	}

	public String getSubscriptionsPieChartJSONText() {
		return subscriptionsPieChartJSON.toString();
	}

	public void setSubscriptionsPieChartJSON(JSONArray subscriptionsPieChartJSON) {
		this.subscriptionsPieChartJSON = subscriptionsPieChartJSON;
	}

	public String getBrokerSeriesColors() {
		return brokerSeriesColors;
	}

	public void setFirstTimeslotInstant(Instant firstTimeslot) {
		this.firstTimeslotInstant = firstTimeslot;
	}

	public Instant getFirstTimeslotInstant() {
		return firstTimeslotInstant;
	}

	/**
	 * Shallow copy of brokers collection.
	 * 
	 * @return
	 */
	public List<BrokerModel> getBrokersClone() {
		List<BrokerModel> brokersClone = new ArrayList<BrokerModel>(brokers.size());
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			brokersClone.add(brokerModel);
		}
		return brokersClone;
	}

	public DayOverview getDayOverview() {
		return dayOverview;
	}

	public void setDayOverview(DayOverview dayOverview) {
		this.dayOverview = dayOverview;
	}

	public void setCurrentWholesaleSnapshot(WholesaleSnapshot currentWholesaleSnapshot) {
		this.currentWholesaleSnapshot = currentWholesaleSnapshot;
	}

	public WholesaleSnapshot getCurrentWholesaleSnapshot() {
		return currentWholesaleSnapshot;
	}

	public WholesaleModel getWholesaleModel() {
		return wholesaleModel;
	}

}
