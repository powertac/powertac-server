package org.powertac.visualizer.services.handlers;

import org.apache.log4j.Logger;
import org.powertac.common.*;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.TariffUpdate;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.CustomerModel;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.BrokerService;
import org.primefaces.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BrokerMessageHandler implements Initializable
{

  private Logger log = Logger.getLogger(BrokerMessageHandler.class);

  @Autowired
  private MessageDispatcher router;
  @Autowired
  private BrokerService brokerService;
  @Autowired
  private AppearanceListBean appearanceListBean;

  public void initialize ()
  {
    for (Class<?> clazz: Arrays.asList(Competition.class,
                                       TariffSpecification.class,
                                       CashPosition.class,
                                       TariffTransaction.class,
                                       DistributionTransaction.class,
                                       BalancingTransaction.class,
                                       TariffExpire.class, TariffRevoke.class,
                                       TariffStatus.class, TariffUpdate.class)) {
      router.registerMessageHandler(this, clazz);
    }
  }

  public void handleMessage (Competition competition)
  {
    List<String> brokersName = competition.getBrokers();
    HashMap<String, BrokerModel> map = new HashMap<String, BrokerModel>();

    ArrayList<BrokerModel> list = new ArrayList<BrokerModel>();

    JSONArray brokerSeriesColors = new JSONArray();
    // StringBuilder seriesOptions = new StringBuilder();
    // String prefix = "";

    JSONArray seriesOptions = new JSONArray();

    for (String name: brokersName) {
      BrokerModel brokerModel =
          new BrokerModel(name, appearanceListBean.getAppereance());

      // build broker series options:
      seriesOptions.put(brokerModel.getJson().getSeriesOptions());
      // build colors:
      brokerSeriesColors.put(brokerModel.getAppearance().getColorCode());

      // for each broker, build its customer list.
      Set<CustomerModel> customerModels = new HashSet<CustomerModel>();
      for (CustomerInfo customerInfo: competition.getCustomers()) {
        customerModels.add(new CustomerModel(customerInfo));

      }
      brokerModel.setCustomerModels(customerModels);

      map.put(brokerModel.getName(), brokerModel);
      list.add(brokerModel);
    }
    brokerService.setMap(map);
    brokerService.setBrokers(list);
    brokerService.getJson().setBrokerSeriesColors(brokerSeriesColors);
    brokerService.getJson().setSeriesOptions(seriesOptions);
  }

  public void handleMessage (TariffSpecification tariffSpecification)
  {
    log.debug("\nBroker: " + tariffSpecification.getBroker().getUsername()
              + " Min duration: " + tariffSpecification.getMinDuration()
              + " EarlyWithdrPaymnt "
              + tariffSpecification.getEarlyWithdrawPayment()
              + " PeriodicPayment: " + tariffSpecification.getPeriodicPayment()
              + " SignupPayment" + tariffSpecification.getSignupPayment()
              + " Expiration: " + tariffSpecification.getExpiration()
              + " PowerType: " + tariffSpecification.getPowerType() + " ID: "
              + tariffSpecification.getId());

    if (tariffSpecification.getSupersedes() != null) {
      log.debug("NO of tariffspec:"
                + tariffSpecification.getSupersedes().size());
    }

    List<Rate> rates = tariffSpecification.getRates();
    String ispis = "";
    for (Rate rate: rates) {
      ispis += "" + rate.toString();
    }
    log.debug("RATE:\n" + ispis);

    // find matching broker and add received tariff spec. to its history.
    BrokerModel brokerModel =
      brokerService.findBrokerByName(tariffSpecification.getBroker()
              .getUsername());
    if (brokerModel != null) {
      brokerModel.addTariffSpecification(tariffSpecification);
    }
  }

  public void handleMessage (CashPosition cashPosition)
  {

    log.debug("\nBalance: " + cashPosition.getBalance() + " for broker "
              + cashPosition.getBroker().getUsername());
    // update balance, if such broker exists
    BrokerModel broker =
      brokerService.findBrokerByName(cashPosition.getBroker().getUsername());

    if (broker != null) {
      broker.updateCashBalance(cashPosition.getBalance());

    }
  }

  public void handleMessage (TariffTransaction tariffTransaction)
  {
    log.debug("Broker: " + tariffTransaction.getBroker() + " Charge: "
              + tariffTransaction.getCharge() + " CustomerCount: "
              + tariffTransaction.getCustomerCount() + "\n KWh: "
              + tariffTransaction.getKWh() + " CustomerInfo: "
              + tariffTransaction.getCustomerInfo() + "Posted time: "
              + tariffTransaction.getPostedTime() + "\n TxType: "
              + tariffTransaction.getTxType());
    // broker, not genco:
    BrokerModel brokerModel =
      brokerService.findBrokerByName(tariffTransaction.getBroker()
              .getUsername());
    if (brokerModel != null) {
      brokerModel.addTariffTransaction(tariffTransaction);

    }

  }

  public void handleMessage (DistributionTransaction distributionTransaction)
  {
    log.debug("Broker: " + distributionTransaction.getBroker() + "\nCharge: "
              + distributionTransaction.getCharge() + "\nkWh: "
              + distributionTransaction.getKWh());

    // fix for brokers that do not receive balancing transaction (because
    // their distributionTransaction is 0 KWh!)
    if (distributionTransaction.getKWh() == 0) {

      BrokerModel brokerModel =
        brokerService.findBrokerByName(distributionTransaction.getBroker()
                .getUsername());
      if (brokerModel != null) {
        brokerModel.updateEnergyBalance(0);
      }
    }

  }

  public void handleMessage (BalancingTransaction balancingTransaction)
  {
    log.debug("Broker: " + balancingTransaction.getBroker() + "\nCharge: "
              + balancingTransaction.getCharge() + "\nkWh: "
              + balancingTransaction.getKWh() + "\n");

    BrokerModel broker =
      brokerService.findBrokerByName(balancingTransaction.getBroker()
              .getUsername());
    if (broker != null) {
      broker.addBalancingTransaction(balancingTransaction);
    }
  }

  public void handleMessage (TariffExpire msg)
  {
    BrokerModel broker =
      brokerService.findBrokerByName(msg.getBroker().getUsername());
    if (broker != null) {
      broker.getTariffInfoMaps()
              .get(msg.getTariffId())
              .addTariffMessage(msg.getClass().getSimpleName() + ":"
                                        + msg.getNewExpiration());
    }
  }

  public void handleMessage (TariffRevoke msg)
  {
    BrokerModel broker =
      brokerService.findBrokerByName(msg.getBroker().getUsername());
    if (broker != null) {
      broker.getTariffInfoMaps().get(msg.getTariffId())
              .addTariffMessage(msg.getClass().getSimpleName());
    }
  }

  public void handleMessage (TariffStatus msg)
  {
    // BrokerModel broker =
    // brokerService.findBrokerByName(msg.getBroker().getUsername());
    // if (broker != null) {
    // broker.getTariffInfoMaps().get(msg.getTariffId()).addTariffMessage(msg.getClass().getSimpleName()+":"+msg.getMessage());
    // }
  }

  public void handleMessage (TariffUpdate msg)
  {
    BrokerModel broker =
      brokerService.findBrokerByName(msg.getBroker().getUsername());
    if (broker != null) {
      broker.getTariffInfoMaps().get(msg.getTariffId())
              .addTariffMessage(msg.getClass().getSimpleName());
    }
  }
}
