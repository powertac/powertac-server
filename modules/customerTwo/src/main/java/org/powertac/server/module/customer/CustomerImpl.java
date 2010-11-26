package org.powertac.server.module.customer;

import org.powertac.common.interfaces.Customer;

public class CustomerImpl implements Customer {

    public CustomerImpl() {
        System.out.println("CustomerImpl Two");
    }

    @Override
    public String id() {
        return "customerTwo.0.0.1";
    }

    @Override
    public String name() {
        return "Second Customer Model";
    }

}
