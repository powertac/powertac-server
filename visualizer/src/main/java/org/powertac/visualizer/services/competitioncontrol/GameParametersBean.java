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
  private String seedName;
  private String weatherName;

	public GameParametersBean() {
		brokers = new ArrayList<FakeBroker>();
	}

	public String getBootstrapData() {
		return bootstrapData;
	}

	public void setBootstrapData(String bootstrapData) {
    if (bootstrapData.trim().equals("")) {
      this.bootstrapData = null;
    } else {
      this.bootstrapData = bootstrapData.trim();
    }
	}

	public String getJmsUrl() {
		return jmsUrl;
	}

	public void setJmsUrl(String jmsUrl) {
    if (jmsUrl.trim().equals("")) {
      this.jmsUrl = null;
    } else {
      this.jmsUrl = jmsUrl.trim();
    }
	}

	public String getLogSuffix() {
		return logSuffix;
	}

	public void setLogSuffix(String logSuffix) {
    if (logSuffix.trim().equals("")) {
      this.logSuffix = null;
    } else {
      this.logSuffix = logSuffix.trim();
    }
	}

	public String getServerConfig() {
		return serverConfig;
	}

	public void setServerConfig(String serverConfig) {
    if (serverConfig.trim().equals("")) {
      this.serverConfig = null;
    } else {
      this.serverConfig = serverConfig.trim();
    }
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
    if (bootstrapFilename.trim().equals("")) {
      this.bootstrapFilename = null;
    } else {
      this.bootstrapFilename = bootstrapFilename.trim();
    }
	}

  public String getSeedName() {
    return seedName;
  }

  public void setSeedName(String seedName) {
    if (seedName.trim().equals("")) {
      this.seedName = null;
    } else {
      this.seedName = seedName.trim();
    }
  }

  public String getWeatherName() {
    return weatherName;
  }

  public void setWeatherName(String weatherName) {
    if (weatherName.trim().equals("")) {
      this.weatherName = null;
    } else {
      this.weatherName = weatherName.trim();
    }
  }
}
