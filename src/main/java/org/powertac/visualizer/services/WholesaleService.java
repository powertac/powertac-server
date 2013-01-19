package org.powertac.visualizer.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.Instant;
import org.powertac.common.ClearedTrade;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.interfaces.WholesaleServiceBeanAccess;
import org.powertac.visualizer.statistical.SingleTimeslotWholesaleData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WholesaleService implements Serializable, Recyclable,
		TimeslotCompleteActivation, WholesaleServiceBeanAccess {

	private static final long serialVersionUID = 1L;
	private ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> clearedTrades;
	@Autowired
	private BrokerService brokerService;
	@Autowired
	private VisualizerBean visualizerBean;

	public WholesaleService() {
		recycle();
	}

	public void recycle() {
		clearedTrades = new ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>>(
				2000, 0.75f, 1);
 
	}

	public ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> getClearedTrades() {
		return clearedTrades;
	}

	public ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> getFinalClearedTrades() {
		ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> finalClearedTrades = new ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>>();

		TimeslotUpdate timeslotUpdate = visualizerBean.getTimeslotUpdate();

		if (timeslotUpdate != null) {
			// this should get all the final cleared trades
			SortedSet<Long> keys = new TreeSet<Long>(clearedTrades.keySet())
					.headSet(timeslotUpdate.getPostedTime().getMillis());
			for (Long key : keys) {
				finalClearedTrades.put(key, clearedTrades.get(key));
			}
		}
		return finalClearedTrades;

	}

	@Override
	public void activate(int timeslotIndex, Instant postedTime) {
	}

}
