package org.powertac.visualizer.services.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.log4j.Logger;
import org.powertac.common.BankTransaction;
import org.powertac.common.CashPosition;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.DistributionTransaction;
import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.common.PluginConfig;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.beans.backing.CustomerBackingBean;
import org.powertac.visualizer.domain.BrokerModel;
import org.primefaces.component.carousel.Carousel;

@Service
public class VisualizerMessageHandlerService {

	private Logger log = Logger
			.getLogger(VisualizerMessageHandlerService.class);
	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private VisualizerMessageHandlerHelperService helper;
	@Autowired
	private AppearanceListBean appearanceListBean;

	public void handleMessage(Competition competition) {

		if (competition != null) {
			visualizerBean.setCompetition(competition);

			// Build a list of brokers
			List<BrokerModel> brokers = helper.buildBrokerList(competition);

			// add fake brokers for testing:
			brokers.add(new BrokerModel("Fake broker2", appearanceListBean
					.getAppereance()));
			brokers.add(new BrokerModel("Fake broker3", appearanceListBean
					.getAppereance()));

			visualizerBean.setBrokers(brokers);

			// Add customers to an existing collection
			visualizerBean.setCustomers(competition.getCustomers());
		}

		else
			log.debug("Received competition is null");
	}

	public void handleMessage(PluginConfig pluginConfig) {
		// TODO
	}

	public void handleMessage(TimeslotUpdate timeslotUpdate) {
		visualizerBean.setTimeslotUpdate(timeslotUpdate);
		visualizerBean.setTimeslotCount(visualizerBean.getTimeslotCount() + 1);
		log.info("Posted time:" + timeslotUpdate.getPostedTime());

	}

	public void handleMessage(SimStart simStart) {
		// TODO

	}

	public void handleMessage(SimPause simPause) {
		// TODO
	}

	public void handleMessage(WeatherReport weatherReport) {
		visualizerBean.setWeatherReport(weatherReport);

	}

	public void handleMessage(WeatherForecast weatherForecast) {
		visualizerBean.setWeatherForecast(weatherForecast);
		weatherForecast.getPredictions();
		// TODO !!!!!!!!!! FORECAST
	}

	public void handleMessage(TariffSpecification tariffSpecification) {

		

		BrokerModel brokerModel = helper.findBroker(tariffSpecification
				.getBroker());
		if (brokerModel != null) {
			brokerModel.addTariffSpecification(tariffSpecification);
		}

		// add to history
		visualizerBean.getTariffSpecifications().add(tariffSpecification);
	}

	public void handleMessage(BankTransaction bankTransaction) {
		// TODO 
	}

	public void handleMessage(CashPosition cashPosition) {

		log.debug("Balance: "+cashPosition.getBalance()+" for broker "+cashPosition.getBroker().getUsername());
		// update balance, if broker exists
		BrokerModel brokerModel = helper.findBroker(cashPosition.getBroker());
		if (brokerModel != null) {
			brokerModel.updateBalance(visualizerBean.getTimeslotUpdate(),
					cashPosition.getBalance(),
					visualizerBean.getTimeslotCount());

		}
	}

	public void handleMessage(Order order) {
		// TODO
	}

	public void handleMessage(ClearedTrade clearedTrade) {
		// TODO

	}

	public void handleMessage(MarketTransaction marketTransaction) {

		// TODO

	}

	public void handleMessage(MarketPosition marketPosition) {

		// TODO

	}

}
