package org.powertac.common.commands;

import org.powertac.common.interfaces.Broker;

import java.math.BigDecimal;

public class CashUpdateCommand {
    Broker broker;
    BigDecimal moneyChange;
    String reason;
    String origin; //the person or entity that caused the update

}
