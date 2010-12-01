package org.powertac.common.interfaces;

import java.util.List;

public interface DistributionUtility {

      /* Model identification */
    String id();
    String name();

    void log(String message);

    /**
     *
     * @param meterReadingBalanceCommands
     * @return a list of DepotUpdateCommand and CashUpdateCommand objects
     */
    List processMeterReadingBalances(List<MeterReadingBalanceCommand> meterReadingBalanceCommands);

}
