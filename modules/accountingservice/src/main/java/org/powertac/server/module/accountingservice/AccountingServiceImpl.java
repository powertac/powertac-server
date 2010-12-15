package org.powertac.server.module.accountingservice;

import org.powertac.common.builders.CashBuilder;
import org.powertac.common.commands.*;
import org.powertac.common.enumerations.CustomerType;
import org.powertac.common.interfaces.AccountingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AccountingServiceImpl implements AccountingService {

    final static Logger log = LoggerFactory.getLogger(AccountingServiceImpl.class);

    @Override
    public List<MeterReadingBalance> processMeterReadings(List<MeterReading> meterReadingList) {
        log.debug("processMeterReadings " + meterReadingList);
        List balanceList = new ArrayList<MeterReadingBalance>();
        balanceList.add(new MeterReadingBalance());
        return balanceList;
    }

    @Override
    public DepotChanged processDepotUpdate(DepotUpdate depotUpdate) {
        log.debug("processDepotUpdate " + depotUpdate) ;
        return new DepotChanged();
    }

    @Override
    public CashChanged processCashUpdate(CashUpdate cashUpdate) {
        log.debug("processCashUpdate " + cashUpdate);
        return CashBuilder.withEmpty().buildCashChanged();
    }

    @Override
    public List<TariffPublish> publishTariffList() {
        // Demo implementation. This should return a list of all currently stored tariffs.
        ArrayList<TariffPublish> tariffList = new ArrayList<TariffPublish>();
        Set<CustomerType> permittedCustomerTypes = new HashSet<CustomerType>();
        permittedCustomerTypes.add(CustomerType.ConsumerOffice);
        TariffPublish tariffPublish = new TariffPublish(permittedCustomerTypes, "testToken", 1l,1.0, 1.0, new Double[] {1.0, 1.0}, new Double[] {0.1, 0.1}, new org.joda.time.LocalDateTime(), new org.joda.time.LocalDateTime(), 1, 2, 1.0, 2.0, 3.0, 4.0);
        tariffList.add(tariffPublish);
        return(tariffList);
    }

    @Override
    public TariffReply processTariffReply(TariffReply tariffReply) {
        // Demo implementation. Should return same object, which is fine.
        return tariffReply;
    }

    @Override
    public List<CustomerInfo> processCustomerInfo(List<CustomerInfo> customerInfo) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
