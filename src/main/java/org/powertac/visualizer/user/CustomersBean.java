package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;

import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomersBean implements Serializable
{

  private static final long serialVersionUID = 1L;
  private CustomerService customerService;
  private ArrayList<Customer> customers;
  private UserSessionBean userSessionBean;

  @Autowired
  public CustomersBean (CustomerService customerService,
                        UserSessionBean userSessionBean)
  {

    this.customerService = customerService;
    this.userSessionBean = userSessionBean;
    retrieveCustomers();
  }

  public ArrayList<Customer> getCustomers ()
  {
    retrieveCustomers();
    return customers;
  }

  private void retrieveCustomers ()
  {
    if (userSessionBean.getGenericType().equalsIgnoreCase("CONSUMPTION")) {
      customers = customerService.getConsumers();
    }
    else if (userSessionBean.getGenericType().equalsIgnoreCase("PRODUCTION")) {
      customers = customerService.getProducers();
    }
    else if (userSessionBean.getGenericType().equalsIgnoreCase("STORAGE")) {
      customers = customerService.getStorages();
    }
  }
}
