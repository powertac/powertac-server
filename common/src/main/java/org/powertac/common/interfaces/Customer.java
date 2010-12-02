package org.powertac.common.interfaces;

import org.powertac.common.commands.*;

import java.util.List;

public interface Customer {

    /* Model identification */
    String id();
    String name();

    //void processTimeslot(TimeslotCreatedCommand timeslotCreatedCommand);
    //void processTimeslot(TimeslotChangedCommand timeslotChangedCommand);

    /**
     *
     * @param List<TariffPublishedCommand> tariffPublishedCommandList
     * @return A list of possible tariff replies which represent subscriptions or negotiation interactions
     */
    List<TariffReplyCommand> processTariffList(List<TariffPublishedCommand> tariffPublishedCommandList);

    MeterReadingCommand generateMeterReading(TimeslotChangedCommand timeslotChangedCommand);

    CustomerInfoCommand generateCustomerInfo();
}
