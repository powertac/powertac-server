package org.powertac.common.commands;

import org.joda.time.LocalDateTime;
import org.powertac.common.interfaces.Product;
import org.powertac.common.interfaces.Timeslot;

import java.math.BigDecimal;

/**
 * A QuoteChangedCommand essentially represents
 * an orderbook state up to the depth of 1 order on
 * the buy and sell side.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 * @see org.powertac.common.commands.OrderbookChangedCommand
 */
public class QuoteChangedCommand {
  LocalDateTime dateCreated;
    Long transactionID;
    Timeslot timeslot;
    Product product;
    BigDecimal bid = null;
    BigDecimal bidSize = new BigDecimal(0);
    BigDecimal ask = null;
    BigDecimal askSize = new BigDecimal(0);

}
