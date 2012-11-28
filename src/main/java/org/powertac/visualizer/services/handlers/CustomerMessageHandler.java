package org.powertac.visualizer.services.handlers;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.TariffTransaction;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class CustomerMessageHandler implements Initializable
{

  @Autowired
  private MessageDispatcher router;
  @Autowired
  private CustomerService customerService;
  @Autowired
  private VisualizerBean visualizerBean;
  private static Logger log = Logger.getLogger(CustomerMessageHandler.class);

  public void initialize ()
  {
    for (Class<?> clazz: Arrays.asList(Competition.class,
                                       TariffTransaction.class,
                                       CustomerBootstrapData.class)) {
      router.registerMessageHandler(this, clazz);
    }
  }

  public void handleMessage (Competition competition)
  {
    customerService.addCustomers(competition.getCustomers());
  }

  public void handleMessage (TariffTransaction transaction)
  {
    Customer customer =
      customerService.findCustomerByCustomerInfo(transaction.getCustomerInfo());
    if (customer == null) {
      // should be PUBLISH or REVOKE tx
      if (transaction.getTxType() != TariffTransaction.Type.PUBLISH
          && transaction.getTxType() != TariffTransaction.Type.REVOKE) {
        log.warn("Customer '" + transaction.getCustomerInfo() + "' not found!");
      }
    }
    else {
      customer.addTariffTransaction(transaction);
    }
  }

  public void handleMessage (CustomerBootstrapData data)
  {
    Customer customer =
      customerService.findCustomerByNameAndType(data.getCustomerName(),
                                                data.getPowerType());
    if (customer != null) {
      customer.addCustomerBootstrapData(data, visualizerBean.getCompetition());
    }

  }

}
