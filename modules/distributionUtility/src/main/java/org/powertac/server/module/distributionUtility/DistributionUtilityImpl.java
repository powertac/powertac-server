package org.powertac.server.module.distributionUtility;

import org.powertac.common.interfaces.DistributionUtility;

public class DistributionUtilityImpl implements DistributionUtility {

    public DistributionUtilityImpl() {
        System.out.println("DistributionUtilityImpl");
    }

    @Override
    public String id() {
        return "du.0.0.1";
    }

    @Override
    public String name() {
        return "Default Distribution Utility Model";
    }

}
