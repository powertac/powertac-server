package org.powertac.common.commands;

import com.sun.corba.se.pept.broker.Broker;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 * User: ddauer
 * Date: 12/1/10
 * Time: 2:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class CashUpdateCommand {
    Broker broker;
    BigDecimal moneyChange;
    String reason;
    String origin; //the person or entity that caused the update

}
