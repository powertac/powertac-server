package org.powertac.common.commands;

import org.joda.time.LocalDateTime;
import org.powertac.common.enumerations.BuySellIndicator;
import org.powertac.common.enumerations.ModReasonCode;
import org.powertac.common.enumerations.OrderType;
import org.powertac.common.interfaces.Product;
import org.powertac.common.interfaces.Timeslot;

import java.math.BigDecimal;

/**
 * Command object that contains all data of a
 * particular shout from the Power TAC wholesale
 * market. It is an "enriched" version of the
 * <code>ShoutCreateCommand</code> a broker originially
 * sends to the server and used to report back the current
 * execution status of a shout to the broker.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 */
public class ShoutChangedCommand {
  Long shoutID;
  Long transactionID;
  Product product;
  Timeslot timeslot;
  BuySellIndicator buySellIndicator;
  BigDecimal quantity;
  BigDecimal limitPrice;
  OrderType orderType = OrderType.MARKET;

  BigDecimal remainingQuantity;
  BigDecimal executionQuantity;
  BigDecimal executionPrice;

  LocalDateTime dateCreated;
  LocalDateTime lastUpdated;
  ModReasonCode modReasonCode;

}
