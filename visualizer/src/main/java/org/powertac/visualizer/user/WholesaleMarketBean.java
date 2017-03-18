package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.ClearedTrade;
import org.powertac.common.MarketTransaction;
import org.powertac.visualizer.display.BrokerSeriesTemplate;
import org.powertac.visualizer.display.WholesaleAnalysisTemplate;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.WholesaleService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.powertac.visualizer.statistical.DynamicData;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class WholesaleMarketBean implements Serializable
{

  private static final long serialVersionUID = 1L;

  private String energyMostRecentClearingsJson;
  private String cashMostRecentClearingsJson;

  private String clearedTradesDataJson;
  private String allMarketTransactionsData;

  private String wholesaleDynData;
  private String wholesaleDynDataOneTimeslot;
  private String wholesaleAverageTimeslotPriceData;// tom

  private final int TIMESLOTS_TO_DISPLAY = 48;

  @Autowired
  public WholesaleMarketBean (BrokerService brokerService,
                              WholesaleService wholesaleService,
                              VisualizerHelperService helper)
  {

    Gson gson = new Gson();

    createMostRecentClearings(gson, wholesaleService, helper);
    createAllClearings(gson, wholesaleService, helper);
    createBrokerWholesaleTransactions(gson, brokerService, helper);

  }

  private void
    createBrokerWholesaleTransactions (Gson gson, BrokerService brokerService,
                                       VisualizerHelperService helper)
  {
    Collection<BrokerModel> brokers = brokerService.getBrokers();

    int safetyTsIndex = helper.getSafetyWholesaleTimeslotIndex();
    ArrayList<Object> allWholesaleData = new ArrayList<Object>();
    ArrayList<Object> wholesaleTxData = new ArrayList<Object>();
    ArrayList<Object> wholesaleTxDataOneTimeslot = new ArrayList<Object>();

    // brokers:
    for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
      BrokerModel brokerModel = (BrokerModel) iterator.next();

      ArrayList<Object> profitData = new ArrayList<Object>();
      ArrayList<Object> netMwhData = new ArrayList<Object>();
      // one timeslot
      ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();
      ArrayList<Object> mwhDataOneTimeslot = new ArrayList<Object>();
      // market tx data
      ArrayList<Object> wholesaleTxBrokerData = new ArrayList<Object>();

      ConcurrentHashMap<Integer, DynamicData> dynDataMap =
        brokerModel.getWholesaleCategory().getDynamicDataMap();

      SortedSet<Integer> dynDataSet =
        new TreeSet<Integer>(brokerModel.getWholesaleCategory()
                .getDynamicDataMap().keySet()).headSet(safetyTsIndex, true);

      double totalProfit = 0;
      double totalEnergy = 0;
      // dynamic wholesale data:
      for (Iterator iterator2 = dynDataSet.iterator(); iterator2.hasNext();) {
        int key = (Integer) iterator2.next();
        DynamicData dynData = dynDataMap.get(key);

        totalEnergy += dynData.getEnergyDelta();
        totalProfit += dynData.getProfitDelta();

        Object[] profit = { helper.getMillisForIndex(key), totalProfit };
        Object[] netMwh = { helper.getMillisForIndex(key), totalEnergy };

        profitData.add(profit);
        netMwhData.add(netMwh);

        // one timeslot:
        Object[] profitOneTimeslot =
          { helper.getMillisForIndex(key), dynData.getProfitDelta() };
        Object[] kWhOneTimeslot =
          { helper.getMillisForIndex(key), dynData.getEnergyDelta() };
        profitDataOneTimeslot.add(profitOneTimeslot);
        mwhDataOneTimeslot.add(kWhOneTimeslot);
      }

      if (dynDataSet.size() == 0) {
        // dummy
        Object[] dummy = { helper.getMillisForIndex(0), 0 };
        profitData.add(dummy);
        netMwhData.add(dummy);
        profitDataOneTimeslot.add(dummy);
        mwhDataOneTimeslot.add(dummy);

      }

      ConcurrentHashMap<Integer, List<MarketTransaction>> mtxMap =
        brokerModel.getWholesaleCategory().getMarketTxs();
      SortedSet<Integer> mtxSortedSet =
        new TreeSet<Integer>(brokerModel.getWholesaleCategory().getMarketTxs()
                .keySet()).headSet(safetyTsIndex, true);

      SortedSet<Integer> mtxSortedSetSubset =
        mtxSortedSet
                .subSet(((safetyTsIndex - TIMESLOTS_TO_DISPLAY) < 0)? 0
                                                                    : safetyTsIndex
                                                                      - TIMESLOTS_TO_DISPLAY,
                        safetyTsIndex);// tom

      for (Iterator iterator2 = mtxSortedSetSubset.iterator(); iterator2
              .hasNext();) {
        int key = (Integer) iterator2.next();

        List<MarketTransaction> mtxList = mtxMap.get(key);
        for (Iterator iterator3 = mtxList.iterator(); iterator3.hasNext();) {
          MarketTransaction marketTransaction =
            (MarketTransaction) iterator3.next();
          Object[] mtxEntry =
            { marketTransaction.getPrice(), marketTransaction.getMWh() };
          wholesaleTxBrokerData.add(mtxEntry);
        }

      }

      wholesaleTxData.add(new BrokerSeriesTemplate(brokerModel.getName(),
                                                   brokerModel.getAppearance()
                                                           .getColorCode(), 0, // +
                                                                               // " PRICE"
                                                   profitData, true));
      wholesaleTxData.add(new BrokerSeriesTemplate(brokerModel.getName(),
                                                   brokerModel.getAppearance()
                                                           .getColorCode(), 1, // +
                                                                               // " MWH"
                                                   netMwhData, false));

      // one timeslot:
      wholesaleTxDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
              .getName(), brokerModel.getAppearance() // + " PRICE"
              .getColorCode(), 0, profitDataOneTimeslot, true));
      wholesaleTxDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
              .getName(), brokerModel.getAppearance() // + " MWH"
              .getColorCode(), 1, mwhDataOneTimeslot, false));
      allWholesaleData
              .add(new BrokerSeriesTemplate(brokerModel.getName(), brokerModel
                      .getAppearance().getColorCodeRGBShading(),
                                            wholesaleTxBrokerData, true));

    }
    this.wholesaleDynData = gson.toJson(wholesaleTxData);
    this.wholesaleDynDataOneTimeslot = gson.toJson(wholesaleTxDataOneTimeslot);
    this.allMarketTransactionsData = gson.toJson(allWholesaleData);
  }

  private void createAllClearings (Gson gson,
                                   WholesaleService wholesaleService,
                                   VisualizerHelperService helper)
  {
    ArrayList<Object> allClearedTrades = new ArrayList<Object>();
    ArrayList<Object> totalDataOneTimeslot = new ArrayList<Object>();// tom
    ArrayList<Object> averageProfitPerTimeslot = new ArrayList<Object>(); // tom
    // maps within the map
    Collection<ConcurrentHashMap<Long, ClearedTrade>> allTrades =
      wholesaleService.getClearedTrades().values();

    // Contains total amount of money payed in each timeslot.
    ConcurrentHashMap<Long, Double> totalPriceInTimeslot =
      new ConcurrentHashMap<Long, Double>();
    // Number of transactions in timeslot
    ConcurrentHashMap<Long, Integer> numberOfTransactions =
      new ConcurrentHashMap<Long, Integer>();
    // Contains total amount of energy traded in each timeslot.
    ConcurrentHashMap<Long, Double> totalEnergyInTimeslot =
      new ConcurrentHashMap<Long, Double>();

    for (Iterator iterator = allTrades.iterator(); iterator.hasNext();) {
      ConcurrentHashMap<Long, ClearedTrade> concurrentHashMap =
        (ConcurrentHashMap<Long, ClearedTrade>) iterator.next();
      // collection:
      Collection<ClearedTrade> trades = concurrentHashMap.values();
      for (Iterator iterator2 = trades.iterator(); iterator2.hasNext();) {
        ClearedTrade ct = (ClearedTrade) iterator2.next();

        Object[] entry = { ct.getExecutionMWh(), ct.getExecutionPrice() };
        allClearedTrades.add(entry);

        // If there is no entry for timeslot, create one; otherwise
        // update given timeslot with amount of money and
        // increase number of transactions in timeslot.
        if (!totalPriceInTimeslot.containsKey(helper.getMillisForIndex(ct
                .getTimeslotIndex()))) {
          totalPriceInTimeslot.put(helper.getMillisForIndex(ct
                  .getTimeslotIndex()), ct.getExecutionPrice());
          totalEnergyInTimeslot.put(helper.getMillisForIndex(ct
                  .getTimeslotIndex()), ct.getExecutionMWh());
          numberOfTransactions.put(helper.getMillisForIndex(ct
                  .getTimeslotIndex()), 1);
        }
        else {
          totalPriceInTimeslot.put(helper.getMillisForIndex(ct
                                           .getTimeslotIndex()),
                                   totalPriceInTimeslot.get(helper
                                           .getMillisForIndex(ct
                                                   .getTimeslotIndex()))
                                           + ct.getExecutionPrice());
          totalEnergyInTimeslot.put(helper.getMillisForIndex(ct
                                            .getTimeslotIndex()),
                                    totalEnergyInTimeslot.get(helper
                                            .getMillisForIndex(ct
                                                    .getTimeslotIndex()))
                                            + ct.getExecutionMWh());
          numberOfTransactions.put(helper.getMillisForIndex(ct
                  .getTimeslotIndex()), numberOfTransactions.get(helper
                  .getMillisForIndex(ct.getTimeslotIndex())) + 1);
        }
      }
    }

    // Sort keys (timeslots)
    SortedSet<Long> sortedSet =
      new TreeSet<Long>(totalPriceInTimeslot.keySet())
              .headSet(helper.getMillisForIndex(helper
                      .getSafetyWholesaleTimeslotIndex()), true);
    for (Iterator totalIterator = sortedSet.iterator(); totalIterator.hasNext();) {
      Long timeslot = (Long) totalIterator.next();
      Double totalProfitTimeslot = totalPriceInTimeslot.get(timeslot);
      Object[] totalProfitOneTimeslot =
        { timeslot, totalProfitTimeslot / numberOfTransactions.get(timeslot),
         totalEnergyInTimeslot.get(timeslot) };
      totalDataOneTimeslot.add(totalProfitOneTimeslot);
    }

    averageProfitPerTimeslot
            .add(new WholesaleAnalysisTemplate(totalDataOneTimeslot));

    clearedTradesDataJson = gson.toJson(allClearedTrades);
    this.wholesaleAverageTimeslotPriceData =
      gson.toJson(averageProfitPerTimeslot);

  }

  private void createMostRecentClearings (Gson gson,
                                          WholesaleService wholesaleService,
                                          VisualizerHelperService helper)
  {
    // most recent clearings for each timeslot.
    ArrayList<Object> energyMostRecentClearings = new ArrayList<Object>();
    ArrayList<Object> cashMostRecentClearings = new ArrayList<Object>();

    ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> mapFinalTrades =
      wholesaleService.getClearedTrades();

    SortedSet<Long> keys =
      new TreeSet<Long>(wholesaleService.getClearedTrades().keySet())
              .headSet(helper.getMillisForIndex(helper
                      .getSafetyWholesaleTimeslotIndex()), true);// mapFinalTrades.keySet());

    for (Long key: keys) {
      ConcurrentHashMap<Long, ClearedTrade> clearedTrades =
        mapFinalTrades.get(key);
      if (clearedTrades != null) {
        Long lastKey = new TreeSet<Long>(clearedTrades.keySet()).last();
        ClearedTrade mostRecentClearing = clearedTrades.get(lastKey);
        Object[] energy = { key, mostRecentClearing.getExecutionMWh() };
        Object[] cash = { key, mostRecentClearing.getExecutionPrice() };
        energyMostRecentClearings.add(energy);
        cashMostRecentClearings.add(cash);
      }
    }
    energyMostRecentClearingsJson = gson.toJson(energyMostRecentClearings);
    cashMostRecentClearingsJson = gson.toJson(cashMostRecentClearings);

  }

  public String getCashMostRecentClearingsJson ()
  {
    return cashMostRecentClearingsJson;
  }

  public String getEnergyMostRecentClearingsJson ()
  {
    return energyMostRecentClearingsJson;
  }

  public String getClearedTradesDataJson ()
  {
    return clearedTradesDataJson;
  }

  public String getAllMarketTransactionsData ()
  {
    return allMarketTransactionsData;
  }

  public String getWholesaleDynData ()
  {
    return wholesaleDynData;
  }

  public String getWholesaleDynDataOneTimeslot ()
  {
    return wholesaleDynDataOneTimeslot;
  }

  public String getWholesaleAverageTimeslotPriceData ()
  {
    return wholesaleAverageTimeslotPriceData;
  }

}
