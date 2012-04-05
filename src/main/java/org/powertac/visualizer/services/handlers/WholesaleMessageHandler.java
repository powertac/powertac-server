package org.powertac.visualizer.services.handlers;

import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.WholesaleService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WholesaleMessageHandler implements Initializable {

	private Logger log = Logger.getLogger(WholesaleMessageHandler.class);

	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private MessageDispatcher router;
	@Autowired
	private WholesaleService wholesaleService;

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(Order.class, Orderbook.class, ClearedTrade.class)) {
			router.registerMessageHandler(this, clazz);
		}

	}

	public void handleMessage(Order order) {

		log.debug("\nBroker: " + order.getBroker() + "\nLimit Price: " + order.getLimitPrice() + "\nMWh: "
				+ order.getMWh() + " Timeslot\n Serial Number: " + order.getTimeslot().getSerialNumber());

		int currentTimeslot = visualizerBean.getCurrentTimeslotSerialNumber();
		// wholesale model:
		int timeslotSerialNumber = order.getTimeslot().getSerialNumber();
		// create new wholesale market if it doesn't exists
		if (!wholesaleService.getWholesaleMarkets().containsKey(timeslotSerialNumber)) {
			wholesaleService.getWholesaleMarkets().put(timeslotSerialNumber, new WholesaleMarket(timeslotSerialNumber));
		}
		WholesaleMarket wholesaleMarket = wholesaleService.findWholesaleMarket(timeslotSerialNumber);
		// same stuff for the snapshot:
		if (!wholesaleMarket.getSnapshotsMap().containsKey(currentTimeslot)) {
			wholesaleMarket.getSnapshotsMap().put(currentTimeslot,
					new WholesaleSnapshot(order.getTimeslot(), currentTimeslot));
		}
		WholesaleSnapshot snapshot = wholesaleMarket.findSnapshot(currentTimeslot);
		snapshot.addOrder(order);
	}

	public void handleMessage(Orderbook orderbook) {

		SortedSet<OrderbookOrder> asks = orderbook.getAsks();
		SortedSet<OrderbookOrder> bids = orderbook.getBids();
		StringBuilder builder = new StringBuilder();
		builder.append("\nBids:\n");
		for (Iterator<OrderbookOrder> iterator = bids.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			builder.append("\nLimitPrice: " + orderbookOrder.getLimitPrice() + " mWh: " + orderbookOrder.getMWh());
		}
		builder.append("\nAsks:\n");
		for (Iterator<OrderbookOrder> iterator = asks.iterator(); iterator.hasNext();) {
			OrderbookOrder orderbookOrder = (OrderbookOrder) iterator.next();
			builder.append("\nLimitPrice: " + orderbookOrder.getLimitPrice() + " mWh: " + orderbookOrder.getMWh());
		}

		builder.append("\n\n Clearing price: " + orderbook.getClearingPrice() + "\nTimeslot\n Serial Number: "
				+ orderbook.getTimeslot().getSerialNumber());

		// wholesale model:
		// orderbook and cleared trade are received one timeslot later than
		// correspondent orders:
		int targetTimeslotIndex = visualizerBean.getCurrentTimeslotSerialNumber() - 1;

		WholesaleMarket market = wholesaleService.findWholesaleMarket(orderbook.getTimeslot().getSerialNumber());
		WholesaleSnapshot snapshot = market.findSnapshot(targetTimeslotIndex);
		snapshot.setOrderbook(orderbook);
		// the end for this snapshot if there is null clearing price:
		if (orderbook.getClearingPrice() == null) {
			snapshot.close();
			checkWholesaleMarket(market);
		}
		log.debug(builder.toString());

	}

	private void checkWholesaleMarket(WholesaleMarket market) {
		// what about market? should be closed when all of its snapshots
		// have been closed and when its timeslot equals the current
		// timeslot
		int offset = market.getTimeslotSerialNumber() - visualizerBean.getCurrentTimeslotSerialNumber();
		if (offset == 0) {
			market.close();
			// update model:
			wholesaleService.addTradedQuantityMWh(market.getTotalTradedQuantityMWh());
		}

	}

	public void handleMessage(ClearedTrade clearedTrade) {
		log.debug("\nTimeslot\n Serial number: " + clearedTrade.getTimeslot().getSerialNumber() + "\nExecutionPrice:"
				+ clearedTrade.getExecutionPrice() + " ExecutionMWh" + clearedTrade.getExecutionMWh());

		// wholesale model:
		// orderbook and cleared trade are received one timeslot later than
		// correspondent orders:
		int targetTimeslotIndex = visualizerBean.getCurrentTimeslotSerialNumber() - 1;
		

		WholesaleMarket market = wholesaleService.findWholesaleMarket(clearedTrade.getTimeslot().getSerialNumber());
		WholesaleSnapshot snapshot = market.findSnapshot(targetTimeslotIndex);
		snapshot.setClearedTrade(clearedTrade);
		// the end for this snapshot:
		snapshot.close();
		// what about market? should be closed when all of its snapshots have
		// been closed and when its timeslot equals the current timeslot
		checkWholesaleMarket(market);

	}

}
