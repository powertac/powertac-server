package org.powertac.server.module.customer;

import org.powertac.common.commands.*;
import org.powertac.common.interfaces.Customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        TariffPublishedCommand bestTariffPublished = null;
        BigDecimal currentMaxUtility = new BigDecimal(0);
        for (TariffPublishedCommand tariffPublishedCommand: tariffPublishedCommandList)
        {
          if(getUtility(tariffPublishedCommand).compareTo(currentMaxUtility)>0)
          {
            bestTariffPublished = tariffPublishedCommand;
          }
        }

        ArrayList<TariffReplyCommand> tariffChoice = new ArrayList<TariffReplyCommand>();
        TariffReplyCommand bestTariffReply = new TariffReplyCommand();
        //tariffChoice.add(bestTariffPublished); //TODO: we need a simply way to convert TariffPublishedCommands into TariffReplyCommands
        //bestTariffReply = TariffParser.convert(bestTariffPublished)
        tariffChoice.add(bestTariffReply);
        return(tariffChoice);
    }

    private BigDecimal getUtility(TariffPublishedCommand tariffPublishedCommand) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public MeterReadingCommand generateMeterReading(TimeslotUpdatedCommand timeslotUpdatedCommand) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

  @Override
  public CustomerInfoCommand generateCustomerInfo() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

}
