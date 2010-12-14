package org.powertac.server.module.distributionUtility;

import org.powertac.common.builders.CashBuilder;
import org.powertac.common.commands.DepotUpdate;
import org.powertac.common.commands.MeterReadingBalance;
import org.powertac.common.interfaces.DistributionUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DistributionUtilityImpl implements DistributionUtility {
    
    final static Logger log = LoggerFactory.getLogger(DistributionUtilityImpl.class);    

    public DistributionUtilityImpl() {
        log.debug("DistributionUtilityImpl");
    }

    @Override
    public String id() {
        return "du.0.0.1";
    }

    @Override
    public String name() {
        return "Default Distribution Utility Model";
    }

    @Override
    public void log(String message) {
        log.debug("This is the distribution utility logging: " + message);
    }

    @Override
    public List processMeterReadingBalances(List<MeterReadingBalance> meterReadingBalances) {
        log.debug("processMeterReadingBalances " + meterReadingBalances);
        List cashAndDepotUpdateCommands = new ArrayList();
        cashAndDepotUpdateCommands.add(CashBuilder.withEmpty().buildCashChanged());
        cashAndDepotUpdateCommands.add(new DepotUpdate());
        return cashAndDepotUpdateCommands;
    }

}
