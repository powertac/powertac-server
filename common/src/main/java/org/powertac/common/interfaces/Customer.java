package org.powertac.common.interfaces;

import org.powertac.common.commands.TariffPublishedCommand;

import java.util.List;

public interface Customer {

    /* Model identification */
    String id();
    String name();

    //void processTimeslot(TimeslotCreatedCommand timeslotCreatedCommand);
    //void processTimeslot(TimeslotUpdatedCommand timeslotUpdatedCommand);

    /**
     *
     * @param List<TariffPublishedCommand> tariffPublishedCommandList
     * @return A list of possible tariff replies which represent subscriptions or negotiation interactions
     */
    List<TariffReplyCommand> processTariffList(List<TariffPublishedCommand> tariffPublishedCommandList);

    MeterReadingCommand generateMeterReading(TimeslotUpdatedCommand timeslotUpdatedCommand);

    CustomerInfoCommand generateCustomerInfo();
}
