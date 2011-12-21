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
	private String brokerColors;
	private CartesianChartModel brokerCashBalancesCartesian;
	private CartesianChartModel brokerEnergyBalancesCartesian;

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
		brokerColors = null;
		brokerCashBalancesCartesian = new CartesianChartModel();
		brokerEnergyBalancesCartesian = new CartesianChartModel();
		
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
		

		StringBuilder brokerColors = new StringBuilder();
		String stringDebug = "";
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			stringDebug += "Name: " + brokerModel.getName() + " Color code: "
					+ brokerModel.getAppearance().getColorCode() + " Icon:"
					+ brokerModel.getAppearance().getIconLocation() + "\n";
			// build broker colors:
			brokerColors.append(brokerModel.getAppearance().getColorCode() + ", ");
			//build cash chart:
			brokerCashBalancesCartesian.addSeries(brokerModel.getCashBalanceChartSeries());
			//build energy chart:
			brokerEnergyBalancesCartesian.addSeries(brokerModel.getEnergyBalanceChartSeries());
			
			
		}
		this.brokerColors = brokerColors.toString();
		this.brokers = brokers;
		log.info("Broker list:\n" + stringDebug);

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
					.append(" POPULATION:" + customerInfo.getPopulation())
					.append(" CustomerType:" + customerInfo.getCustomerType());

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

	public String getBrokerColors() {
		return brokerColors;

	}
	public CartesianChartModel getBrokerCashBalancesCartesian() {
		return brokerCashBalancesCartesian;
	}
	
	public CartesianChartModel getBrokerEnergyBalancesCartesian() {
		return brokerEnergyBalancesCartesian;
	}

}
