package org.powertac.common.commands;

import org.powertac.common.enumerations.BuySellIndicator;
import org.powertac.common.enumerations.OrderType;

import java.math.BigDecimal;

/**
 * Command object that represents a new (incoming) shout from a broker that should be
 * matched in the power tac wholesale market
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 */
public class ShoutCreateCommand {
  String authToken;
  Long timeslotId;
  Long productId;
  BuySellIndicator buySellIndicator;
  BigDecimal quantity;
  BigDecimal limitPrice;
  OrderType orderType = OrderType.MARKET;
}
