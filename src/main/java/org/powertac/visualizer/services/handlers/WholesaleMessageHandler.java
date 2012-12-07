package org.powertac.visualizer.services.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.powertac.common.ClearedTrade;
import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.wholesale.VisualizerOrderbook;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.json.WholesaleServiceJSON;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.WholesaleService;
import org.powertac.visualizer.statistical.WholesaleCategory;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

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
	@Autowired
	BrokerService brokerService;

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(Order.class, Orderbook.class,
				ClearedTrade.class, MarketPosition.class,
				MarketTransaction.class)) {
			router.registerMessageHandler(this, clazz);
		}

	}

	public void handleMessage(MarketTransaction tx) {

		BrokerModel broker = brokerService.getBrokersMap().get(
				tx.getBroker().getUsername());
		tx.getTimeslot().getStartInstant().getMillis();

		if (broker != null) {
			WholesaleCategory cat = broker.getWholesaleCategory();
			
			TimeslotUpdate old = visualizerBean.getOldTimeslotUpdate();
			
			if(old!=null){
			cat.processMarketTransaction(tx, old.getPostedTime().getMillis());
			// NOTE: a market transaction will refer to a order that has been
			// posted the timeslot before.
			}
			else{
				log.warn("The old timeslot update does not exist.");
			}
		}
	}

	public void handleMessage(MarketPosition marketPosition) {

	}

	public void handleMessage(Order order) {

		BrokerModel broker = brokerService.getBrokersMap().get(
				order.getBroker().getUsername());
		order.getTimeslot().getStartInstant().getMillis();

		if (broker != null) {
			WholesaleCategory cat = broker.getWholesaleCategory();
			cat.processOrder(order, visualizerBean.getTimeslotUpdate()
					.getPostedTime().getMillis());
		}

	}

	public void handleMessage(Orderbook orderbook) {
	}

	public void handleMessage(ClearedTrade clearedTrade) {
		ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> map = wholesaleService
				.getClearedTrades();
		// if there is a new key:
		map.putIfAbsent(clearedTrade.getTimeslot().getStartInstant()
				.getMillis(), new ConcurrentHashMap<Long, ClearedTrade>(24,
				0.75f, 1));

		TimeslotUpdate old = visualizerBean.getOldTimeslotUpdate();

		if (old != null) {
			map.get(clearedTrade.getTimeslot().getStartInstant().getMillis())
					.put(old.getPostedTime().getMillis(), clearedTrade);
		} else{
			log.warn("The old timeslot update does not exist.");
		}

	}

}
