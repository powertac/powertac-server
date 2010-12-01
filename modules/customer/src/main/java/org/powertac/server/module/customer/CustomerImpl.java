package org.powertac.server.module.customer;

import org.powertac.common.commands.MeterReadingCommand;
import org.powertac.common.commands.TariffPublishedCommand;
import org.powertac.common.commands.TariffReplyCommand;
import org.powertac.common.commands.TimeslotUpdatedCommand;
import org.powertac.common.interfaces.Customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class CustomerImpl implements Customer {

    public CustomerImpl() {
        //System.out.println("CustomerImpl");
    }

    @Override
    public String id() {
        return "customer.0.0.1";
    }

    @Override
    public String name() {
        return "The name of the default customer model";
    }

    @Override
    public List<TariffReplyCommand> processTariffList(List<TariffPublishedCommand> tariffPublishedCommandList) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ArrayList<TariffReplyCommand> processTariffList(ArrayList<TariffPublishedCommand> tariffPublishedCommandList) {
        TariffPublishedCommand bestTariff = null;
        BigDecimal currentMaxUtility = new BigDecimal(0);
        for (TariffPublishedCommand tariffPublishedCommand: tariffPublishedCommandList)
        {
          if(getUtility(tariffPublishedCommand).compareTo(currentMaxUtility)>0)
          {
            bestTariff = tariffPublishedCommand;
          }
        }

         ArrayList<TariffReplyCommand> tariffChoice = new ArrayList<TariffReplyCommand>();
        tariffChoice.add(bestTariff);
        return(tariffChoice);
    }

    private BigDecimal getUtility(TariffPublishedCommand tariffPublishedCommand) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public MeterReadingCommand generateMeterReading(TimeslotUpdatedCommand timeslotUpdatedCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
