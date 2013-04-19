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
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.domain.wholesale.VisualizerOrderbook;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.json.WholesaleServiceJSON;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.WholesaleService;
import org.powertac.visualizer.statistical.DynamicData;
import org.powertac.visualizer.statistical.TariffCategory;
import org.powertac.visualizer.statistical.WholesaleCategory;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
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
	private BrokerService brokerService;
	@Autowired
	private VisualizerHelperService helper;

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(Order.class, Orderbook.class,
				ClearedTrade.class, MarketPosition.class,
				MarketTransaction.class)) {
			router.registerMessageHandler(this, clazz);
		}

	}

	/**
	 * Handles message received when wholesale market transaction has been made.
	 * Updates timeslot in marketTxs with received data, and adds received transaction to list of transactions. 
	 * */
	public void handleMessage(MarketTransaction msg) {

		BrokerModel broker = brokerService.getBrokersMap().get(
				msg.getBroker().getUsername());

		if (broker != null) {
			WholesaleCategory wc = broker.getWholesaleCategory();

			int tsIndex = msg.getTimeslot().getSerialNumber();
			wc.update(tsIndex, msg.getMWh(), msg.getPrice()*msg.getMWh());
			
			wc.getMarketTxs().putIfAbsent(msg.getTimeslot().getSerialNumber(), new ArrayList<MarketTransaction>(24));
			wc.getMarketTxs().get(msg.getTimeslot().getSerialNumber()).add(msg);
		}
	}

	public void handleMessage(MarketPosition marketPosition) {

	}

	public void handleMessage(Order order) {

		// BrokerModel broker = brokerService.getBrokersMap().get(
		// order.getBroker().getUsername());
		// order.getTimeslot().getStartInstant().getMillis();
		//
		// if (broker != null) {
		// WholesaleCategory cat = broker.getWholesaleCategory();
		//
		// }

	}

	public void handleMessage(Orderbook orderbook) {
	}

	public void handleMessage(ClearedTrade ct) {
		ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> map = wholesaleService.getClearedTrades();
		// if there is a new key:
		map.putIfAbsent(ct.getTimeslot().getStartInstant().getMillis(),
				new ConcurrentHashMap<Long, ClearedTrade>(24, 0.75f, 1));

		TimeslotUpdate old = visualizerBean.getOldTimeslotUpdate();

		if (old != null) {
			map.get(ct.getTimeslot().getStartInstant().getMillis()).put(
					old.getPostedTime().getMillis(), ct);
		} else {
			log.warn("The old timeslot update does not exist.");
		}

	}

}
