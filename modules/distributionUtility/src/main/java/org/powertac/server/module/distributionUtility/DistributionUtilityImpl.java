package org.powertac.server.module.distributionUtility;

import org.powertac.common.commands.CashUpdate;
import org.powertac.common.commands.DepotUpdate;
import org.powertac.common.commands.MeterReadingBalanceCommand;
import org.powertac.common.interfaces.DistributionUtility;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public void log(String message) {
        System.out.println("This is the distribution utility logging: " + message);
    }

    @Override
    public List processMeterReadingBalances(List<MeterReadingBalanceCommand> meterReadingBalanceCommands) {
        System.out.println("processMeterReadingBalances " + meterReadingBalanceCommands);
        List cashAndDepotUpdateCommands = new ArrayList();
        cashAndDepotUpdateCommands.add(new CashUpdate());
        cashAndDepotUpdateCommands.add(new DepotUpdate());
        return cashAndDepotUpdateCommands;
    }

}
