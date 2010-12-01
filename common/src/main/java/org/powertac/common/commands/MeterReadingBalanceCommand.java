package org.powertac.common.commands;

import org.powertac.common.interfaces.Broker;
import org.powertac.common.interfaces.Customer;
import org.powertac.common.interfaces.Timeslot;

import java.math.BigDecimal;

/**
 * User: cblock
 * Date: 01.12.10
 * Time: 16:36
 */
public class MeterReadingBalanceCommand {
  Broker broker;
  Timeslot timeslot;
  BigDecimal meterReading;
}
