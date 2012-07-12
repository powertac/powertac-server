package org.powertac.visualizer.services.handlers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.powertac.common.BalancingTransaction;
import org.powertac.common.BankTransaction;
import org.powertac.common.CashPosition;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Competition;
import org.powertac.common.DistributionTransaction;
import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.PluginConfig;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherReport;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.msg.DistributionReport;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.TariffUpdate;
import org.powertac.common.msg.TimeslotComplete;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.VisualizerApplicationContext;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.interfaces.VisualBroker;
import org.powertac.visualizer.services.WeatherInfoService;
import org.powertac.visualizer.services.WholesaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WeatherMessageHandler implements Initializable {

	private Logger log = Logger.getLogger(WeatherMessageHandler.class);
	@Autowired
	private WeatherInfoService service;

	@Autowired
	private MessageDispatcher router;
	

	
	public void handleMessage(WeatherReport weatherReport) {
		service.setCurrentReport(weatherReport);
	}

	public void handleMessage(WeatherForecast weatherForecast) {
		service.setCurrentForecast(weatherForecast);
	}

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(WeatherForecast.class, WeatherReport.class)) {
			router.registerMessageHandler(this, clazz);
		}
	}

}
