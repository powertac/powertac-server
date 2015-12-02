package org.powertac.visualizer.services.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.powertac.common.BalancingTransaction;
import org.powertac.common.CashPosition;
import org.powertac.common.Competition;
import org.powertac.common.DistributionTransaction;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TariffTransaction.Type;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.push.InfoPush;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.GradingService;
import org.powertac.visualizer.services.PushService;
import org.powertac.visualizer.statistical.BalancingCategory;
import org.powertac.visualizer.statistical.DistributionCategory;
import org.powertac.visualizer.statistical.FinanceCategory;
import org.powertac.visualizer.statistical.FinanceDynamicData;
import org.powertac.visualizer.statistical.TariffCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//import org.apache.tools.ant.taskdefs.Tstamp;

@Service
public class BrokerMessageHandler implements Initializable
{

  private Logger log = LogManager.getLogger(BrokerMessageHandler.class);

  @Autowired
  private MessageDispatcher router;
  @Autowired
  private BrokerService brokerService;
  @Autowired
  private AppearanceListBean appearanceListBean;
  @Autowired
  private VisualizerBean vizBean;
  @Autowired
  private PushService pushService;
  @Autowired
  private GradingService gradingBean;

  public void initialize ()
  {
    for (Class<?> clazz: Arrays.asList(Competition.class,
                                       TariffSpecification.class,
                                       CashPosition.class,
                                       TariffTransaction.class,
                                       DistributionTransaction.class,
                                       BalancingTransaction.class)) {
      router.registerMessageHandler(this, clazz);
    }
  }

  public void handleMessage (Competition competition)
  {
    List<String> brokersName = competition.getBrokers();

    ArrayList<BrokerModel> list = new ArrayList<BrokerModel>();
    ConcurrentHashMap<String, BrokerModel> map =
      new ConcurrentHashMap<String, BrokerModel>(10, 0.75f, 1);

    for (Iterator<String> iterator = brokersName.iterator(); iterator.hasNext();) {
      String name = (String) iterator.next();
      BrokerModel brokerModel =
        new BrokerModel(name, appearanceListBean.getAppereance());
      list.add(brokerModel);
      map.put(brokerModel.getName(), brokerModel);
    }
    brokerService.setBrokersMap(map);
    brokerService.setBrokers(list);
    vizBean.setCompetition(competition);
    vizBean.setRunning(true);
    vizBean.setFinished(false);
    // notification:
    pushService.pushInfoMessage(new InfoPush("start"));
    pushService.pushInfoMessage(new InfoPush(competition.getName()));
  }

  public void handleMessage (TariffSpecification tariffSpecification)
  {
    if (tariffSpecification.getSupersedes() != null) {
      log.debug("NO of tariffspec:"
                + tariffSpecification.getSupersedes().size());
    }

    // find matching broker and add received tariff spec. to its history.
    BrokerModel brokerModel =
      brokerService.findBrokerByName(tariffSpecification.getBroker()
              .getUsername());
    if (brokerModel != null) {
      brokerModel.getTariffCategory()
              .processTariffSpecification(tariffSpecification);
    }
  }

  public void handleMessage (CashPosition msg)
  {

    // update balance, if such broker exists
    BrokerModel broker =
      brokerService.findBrokerByName(msg.getBroker().getUsername());
    if (broker != null) {
      FinanceCategory fc = broker.getFinanceCategory();
      int tsIndex = vizBean.getCurrentTimeslotSerialNumber();
      ConcurrentHashMap<Integer, FinanceDynamicData> fddMap =
        fc.getFinanceDynamicDataMap();
      if (!fddMap.containsKey(tsIndex)) {
        FinanceDynamicData fdd =
          new FinanceDynamicData(fc.getProfit(), tsIndex);
        fc.setLastFinanceDynamicData(fdd);
        fddMap.put(tsIndex, fdd);
      }
      fddMap.get(tsIndex).updateProfit(msg.getBalance());
      fc.setProfit(msg.getBalance());
    }

  }

  public void handleMessage (TariffTransaction msg)
  {
    // broker, not genco:
    BrokerModel broker =
      brokerService.findBrokerByName(msg.getBroker().getUsername());
    if (broker != null) {
      gradingBean.addCharge(Math.abs(msg.getCharge()));
      if (msg.getTxType() == Type.SIGNUP) {
        broker.getTariffCategory().addCustomers(msg.getCustomerCount());
      }

      if (msg.getTxType() == Type.CONSUME) {
        gradingBean.addSoldEnergyTariffMarket(Math.abs(msg.getKWh()));
        broker.getTariffCategory()
                .addConsumptionConsumers(msg.getCustomerCount());
        broker.getTariffCategory().addSoldEnergy(Math.abs(msg.getKWh()));
        broker.getTariffCategory().addMoneyFromSold(msg.getCharge());
      }
      if (msg.getTxType() == Type.PRODUCE) {
        gradingBean.addBoughtEnergyTariffMarket(msg.getKWh());
        broker.getTariffCategory().addBoughtEnergy(msg.getKWh());
      }
      TariffCategory tc = broker.getTariffCategory();
      tc.addCharge(Math.abs(msg.getCharge()));
      int tsIndex = msg.getPostedTimeslotIndex();
      ConcurrentHashMap<Integer, TariffDynamicData> tddmap =
        tc.getTariffDynamicDataMap();

      if (!tddmap.containsKey(tsIndex)) {
        TariffDynamicData tdd =
          new TariffDynamicData(tsIndex, tc.getProfit(), tc.getEnergy(),
                                tc.getCustomerCount());
        tc.addTariffDynamicData(tdd);
      }
      tc.update(tsIndex, msg.getKWh(), msg.getCharge(),
                Helper.getCustomerCount(msg));
      if (broker != null) {
        broker.getTariffCategory()
                .processTariffSpecification(msg.getTariffSpec());
      }
      tc.getTariffData().get(msg.getTariffSpec())
              .setCustomers(Helper.getCustomerCount(msg));
      broker.getTariffCategory().getTariffData().get(msg.getTariffSpec())
              .processTariffTx(msg);
    }
  }

  public void handleMessage (DistributionTransaction msg)
  {
    BrokerModel broker =
      brokerService.findBrokerByName(msg.getBroker().getUsername());
    if (broker != null) {
      DistributionCategory dc = broker.getDistributionCategory();
      dc.update(msg.getPostedTimeslotIndex(), msg.getKWh(), msg.getCharge());
      gradingBean.addEnergyDistribution(Math.abs(msg.getKWh()));
    }

  }

  public void handleMessage (BalancingTransaction bt)
  {
    BrokerModel broker =
      brokerService.findBrokerByName(bt.getBroker().getUsername());
    if (broker != null) {
      BalancingCategory bc = broker.getBalancingCategory();
      bc.update(bt.getPostedTimeslotIndex(), bt.getKWh(), bt.getCharge());
    }
  }
}
