package org.powertac.common.commands;

import org.powertac.common.interfaces.Customer;
import org.powertac.common.interfaces.Timeslot;

import java.math.BigDecimal;


public class MeterReadingCommand {
  Customer customer;
  Timeslot timeslot;
  BigDecimal meterReading;
}
