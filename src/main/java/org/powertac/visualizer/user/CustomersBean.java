package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.powertac.common.TariffTransaction;
import org.powertac.visualizer.display.BrokerSeriesTemplate;
import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.services.CustomerService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;

public class CustomersBean implements Serializable
{

  private static final long serialVersionUID = 1L;
  private CustomerService customerService;
  private ArrayList<Customer> customers;
  private UserSessionBean userSessionBean;
  private String wholesaleDynDataOneTimeslot;
  @Autowired
  VisualizerHelperService helper;

  private List<String> tabs;

  private int customerType = 0;

  @Autowired
  public CustomersBean (CustomerService customerService,
                        UserSessionBean userSessionBean,
                        VisualizerHelperService helper)
  {
    this.customerService = customerService;
    this.userSessionBean = userSessionBean;

    Gson gson = new Gson();

    ArrayList<Object> wholesaleTxDataOneTimeslot = new ArrayList<Object>();
    ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();
    ArrayList<Object> mwhDataOneTimeslot = new ArrayList<Object>();

    for (TariffTransaction tx: customerService.getCustomerList()
            .get(customerType).getCustomerModel().getTariffTransactions()) {
      Object[] profitOneTimeslot =
        { helper.getMillisForIndex(tx.getPostedTimeslotIndex()), tx.getCharge() };
      Object[] kWhOneTimeslot =
        { helper.getMillisForIndex(tx.getPostedTimeslotIndex()), tx.getKWh() };

      profitDataOneTimeslot.add(profitOneTimeslot);
      mwhDataOneTimeslot.add(kWhOneTimeslot);
    }

    wholesaleTxDataOneTimeslot
            .add(new BrokerSeriesTemplate("Price", "#aaaaaa", 0,
                                          profitDataOneTimeslot, true));
    wholesaleTxDataOneTimeslot.add(new BrokerSeriesTemplate("Energy",
                                                            "#555555", 1,
                                                            mwhDataOneTimeslot,
                                                            true));
    this.wholesaleDynDataOneTimeslot = gson.toJson(wholesaleTxDataOneTimeslot);
  }

  public ArrayList<Customer> getCustomers ()
  {
    return customers;
  }

  public CustomerService getCustomerService ()
  {
    return customerService;
  }

  public String wholesaleDynDataOneTimeslot(int index)
  {
    Gson gson = new Gson();

    ArrayList<Object> wholesaleTxDataOneTimeslot = new ArrayList<Object>();
    ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();
    ArrayList<Object> mwhDataOneTimeslot = new ArrayList<Object>();
    for (TariffTransaction tx: customerService.getCustomerList()
            .get(index).getCustomerModel().getTariffTransactions()) {
      Object[] profitOneTimeslot =
        { helper.getMillisForIndex(tx.getPostedTimeslotIndex()), tx.getCharge() };
      Object[] kWhOneTimeslot =
        { helper.getMillisForIndex(tx.getPostedTimeslotIndex()), tx.getKWh() };

      profitDataOneTimeslot.add(profitOneTimeslot);
      mwhDataOneTimeslot.add(kWhOneTimeslot);
    }

    wholesaleTxDataOneTimeslot
            .add(new BrokerSeriesTemplate("Price", "#aaaaaa", 0,
                                          profitDataOneTimeslot, true));
    wholesaleTxDataOneTimeslot.add(new BrokerSeriesTemplate("Energy",
                                                            "#aaaaaa", 1,
                                                            mwhDataOneTimeslot,
                                                            true));
    this.wholesaleDynDataOneTimeslot = gson.toJson(wholesaleTxDataOneTimeslot);
    return gson.toJson(wholesaleTxDataOneTimeslot);
  }

  public void onChange (TabChangeEvent event)
  {
    int i;
    for (i=0; i < this.customerService.getCustomerList().size(); i++)
      if (customerService.getCustomerList().get(i).getCustomerInfo().getName()
              .equals(event.getData().toString()))
        break;
    this.customerType = i;
  }

}
