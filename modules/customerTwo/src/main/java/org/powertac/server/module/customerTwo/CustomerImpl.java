package org.powertac.server.module.customerTwo;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.powertac.common.interfaces.Customer;

public class CustomerImpl implements Customer, BundleActivator {

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

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("starting customer two bundle");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        System.out.println("stopping customer two bundle");
    }
}
