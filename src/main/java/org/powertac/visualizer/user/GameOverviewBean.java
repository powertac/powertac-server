package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.powertac.visualizer.display.GameOverviewTemplate;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.GradingService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.TariffCategory;
import org.powertac.visualizer.statistical.WholesaleCategory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class GameOverviewBean implements Serializable
{

  private static final long serialVersionUID = 1L;
  private String gameOverview;

  @Autowired
  public GameOverviewBean (BrokerService brokerService,
                           VisualizerHelperService helper,
                           GradingService gradingBean)
  {

    Gson gson = new Gson();
    createMostRecentOverview(gson, brokerService, helper, gradingBean);

  }

  private void createMostRecentOverview (Gson gson,
                                         BrokerService brokerService,
                                         VisualizerHelperService helper,
                                         GradingService gradingBean)
  {

    Collection<BrokerModel> brokers = brokerService.getBrokers();

    Iterator<BrokerModel> brokerIterator = brokers.iterator();
    double highestEnergyAmount =
      brokerIterator.next().getDistributionCategory().getLastDynamicData()
              .getEnergy();
    while (brokerIterator.hasNext()) {
      double energy =
        brokerIterator.next().getDistributionCategory().getLastDynamicData()
                .getEnergy();
      if (energy > highestEnergyAmount)
        highestEnergyAmount = energy;
    }

    ArrayList<Object> allBrokersData = new ArrayList<Object>();

    for (Iterator<BrokerModel> iterator = brokers.iterator(); iterator
            .hasNext();) {
      ArrayList<Double> data = new ArrayList<Double>();
      BrokerModel brokerModel = (BrokerModel) iterator.next();
      TariffCategory tc = brokerModel.getTariffCategory();

      data.add(gradingBean.getTariffGrade(tc.getTotalMoneyFlow(),
                                          tc.getConsumptionConsumers(),
                                          tc.getTotalMoneyFromSold(),
                                          tc.getTotalBoughtEnergy(),
                                          tc.getTotalSoldEnergy(),
                                          tc.getCustomerCount(),
                                          tc.getLostCustomers()));
      // wholesale grade
      WholesaleCategory wc = brokerModel.getWholesaleCategory();
      data.add(gradingBean.getWholesaleGrade(wc.getTotalRevenueFromSelling(),
                                             wc.getTotalCostFromBuying(),
                                             wc.getTotalEnergyBought(),
                                             wc.getTotalEnergySold()));
      // balancing grade
      data.add(gradingBean.getBalancingGrade(brokerModel.getBalancingCategory()
              .getEnergy(), brokerModel.getDistributionCategory()
              .getLastDynamicData().getEnergy(), brokerModel
              .getBalancingCategory().getProfit()));
      // distribution grade
      data.add(gradingBean.getDistributionGrade(brokerModel
              .getDistributionCategory().getLastDynamicData().getEnergy()));
      allBrokersData.add(new GameOverviewTemplate(brokerModel.getName(), data));
    }

    this.gameOverview = gson.toJson(allBrokersData);
  }

  public String getGameOverview ()
  {
    return gameOverview;
  }

}
