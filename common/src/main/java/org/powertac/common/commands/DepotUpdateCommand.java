package org.powertac.common.commands;

import com.sun.corba.se.pept.broker.Broker;
import org.powertac.common.interfaces.Product;

import java.math.BigDecimal;

/**
 * User: cblock
 * Date: 01.12.10
 * Time: 16:45
 */
public class DepotUpdateCommand {
  Broker broker;
  Product product;
  BigDecimal depotChange;
  String reason;
  String origin; //the person or entity that caused the update
}
