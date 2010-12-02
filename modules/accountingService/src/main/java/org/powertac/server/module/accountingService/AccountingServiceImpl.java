package org.powertac.server.module.accountingService;

import org.powertac.common.commands.*;
import org.powertac.common.interfaces.AccountingService;

import java.util.List;

public class AccountingServiceImpl implements AccountingService {

    public AccountingServiceImpl() {
        System.out.println("AccountingServiceImpl");
    }

    @Override
    public List<MeterReadingBalanceCommand> processMeterReadings(List<MeterReadingCommand> meterReadingCommandList) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DepotPositionCommand processDepotUpdate(DepotUpdateCommand depotUpdateCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CashChangedCommand processCashUpdate(CashUpdateCommand cashUpdateCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<TariffPublishedCommand> publishTariffList() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public TariffReplyCommand processTariffReply(TariffReplyCommand tariffReplyCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<CustomerInfoCommand> processCustomerInfo(List<CustomerInfoCommand> customerInfoCommands) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
