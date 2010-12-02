package org.powertac.server.core;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class CompetitionController implements BundleActivator {

    public CompetitionController() {
        System.out.println("CompetitionController");
    }

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        System.out.println("starting");
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        System.out.println("stopping");
    }
}
