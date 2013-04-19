package org.powertac.visualizer.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.Instant;
import org.powertac.common.ClearedTrade;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.push.StatisticsPusher;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WholesaleService implements Serializable, Recyclable,
		TimeslotCompleteActivation {

	private static final long serialVersionUID = 1L;
	private ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> clearedTrades;
	@Autowired
	private BrokerService brokerService;
	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private PushService pushService;
	@Autowired
	private VisualizerHelperService visualizerHelperService;
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


	//called when TimeslotCompleteActivation message is received, used for dynamically adding points to wholesale graph
	//displaying average clearing price and amount of traded energy in timeslot
	@Override
	public void activate(int timeslotIndex, Instant postedTime) {
		ArrayList<StatisticsPusher> statisticsPushers = new ArrayList<StatisticsPusher>();
		double totalRevenue = 0;
		double totalEnergy = 0;
		int numberOfTransactions = 0;
		//get all cleared trades for timeslot with timeslot index = safety
		ConcurrentHashMap<Long, ClearedTrade> clearedTradesInSafetyTimeslot = clearedTrades.get(visualizerHelperService.getMillisForIndex(visualizerHelperService.getSafetyWholesaleTimeslotIndex()));
		//calculate total revenue and amount of traded energy for safety timeslot and count number of transactions in that timeslot
		for (ClearedTrade clearedTrade : clearedTradesInSafetyTimeslot.values()){
			totalRevenue += clearedTrade.getExecutionPrice();
			totalEnergy += clearedTrade.getExecutionMWh();
			numberOfTransactions++;
		}
		statisticsPushers.add(new StatisticsPusher(visualizerHelperService.getMillisForIndex(visualizerHelperService.getSafetyWholesaleTimeslotIndex()), totalRevenue/numberOfTransactions,  totalEnergy));
		pushService.pushWholesaleAvg(statisticsPushers);
	
		
	}

}
