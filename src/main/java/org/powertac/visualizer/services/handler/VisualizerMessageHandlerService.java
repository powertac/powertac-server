package org.powertac.visualizer.services.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.joda.time.field.OffsetDateTimeField;
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
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherForecast;
import org.powertac.common.WeatherReport;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.BrokerModel;
import org.powertac.visualizer.domain.DayOverview;
import org.powertac.visualizer.domain.DayState;
import org.powertac.visualizer.domain.GencoModel;
import org.powertac.visualizer.domain.VisualBroker;
import org.powertac.visualizer.wholesale.WholesaleMarket;
import org.powertac.visualizer.wholesale.WholesaleModel;
import org.powertac.visualizer.wholesale.WholesaleSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.xml.bind.v2.runtime.unmarshaller.LocatorEx.Snapshot;

@Service
public class VisualizerMessageHandlerService {

	private Logger log = Logger.getLogger(VisualizerMessageHandlerService.class);
	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private VisualizerMessageHandlerHelperService helper;
	@Autowired
	private AppearanceListBean appearanceListBean;

	// relative index = timeslot's serial number - 360; because game actually
	// starts at 360th timeslot number.
	private static final int timeslotOffset = 360;

	public void handleMessage(Competition competition) {

		if (competition != null) {
			visualizerBean.setCompetition(competition);

			// Build a list of brokers
			List<BrokerModel> brokers = helper.buildBrokerList(competition);

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

		// for the first timeslot:
		if (visualizerBean.getFirstTimeslotInstant() == null)
			visualizerBean.setFirstTimeslotInstant(timeslotUpdate.getPostedTime());

		ArrayList<Timeslot> enabledTimeslots = timeslotUpdate.getEnabled();

		StringBuilder builder = new StringBuilder();
		builder.append("Enabled timeslots:");
		for (Iterator<Timeslot> iterator = enabledTimeslots.iterator(); iterator.hasNext();) {
			Timeslot timeslot = (Timeslot) iterator.next();
			builder.append(" ").append(timeslot.getSerialNumber());
		}

		log.info(builder + "\n");

		int relativeTimeslotIndex = helper.computeRelativeTimeslotIndex(timeslotUpdate.getPostedTime());
		// for all brokers and gencos:
		helper.updateTimeslotIndex(relativeTimeslotIndex);
		// update for visualizerBean:
		visualizerBean.setRelativeTimeslotIndex(relativeTimeslotIndex);

		// new day? if so, make new day overview:
		if (relativeTimeslotIndex != 0 && relativeTimeslotIndex % 24 == 0) {
			helper.buildDayOverview();
		}

		// update global charts each timeslot:
		helper.updateGlobalCharts();

		log.info("\nTimeslot index: " + relativeTimeslotIndex + "\nPostedtime:" + timeslotUpdate.getPostedTime());

		// wholesale update:

	}

	public void handleMessage(SimStart simStart) {
		log.info("\nINSTANT: " + simStart.getStart());
		// TODO

	}

	public void handleMessage(SimPause simPause) {
		// TODO
	}

	public void handleMessage(WeatherReport weatherReport) {

		if (weatherReport.getCurrentTimeslot() != null) {
			log.info("\nStart Instant: " + weatherReport.getCurrentTimeslot().getStartInstant());
			visualizerBean.setWeatherReport(weatherReport);
		} else {
			log.warn("Timeslot for Weather report object is null!!");
		}

		log.info("CLOUD COVER: " + weatherReport.getCloudCover() + " TEMP: " + weatherReport.getTemperature()
				+ "W DIR:" + weatherReport.getWindDirection() + "W SPEED:" + weatherReport.getWindSpeed());

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
				+ tariffSpecification.getPowerType() + " ID: " + tariffSpecification.getId());

		if (tariffSpecification.getSupersedes() != null) {
			log.info("NO of tariffspec:" + tariffSpecification.getSupersedes().size());
		}

		List<Rate> rates = tariffSpecification.getRates();
		String ispis = "";
		for (Iterator iterator = rates.iterator(); iterator.hasNext();) {
			Rate rate = (Rate) iterator.next();
			ispis += "" + rate.toString();
		}
		log.info("RATE:\n" + ispis);

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

		int relativeTimeslotIndex = visualizerBean.getRelativeTimeslotIndex();
		// wholesale model:
		WholesaleModel wholesaleModel = visualizerBean.getWholesaleModel();
		int timeslotSerialNumber = order.getTimeslot().getSerialNumber();
		// create new wholesale market if it doesn't exists
		if (!wholesaleModel.getWholesaleMarkets().containsKey(timeslotSerialNumber)) {
			wholesaleModel.getWholesaleMarkets().put(timeslotSerialNumber, new WholesaleMarket(timeslotSerialNumber));
		}
		WholesaleMarket wholesaleMarket = wholesaleModel.findWholesaleMarket(timeslotSerialNumber);
		// same stuff for the snapshot:
		if (!wholesaleMarket.getSnapshotsMap().containsKey(relativeTimeslotIndex)) {
			wholesaleMarket.getSnapshotsMap().put(
					relativeTimeslotIndex,
					new WholesaleSnapshot(order.getTimeslot(), relativeTimeslotIndex, timeslotOffset
							+ relativeTimeslotIndex));
		}
		WholesaleSnapshot snapshot = wholesaleMarket.findSnapshot(relativeTimeslotIndex);
		snapshot.addOrder(order);
	}

	public void handleMessage(Orderbook orderbook) {

		SortedSet<OrderbookOrder> asks = orderbook.getAsks();
		SortedSet<OrderbookOrder> bids = orderbook.getBids();
		StringBuilder builder = new StringBuilder();
		builder.append("\nBids:\n");
		for (Iterator iterator = bids.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			builder.append("\nLimitPrice: " + orderbookOrder.getLimitPrice() + " mWh: " + orderbookOrder.getMWh());
		}
		builder.append("\nAsks:\n");
		for (Iterator iterator = asks.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			builder.append("\nLimitPrice: " + orderbookOrder.getLimitPrice() + " mWh: " + orderbookOrder.getMWh());
		}
		int dateExecutedTimeslotIndex = helper.computeRelativeTimeslotIndex(orderbook.getDateExecuted());
		builder.append("\n\n Clearing price: " + orderbook.getClearingPrice() + " DateExecuted timeslot index:"
				+ dateExecutedTimeslotIndex + "\nTimeslot\n Serial Number: "
				+ orderbook.getTimeslot().getSerialNumber() + " Index: "
				+ helper.computeRelativeTimeslotIndex(orderbook.getTimeslot().getStartInstant()));

		// wholesale model:
		// orderbook and cleared trade are received one timeslot later than
		// correspondent orders:
		int targetRelativeTimeslotIndex = visualizerBean.getRelativeTimeslotIndex() - 1;
		WholesaleModel model = visualizerBean.getWholesaleModel();

		WholesaleMarket market = model.findWholesaleMarket(orderbook.getTimeslot().getSerialNumber());
		WholesaleSnapshot snapshot = market.findSnapshot(targetRelativeTimeslotIndex);
		snapshot.setOrderbook(orderbook);
		// the end for this snapshot if there is null clearing price:
		if (orderbook.getClearingPrice() == null) {
			snapshot.close();
			// what about market? should be closed when all of its snapshots
			// have been closed and when its timeslot equals the current
			// timeslot
			int offset = market.getTimeslotSerialNumber() - visualizerBean.getRelativeTimeslotIndex();
			if (offset == timeslotOffset) {
				market.close();
				// update model:
				model.addTradedQuantityMWh(market.getTotalTradedQuantityMWh());
			}
		}
		log.debug(builder.toString());

		// TODO

	}

	public void handleMessage(ClearedTrade clearedTrade) {
		int dateExecuted = helper.computeRelativeTimeslotIndex(clearedTrade.getDateExecuted());
		log.info("\nTimeslot\n Serial number: " + clearedTrade.getTimeslot().getSerialNumber() + " Index: "
				+ helper.computeRelativeTimeslotIndex(clearedTrade.getTimeslot().getStartInstant())
				+ "\nExecutionPrice:" + clearedTrade.getExecutionPrice() + " ExecutionMWh"
				+ clearedTrade.getExecutionMWh() + " DateExecuted (timeslot index):" + dateExecuted);

		// wholesale model:
		// orderbook and cleared trade are received one timeslot later than
		// correspondent orders:
		int targetRelativeTimeslotIndex = visualizerBean.getRelativeTimeslotIndex() - 1;
		WholesaleModel model = visualizerBean.getWholesaleModel();

		WholesaleMarket market = model.findWholesaleMarket(clearedTrade.getTimeslot().getSerialNumber());
		WholesaleSnapshot snapshot = market.findSnapshot(targetRelativeTimeslotIndex);
		snapshot.setClearedTrade(clearedTrade);
		// the end for this snapshot:
		snapshot.close();
		// what about market? should be closed when all of its snapshots have
		// been closed and when its timeslot equals the current timeslot
		int offset = market.getTimeslotSerialNumber() - visualizerBean.getRelativeTimeslotIndex();
		log.info(offset);
		if (offset == timeslotOffset) {
			market.close();
			// update model:
			model.addTradedQuantityMWh(market.getTotalTradedQuantityMWh());

		}

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

	public void handleMessage(TariffTransaction tariffTransaction) {
		log.info("Broker: " + tariffTransaction.getBroker() + " Charge: " + tariffTransaction.getCharge()
				+ " CustomerCount: " + tariffTransaction.getCustomerCount() + "\n KWh: " + tariffTransaction.getKWh()
				+ " CustomerInfo: " + tariffTransaction.getCustomerInfo() + "Posted time: "
				+ tariffTransaction.getPostedTime() + "\n TxType: " + tariffTransaction.getTxType());
		// broker, not genco:
		BrokerModel brokerModel = helper.findBrokerModel(tariffTransaction.getBroker());
		if (brokerModel != null) {
			brokerModel.addTariffTransaction(tariffTransaction);
			// update overall status for customers:
			visualizerBean.getCustomerModel().addTariffTransaction(tariffTransaction);
		}

	}

	public void handleMessage(DistributionTransaction distributionTransaction) {
		log.info("Broker: " + distributionTransaction.getBroker() + "\nCharge: " + distributionTransaction.getCharge()
				+ "\nkWh: " + distributionTransaction.getKWh() + "\nPostedTime timeslot index: "
				+ helper.computeRelativeTimeslotIndex(distributionTransaction.getPostedTime()));

		// fix for brokers that do not receive balancing transaction (because
		// their distributionTransaction is 0 KWh!)
		if (distributionTransaction.getKWh() == 0) {

			BrokerModel brokerModel = helper.findBrokerModel(distributionTransaction.getBroker());
			if (brokerModel != null) {
				brokerModel.updateEnergyBalance(0);
			}
		}

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
