package org.powertac.visualizer.user;

import com.google.gson.Gson;
import org.powertac.visualizer.display.GameOverviewTemplate;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.DynamicData;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.NavigableSet;
import java.util.TreeSet;


public class GameOverviewBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private String gameOverview;

	@Autowired
	public GameOverviewBean(BrokerService brokerService,
			VisualizerHelperService helper)
  {
		Gson gson = new Gson();
		createMostRecentOverview(gson, brokerService, helper);
	}

	private void createMostRecentOverview(Gson gson, BrokerService brokerService,
			 VisualizerHelperService helper)
  {
		ArrayList<Object> allBrokersData = new ArrayList<Object>();

    for (BrokerModel broker : brokerService.getBrokersMap().values()) {
      ArrayList<Double> data = new ArrayList<Double>();
      data.add(broker
          .getFinanceCategory().getLastFinanceDynamicData().getProfit());
      data.add(broker
          .getTariffCategory().getLastTariffDynamicData().getDynamicData().getProfit());

      NavigableSet<Integer> safeKeys = new TreeSet<Integer>(broker.getWholesaleCategory()
          .getDynamicDataMap().keySet()).headSet(helper.getSafetyWholesaleTimeslotIndex(), true);
      if (!safeKeys.isEmpty()) {
        DynamicData lastWholesaledd = broker.getWholesaleCategory().getDynamicDataMap().get(
            safeKeys.last());
        double profit = lastWholesaledd.getProfit();
        data.add(profit);
      }
      data.add(broker.getBalancingCategory().getLastDynamicData().getProfit());
      data.add(broker.getDistributionCategory().getLastDynamicData().getProfit());
      allBrokersData.add(new GameOverviewTemplate(broker.getName(), data));
    }
		
		this.gameOverview = gson.toJson(allBrokersData);
	}

	public String getGameOverview() {
		return gameOverview;
	}

}
