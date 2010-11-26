package org.powertac.server.module.customer;

import org.powertac.common.interfaces.Customer;

public class CustomerImpl implements Customer {

    public CustomerImpl() {
        System.out.println("CustomerImpl");
    }

    @Override
    public String id() {
        return "customer.0.0.1";
    }

    @Override
    public String name() {
        return "The name of the default customer model";
    }

}
