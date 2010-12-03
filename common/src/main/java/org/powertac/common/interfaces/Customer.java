package org.powertac.common.interfaces;

import org.powertac.common.commands.*;

import java.util.List;

public interface Customer {

    //void processTimeslot(TimeslotCreatedCommand timeslotCreatedCommand);
    //void processTimeslot(TimeslotChangedCommand timeslotChangedCommand);

    /**
     *
     * @param List<TariffPublishCommand> tariffPublishCommandList
     * @return A list of possible tariff replies which represent subscriptions or negotiation interactions
     */
    List<TariffReplyCommand> processTariffList(List<TariffPublishCommand> tariffPublishedCommandList);

    MeterReadingCommand generateMeterReading(TimeslotChangedCommand timeslotChangedCommand);

    CustomerInfoCommand generateCustomerInfo();
}
