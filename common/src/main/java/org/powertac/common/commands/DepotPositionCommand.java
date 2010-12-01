package org.powertac.common.commands;

import com.sun.corba.se.pept.broker.Broker;
import org.joda.time.LocalDateTime;
import org.powertac.common.interfaces.Product;

import java.math.BigDecimal;

/**
 * User: cblock
 * Date: 01.12.10
 * Time: 16:45
 */
public class DepotPositionCommand {
  Broker broker;
  Product product;
  Long transactionId;
  BigDecimal depotChange;
  BigDecimal balance;
  String reason;
  String origin;
  LocalDateTime dateCreated;
}
