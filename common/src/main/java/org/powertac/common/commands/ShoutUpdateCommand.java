package org.powertac.common.commands;

import java.math.BigDecimal;

/**
 * Command object that can be used by
 * a broker to require the server to change
 * price and / or quantity
 * an already issued shout specific shout;
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 */
public class ShoutUpdateCommand {
  String authToken;
  BigDecimal quantity;
  BigDecimal limitPrice;
}
