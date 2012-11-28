package org.powertac.visualizer.services.competitioncontrol;

import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class GameParametersBean {
	private String bootstrapData;
	private String jmsUrl;
	private String serverConfig;
	private ArrayList<FakeBroker> brokers;
	private String logSuffix;
	private String newBrokerName;
	private String bootstrapFilename;

	public GameParametersBean() {
		brokers = new ArrayList<FakeBroker>();
	}

	public String getBootstrapData() {
		return bootstrapData;
	}

	public void setBootstrapData(String bootstrapData) {
		this.bootstrapData = bootstrapData;
	}

	public String getJmsUrl() {
		return jmsUrl;
	}

	public void setJmsUrl(String jmsUrl) {
		this.jmsUrl = jmsUrl;
	}

	public String getLogSuffix() {
		return logSuffix;
	}

	public void setLogSuffix(String logSuffix) {
		this.logSuffix = logSuffix;
	}

	public String getServerConfig() {
		return serverConfig;
	}

	public void setServerConfig(String serverConfig) {
		this.serverConfig = serverConfig;
	}

	public ArrayList<FakeBroker> getBrokers() {
		return brokers;
	}

	public void setBrokers(ArrayList<FakeBroker> brokers) {
		this.brokers = brokers;
	}

	public String getNewBrokerName() {
		return newBrokerName;
	}

	public void setNewBrokerName(String newBrokerName) {
		this.newBrokerName = newBrokerName;
	}

	public void addBroker() {
		brokers.add(new FakeBroker(newBrokerName));
	}

	public void resetBrokerList() {
		brokers = new ArrayList<FakeBroker>();
	}

	public String getBootstrapFilename() {
		return bootstrapFilename;
	}
	public void setBootstrapFilename(String bootstrapFilename) {
		this.bootstrapFilename = bootstrapFilename;
	}

}
