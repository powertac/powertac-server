package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.powertac.visualizer.display.GameOverviewTemplate;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.DynamicData;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class GameOverviewBean implements Serializable {

	private static final long serialVersionUID = 1L;
	//private Logger log = Logger.getLogger(GameOverviewBean.class);
	private String gameOverview;

	@Autowired
	public GameOverviewBean(BrokerService brokerService,
			VisualizerHelperService helper) {

		Gson gson = new Gson();
		createMostRecentOverview(gson, brokerService, helper);

	}

	private void createMostRecentOverview(Gson gson, BrokerService brokerService,
			 VisualizerHelperService helper) {
		
		Collection<BrokerModel> brokers = brokerService.getBrokers();
		ArrayList<Object> allBrokersData = new ArrayList<Object>();
		
		for (Iterator<BrokerModel> iterator = brokers.iterator(); iterator.hasNext();) {
			ArrayList<Double> data = new ArrayList<Double>();
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			data.add(brokerModel
					.getFinanceCategory().getLastFinanceDynamicData().getProfit());
			data.add(brokerModel
					.getTariffCategory().getLastTariffDynamicData().getDynamicData().getProfit());
			
			NavigableSet<Integer> safeKeys = new TreeSet<Integer>(brokerModel.getWholesaleCategory()
					.getDynamicDataMap().keySet()).headSet(helper.getSafetyWholesaleTimeslotIndex(), true);
			if (!safeKeys.isEmpty()) {
				DynamicData lastWholesaledd = brokerModel.getWholesaleCategory().getDynamicDataMap().get(
						safeKeys.last());
				double profit = lastWholesaledd.getProfit();	
				data.add(profit);
			}
			data.add(brokerModel.getBalancingCategory().getLastDynamicData().getProfit());
			data.add(brokerModel.getDistributionCategory().getLastDynamicData().getProfit());
			allBrokersData.add(new GameOverviewTemplate(brokerModel.getName(), data));
		}
		
		this.gameOverview = gson.toJson(allBrokersData);
	}

	public String getGameOverview() {
		return gameOverview;
	}

}
