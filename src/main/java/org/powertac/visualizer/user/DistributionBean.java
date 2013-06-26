package org.powertac.visualizer.user;

import com.google.gson.Gson;
import org.powertac.visualizer.display.BrokerSeriesTemplate;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.DynamicData;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;


public class DistributionBean implements Serializable
{
  private static final long serialVersionUID = 1L;
  private String distributionDynData;
  private String distributionDynDataOneTimeslot;

  @Autowired
  public DistributionBean (BrokerService brokerService,
                           VisualizerHelperService helper)
  {
    Gson gson = new Gson();
    createBrokerDistributionTransactions(gson, brokerService, helper);
  }

  private void createBrokerDistributionTransactions (Gson gson,
                                                     BrokerService brokerService,
                                                     VisualizerHelperService helper)
  {
    ArrayList<Object> distributionTxData = new ArrayList<Object>();
    ArrayList<Object> distributionTxDataOneTimeslot = new ArrayList<Object>();

    // brokers:
    for (BrokerModel brokerModel : brokerService.getBrokersMap().values()) {
      ArrayList<Object> profitData = new ArrayList<Object>();
      ArrayList<Object> netKwhData = new ArrayList<Object>();
      // one timeslot
      ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();
      ArrayList<Object> kwhDataOneTimeslot = new ArrayList<Object>();

      ConcurrentHashMap<Integer, DynamicData> dynDataMap = brokerModel
          .getDistributionCategory().getDynamicDataMap();
      SortedSet<Integer> dynDataSet = new TreeSet<Integer>(dynDataMap.keySet());

      // dynamic wholesale data:
      for (Integer key : dynDataSet) {
        DynamicData dynData = dynDataMap.get(key);

        Object[] profit = {helper.getMillisForIndex(key),
            dynData.getProfit()};
        Object[] netMwh = {helper.getMillisForIndex(key),
            dynData.getEnergy()};

        profitData.add(profit);
        netKwhData.add(netMwh);

        // one timeslot:
        Object[] profitOneTimeslot = {helper.getMillisForIndex(key),
            dynData.getProfitDelta()};
        Object[] kWhOneTimeslot = {helper.getMillisForIndex(key),
            dynData.getEnergyDelta()};
        profitDataOneTimeslot.add(profitOneTimeslot);
        kwhDataOneTimeslot.add(kWhOneTimeslot);
      }
      if (dynDataSet.size() == 0) {
        //dummy:
        double[] dummy = {helper.getMillisForIndex(0), 0};
        profitData.add(dummy);
        profitDataOneTimeslot.add(dummy);
        kwhDataOneTimeslot.add(dummy);
        netKwhData.add(dummy);
      }

      distributionTxData.add(new BrokerSeriesTemplate(brokerModel.getName()
          , brokerModel.getAppearance().getColorCode(), 0, //+ " PRICE"
          profitData, true));
      distributionTxData.add(new BrokerSeriesTemplate(brokerModel.getName()
          , brokerModel.getAppearance().getColorCode(), 1, //+ " KWH"
          netKwhData, false));

      // one timeslot:
      distributionTxDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
          .getName(), brokerModel.getAppearance() // + " PRICE"
          .getColorCode(), 0, profitDataOneTimeslot, true));
      distributionTxDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
          .getName(), brokerModel.getAppearance() // + " KWH"
          .getColorCode(), 1, kwhDataOneTimeslot, false));
    }
    this.distributionDynData = gson.toJson(distributionTxData);
    this.distributionDynDataOneTimeslot = gson
        .toJson(distributionTxDataOneTimeslot);
  }

  public String getDistributionDynData ()
  {
    return distributionDynData;
  }

  public String getDistributionDynDataOneTimeslot ()
  {
    return distributionDynDataOneTimeslot;
  }
}
