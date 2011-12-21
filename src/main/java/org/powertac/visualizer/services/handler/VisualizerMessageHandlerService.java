package org.powertac.visualizer.services.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.log4j.Logger;
import org.hibernate.validator.util.GetAnnotationParameter;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.format.DateTimeFormatter;
import org.powertac.common.BalancingTransaction;
import org.powertac.common.BankTransaction;
import org.powertac.common.CashPosition;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.DistributionTransaction;
import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.PluginConfig;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherForecastPrediction;
import org.powertac.common.WeatherReport;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.beans.backing.CustomerBackingBean;
import org.powertac.visualizer.comparators.BrokerCashComparator;
import org.powertac.visualizer.comparators.GencoCashComparator;
import org.powertac.visualizer.domain.BrokerModel;
import org.powertac.visualizer.domain.GencoModel;
import org.powertac.visualizer.domain.VisualBroker;
import org.primefaces.component.carousel.Carousel;

@Service
public class VisualizerMessageHandlerService {

	private Logger log = Logger.getLogger(VisualizerMessageHandlerService.class);
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
			brokers.add(0, new BrokerModel("Fake broker2", appearanceListBean.getAppereance()));
			brokers.add(0, new BrokerModel("Fake broker3", appearanceListBean.getAppereance()));

			visualizerBean.setBrokers(brokers);

			// Add customers to an existing collection
			visualizerBean.setCustomers(competition.getCustomers());

			// build customer models for each broker
			helper.buildCustomerModels(visualizerBean.getBrokers(), visualizerBean.getCustomers());
		}

		else
			log.warn("Received competition is null");
	}

	public void handleMessage(PluginConfig pluginConfig) {

		log.info("\n" + pluginConfig.toString());

		// build GENCO
		if (pluginConfig.getRoleName().equals("genco")) {
			visualizerBean.getGencos().add(
					new GencoModel(pluginConfig.getName(), appearanceListBean.getAppereance(), pluginConfig));
			log.info("\nFollowing genco added: "
					+ visualizerBean.getGencos().get(visualizerBean.getGencos().size() - 1).getName());
		}

		// TODO
	}

	public void handleMessage(TimeslotUpdate timeslotUpdate) {
		visualizerBean.setTimeslotUpdate(timeslotUpdate);
		visualizerBean.getCompetition().computeTimeslotIndex(timeslotUpdate.getPostedTime());
		visualizerBean.setTimeslotIndex(visualizerBean.getCompetition().computeTimeslotIndex(
				timeslotUpdate.getPostedTime()));
		int relativeTimeslotIndex = helper.computeRelativeTimeslotIndex(timeslotUpdate.getPostedTime());
		helper.updateTimeslotIndex(relativeTimeslotIndex);

		log.info("\nTimeslot index: " + relativeTimeslotIndex + "\nPostedtime:" + timeslotUpdate.getPostedTime());
	}

	public void handleMessage(SimStart simStart) {
		log.info("\nINSTANT: " + simStart.getStart());
		// TODO

	}

	public void handleMessage(SimPause simPause) {
		// TODO
	}

	public void handleMessage(WeatherReport weatherReport) {
		log.info("\nStart Instant: " + weatherReport.getCurrentTimeslot().getStartInstant());
		visualizerBean.setWeatherReport(weatherReport);

	}

	public void handleMessage(WeatherForecast weatherForecast) {
		log.info("\nCurrent timeslot:\n Serial number: " + weatherForecast.getCurrentTimeslot().getSerialNumber()
				+ " ID:" + weatherForecast.getCurrentTimeslot().getId() + " Start instant: "
				+ weatherForecast.getCurrentTimeslot().getStartInstant());
		visualizerBean.setWeatherForecast(weatherForecast);
		weatherForecast.getPredictions();
		// TODO !!!!!!!!!! FORECAST
	}

	public void handleMessage(TariffSpecification tariffSpecification) {
		log.info("\nBroker: " + tariffSpecification.getBroker().getUsername() + " Min duration: "
				+ tariffSpecification.getMinDuration() + " EarlyWithdrPaymnt "
				+ tariffSpecification.getEarlyWithdrawPayment() + " PeriodicPayment: "
				+ tariffSpecification.getPeriodicPayment() + " SignupPayment" + tariffSpecification.getSignupPayment()
				+ " Expiration: " + tariffSpecification.getExpiration() + " PowerType: "
				+ tariffSpecification.getPowerType());

		// find matching broker and add received tariff spec. to its history.
		BrokerModel brokerModel = helper.findBrokerModel(tariffSpecification.getBroker());
		if (brokerModel != null) {
			brokerModel.addTariffSpecification(tariffSpecification);
		}

	}

	public void handleMessage(BankTransaction bankTransaction) {
		// TODO
	}

	public void handleMessage(CashPosition cashPosition) {

		log.info("\nBalance: " + cashPosition.getBalance() + " for broker " + cashPosition.getBroker().getUsername());
		// update balance, if such broker exists
		VisualBroker visualBroker = helper.findVisualBroker(cashPosition.getBroker());

		if (visualBroker != null) {
			visualBroker.updateCashBalance(cashPosition.getBalance());

			// sort brokerslist
			// Collections.sort(visualizerBean.getBrokers(), new
			// BrokerCashComparator());
			// sort gencos
			// Collections.sort(visualizerBean.getGencos(), new
			// GencoCashComparator());

		}

	}

	public void handleMessage(Order order) {

		log.info("\nBroker: " + order.getBroker() + "\nLimit Price: " + order.getLimitPrice() + "\nMWh: "
				+ order.getMWh() + " Timeslot\n Serial Number: " + order.getTimeslot().getSerialNumber() + " Index: "
				+ helper.computeRelativeTimeslotIndex(order.getTimeslot().getStartInstant()));
		// TODO
	}

	public void handleMessage(ClearedTrade clearedTrade) {
		int dateExecuted = helper.computeRelativeTimeslotIndex(clearedTrade.getDateExecuted());
		log.info("\nTimeslot\n Serial number: " + clearedTrade.getTimeslot().getSerialNumber() + " Index:"
				+ helper.computeRelativeTimeslotIndex(clearedTrade.getTimeslot().getStartInstant())
				+ "\nExecutionPrice:" + clearedTrade.getExecutionPrice() + " ExecutionMWh"
				+ clearedTrade.getExecutionMWh() + " DateExecuted (timeslot index):" + dateExecuted);
		// TODO

	}

	public void handleMessage(MarketTransaction marketTransaction) {
		log.info("\nBroker: " + marketTransaction.getBroker().getUsername() + " MWh: " + marketTransaction.getMWh()
				+ "\n Price: " + marketTransaction.getPrice() + " Postedtime: " + marketTransaction.getPostedTime()
				+ " Timeslot\n Serial Number: " + marketTransaction.getTimeslot().getSerialNumber() + " Index: "
				+ helper.computeRelativeTimeslotIndex(marketTransaction.getTimeslot().getStartInstant()));

		// TODO

	}

	public void handleMessage(MarketPosition marketPosition) {

		log.info("\nBroker: " + marketPosition.getBroker() + " Overall Balance: " + marketPosition.getOverallBalance()
				+ "\n Timeslot:\n serialnumber: " + marketPosition.getTimeslot().getSerialNumber()
				+ " Timeslot\n Serial Number: " + marketPosition.getTimeslot().getSerialNumber() + " Index: "
				+ helper.computeRelativeTimeslotIndex(marketPosition.getTimeslot().getStartInstant()));
		// TODO

	}

	public void handleMessage(SimResume simResume) {
		// TODO

	}

	public void handleMessage(Orderbook orderbook) {

		SortedSet<OrderbookOrder> asks = orderbook.getAsks();
		SortedSet<OrderbookOrder> bids = orderbook.getBids();
		StringBuilder builder = new StringBuilder();
		builder.append("\nBids:\n");
		for (Iterator iterator = bids.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			builder.append("\nLimitPrice: " + orderbookOrder.getLimitPrice() + " MWh: " + orderbookOrder.getMWh());
		}
		builder.append("\nAsks:\n");
		for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			builder.append("\nLimitPrice: " + orderbookOrder.getLimitPrice() + " MWh: " + orderbookOrder.getMWh());
		}
		int dateExecutedTimeslotIndex = helper.computeRelativeTimeslotIndex(orderbook.getDateExecuted());
		builder.append("\n\n Clearing price: " + orderbook.getClearingPrice() + " DateExecuted timeslot index:"
				+ dateExecutedTimeslotIndex + "\nTimeslot\n Serial Number: "
				+ orderbook.getTimeslot().getSerialNumber() + " Index: "
				+ helper.computeRelativeTimeslotIndex(orderbook.getTimeslot().getStartInstant()));

		log.info(builder.toString());

		// TODO

	}

	public void handleMessage(TariffTransaction tariffTransaction) {
		log.info("Broker: " + tariffTransaction.getBroker() + " Charge: " + tariffTransaction.getCharge()
				+ " CustomerCount: " + tariffTransaction.getCustomerCount() + "\n KWh: " + tariffTransaction.getKWh()
				+ " CustomerInfo: " + tariffTransaction.getCustomerInfo() + "Posted time: "
				+ tariffTransaction.getPostedTime() + "\n TxType: " + tariffTransaction.getTxType());
		// broker, not genco:
		BrokerModel brokerModel = helper.findBrokerModel(tariffTransaction.getBroker());
		if (brokerModel != null) {
			brokerModel.addTariffTransaction(tariffTransaction);
		}

	}

	public void handleMessage(DistributionTransaction distributionTransaction) {
		log.info("Broker: " + distributionTransaction.getBroker() + "\nCharge: " + distributionTransaction.getCharge()
				+ "\nkWh: " + distributionTransaction.getKWh() + "\nPostedTime timeslot index: "
				+ helper.computeRelativeTimeslotIndex(distributionTransaction.getPostedTime()));
	}

	public void handleMessage(BalancingTransaction balancingTransaction) {
		log.info("Broker: " + balancingTransaction.getBroker() + "\nCharge: " + balancingTransaction.getCharge()
				+ "\nkWh: " + balancingTransaction.getKWh() + "\nPostedTime timeslot index: "
				+ helper.computeRelativeTimeslotIndex(balancingTransaction.getPostedTime()));

		VisualBroker visualBroker = helper.findVisualBroker(balancingTransaction.getBroker());
		if (visualBroker != null) {
			visualBroker.addBalancingTransaction(balancingTransaction);
		}
	}
}
