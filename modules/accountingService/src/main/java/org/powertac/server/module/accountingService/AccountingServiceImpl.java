package org.powertac.server.module.accountingService;

import org.powertac.common.commands.*;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.interfaces.AccountingService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccountingServiceImpl implements AccountingService {

    @Override
    public List<MeterReadingBalanceCommand> processMeterReadings(List<MeterReadingCommand> meterReadingCommandList) {
        System.out.println("processMeterReadings " + meterReadingCommandList);
        List balanceList = new ArrayList<MeterReadingBalanceCommand>();
        balanceList.add(new MeterReadingBalanceCommand());
        return balanceList;
    }

    @Override
    public DepotChanged processDepotUpdate(DepotUpdate depotUpdate) {
        System.out.println("processDepotUpdate " + depotUpdate);
        return new DepotChanged();
    }

    @Override
    public CashChanged processCashUpdate(CashUpdate cashUpdate) {
        System.out.println("processCashUpdate " + cashUpdate);
        return new CashChanged();
    }

    @Override
    public List<TariffPublishCommand> publishTariffList() {
        // Demo implementation. This should return a list of all currently stored tariffs.
        ArrayList<TariffPublishCommand> tariffList = new ArrayList<TariffPublishCommand>();
        Set<CustomerType> permittedCustomerTypes = new HashSet<CustomerType>();
        permittedCustomerTypes.add(CustomerType.ConsumerOffice);
        TariffPublishCommand tariffPublishedCommand = new TariffPublishCommand(permittedCustomerTypes, "testToken", 1l,1.0, 1.0, new Double[] {1.0, 1.0}, new Double[] {0.1, 0.1}, new org.joda.time.LocalDateTime(), new org.joda.time.LocalDateTime(), 1, 2, 1.0, 2.0, 3.0, 4.0);
        tariffList.add(tariffPublishedCommand);
        return(tariffList);
    }

    @Override
    public TariffReplyCommand processTariffReply(TariffReplyCommand tariffReplyCommand) {
        // Demo implementation. Should return same object, which is fine.
        return tariffReplyCommand;
    }

    @Override
    public List<CustomerInfo> processCustomerInfo(List<CustomerInfo> customerInfo) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
