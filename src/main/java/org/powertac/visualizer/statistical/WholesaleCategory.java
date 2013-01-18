package org.powertac.visualizer.statistical;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.MarketTransaction;
import org.powertac.visualizer.domain.broker.BrokerModel;

public class WholesaleCategory extends AbstractPerformanceCategory {

	public WholesaleCategory(BrokerModel broker) {
		super(broker);
		// TODO Auto-generated constructor stub
	}

	private ConcurrentHashMap<Integer, List<MarketTransaction>> marketTxs = new ConcurrentHashMap<Integer, List<MarketTransaction>>(
			24, 0.75f, 1);
	
	public ConcurrentHashMap<Integer, List<MarketTransaction>> getMarketTxs() {
		return marketTxs;
	}

}
