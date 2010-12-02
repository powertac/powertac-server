package org.powertac.server.module.customer;

import org.powertac.common.commands.*;
import org.powertac.common.interfaces.Customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CustomerImpl implements Customer {

    @Override
    public List<TariffReplyCommand> processTariffList(List<TariffPublishedCommand> tariffPublishedCommandList) {
        /*TariffPublishedCommand bestTariffPublished = null;
        BigDecimal currentMaxUtility = new BigDecimal(0);
        for (TariffPublishedCommand tariffPublishedCommand : tariffPublishedCommandList) {
            if (getUtility(tariffPublishedCommand).compareTo(currentMaxUtility) > 0) {
                bestTariffPublished = tariffPublishedCommand;
            }
        }*/

        // Demo implementation. Returns empty list.
        ArrayList<TariffReplyCommand> tariffChoice = new ArrayList<TariffReplyCommand>();
        TariffReplyCommand bestTariffReply = new TariffReplyCommand();
        tariffChoice.add(bestTariffReply);
        return (tariffChoice);
    }

    private BigDecimal getUtility(TariffPublishedCommand tariffPublishedCommand) {
        return null;
    }

    @Override
    public MeterReadingCommand generateMeterReading(TimeslotUpdatedCommand timeslotUpdatedCommand) {
        return null;
    }

    @Override
    public CustomerInfoCommand generateCustomerInfo() {
        return null;
    }

}
