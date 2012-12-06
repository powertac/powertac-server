package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.ClearedTrade;
import org.powertac.common.OrderbookOrder;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.wholesale.VisualizerOrderbookOrder;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.WholesaleService;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.paginator.PaginatorElementRenderer;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class WholesaleBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String energyMostRecentClearingsJson;
	private String cashMostRecentClearingsJson;

	@Autowired
	public WholesaleBean(BrokerService brokerService,
			WholesaleService wholesaleService) {
		
		Gson gson = new Gson();

		// most recent clearings for each timeslot.
		ArrayList<Object> energyMostRecentClearings = new ArrayList<Object>();
		ArrayList<Object> cashMostRecentClearings = new ArrayList<Object>();
		
		ConcurrentHashMap<Long, ArrayList<ClearedTrade>> map = wholesaleService.getClearedTrades();
		
		SortedSet<Long> keys = new TreeSet<Long>(map.keySet());
		
		for(Long key:keys){
			ArrayList<ClearedTrade> clearedTrades = map.get(key);
			if (clearedTrades != null) {
				ClearedTrade mostRecentClearing = clearedTrades
						.get(clearedTrades.size() - 1);
				Object[] energy = { key,
						mostRecentClearing.getExecutionMWh() };
				Object[] cash = { key,
						mostRecentClearing.getExecutionPrice() };
				energyMostRecentClearings.add(energy);
				cashMostRecentClearings.add(cash);
			}
		}
		energyMostRecentClearingsJson = gson.toJson(energyMostRecentClearings);
		cashMostRecentClearingsJson = gson.toJson(cashMostRecentClearings);
	}
	
	public String getCashMostRecentClearingsJson() {
		return cashMostRecentClearingsJson;
	}
	
	public String getEnergyMostRecentClearingsJson() {
		return energyMostRecentClearingsJson;
	}

}
