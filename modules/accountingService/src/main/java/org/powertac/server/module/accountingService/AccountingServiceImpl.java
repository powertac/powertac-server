package org.powertac.server.module.accountingService;

import org.powertac.common.commands.*;
import org.powertac.common.interfaces.AccountingService;

import java.util.ArrayList;
import java.util.List;

public class AccountingServiceImpl implements AccountingService {

    @Override
    public List<MeterReadingBalanceCommand> processMeterReadings(List<MeterReadingCommand> meterReadingCommandList) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DepotPositionCommand processDepotUpdate(DepotUpdateCommand depotUpdateCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public CashPositionCommand processCashUpdate(CashUpdateCommand cashUpdateCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<TariffPublishedCommand> publishTariffList() {
        // Demo implementation. Returns empty list.
        ArrayList<TariffPublishedCommand> tariffList = new ArrayList<TariffPublishedCommand>();
        TariffPublishedCommand tariffPublishedCommand = new TariffPublishedCommand();
        tariffList.add(tariffPublishedCommand);
        return(tariffList);
    }

    @Override
    public TariffReplyCommand processTariffReply(TariffReplyCommand tariffReplyCommand) {
        // Demo implementation. Should return same object, which is fine.
        return tariffReplyCommand;
    }

    @Override
    public List<CustomerInfoCommand> processCustomerInfo(List<CustomerInfoCommand> customerInfoCommands) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
