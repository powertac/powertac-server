package org.powertac.visualizer.statistical;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.MarketTransaction;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.springframework.context.annotation.Profile;

/**
 * Holds broker's transactions made in current timeslot for available future
 * timeslots (24 of them).
 * */
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

	@Override
	public void update(int tsIndex, double energy, double cash) {
		if (!this.getDynamicDataMap().containsKey(tsIndex)) {
			this.setLastDynamicData(new DynamicData(tsIndex, 0, 0));
			this.getDynamicDataMap().put(tsIndex, this.getLastDynamicData());

		}
		this.getDynamicDataMap().get(tsIndex).update(energy, cash);
	}

	public void updateAccounts(int tsIndex) {

		if (getDynamicDataMap().containsKey(tsIndex)) {
			getDynamicDataMap().get(tsIndex).setProfit(profit);
			getDynamicDataMap().get(tsIndex).setEnergy(energy);
			profit += getDynamicDataMap().get(tsIndex).getProfitDelta();
			energy += getDynamicDataMap().get(tsIndex).getEnergyDelta();
		}

	}

}
