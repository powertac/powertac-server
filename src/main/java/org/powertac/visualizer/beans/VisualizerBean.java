package org.powertac.visualizer.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.visualizer.domain.*;

import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffSpecification;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherReport;
import org.powertac.common.msg.TimeslotUpdate;
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
	private List<TariffSpecification> tariffSpecifications;
	private int timeslotCount;

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
		tariffSpecifications = new ArrayList<TariffSpecification>();
		timeslotCount = 0;
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
		this.brokers = brokers;

		String stringDebug = "";
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			stringDebug += "Name: " + brokerModel.getName() + " Color code: "
					+ brokerModel.getAppearance().getColorCode() + " Icon:"
					+ brokerModel.getAppearance().getIconLocation() + "\n";

		}
		log.debug("I have got following brokers:\n" + stringDebug);

	}
	public List<BrokerModel> getBrokers() {
		return brokers;
	}
	public List<CustomerInfo> getCustomers() {
		return customers;
	}

	public void setCustomers(List<CustomerInfo> customers) {
		this.customers = customers;
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

	public List<TariffSpecification> getTariffSpecifications() {
		return tariffSpecifications;
	}

	public int getTimeslotCount() {
		return timeslotCount;
	}

	public void setTimeslotCount(int timeslotCount) {
		this.timeslotCount = timeslotCount;
	}
	
	public void fake(){
		
	}

}
