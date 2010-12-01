package org.powertac.common.commands;

import org.joda.time.LocalDateTime;
import org.powertac.common.interfaces.Product;
import org.powertac.common.interfaces.Timeslot;

import java.math.BigDecimal;

/**
 * An OrderbookChangedCommand represents
 * an orderbook state up to the depth of 10 orders on
 * the buy and sell side.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 */
public class OrderbookChangedCommand {
    LocalDateTime dateCreated;
    Long transactionID;
    Timeslot timeslot;
    Product product;
    BigDecimal bid0 = null;
    BigDecimal bid1 = null;
    BigDecimal bid3 = null;
    BigDecimal bid2 = null;
    BigDecimal bid4 = null;
    BigDecimal bid5 = null;
    BigDecimal bid6 = null;
    BigDecimal bid7 = null;
    BigDecimal bid8 = null;
    BigDecimal bid9 = null;
    BigDecimal bidSize0 = new BigDecimal(0);
    BigDecimal bidSize1 = new BigDecimal(0);
    BigDecimal bidSize2 = new BigDecimal(0);
    BigDecimal bidSize3 = new BigDecimal(0);
    BigDecimal bidSize4 = new BigDecimal(0);
    BigDecimal bidSize5 = new BigDecimal(0);
    BigDecimal bidSize6 = new BigDecimal(0);
    BigDecimal bidSize7 = new BigDecimal(0);
    BigDecimal bidSize8 = new BigDecimal(0);
    BigDecimal bidSize9 = new BigDecimal(0);
    BigDecimal ask0 = null;
    BigDecimal ask1 = null;
    BigDecimal ask2 = null;
    BigDecimal ask3 = null;
    BigDecimal ask4 = null;
    BigDecimal ask5 = null;
    BigDecimal ask6 = null;
    BigDecimal ask7 = null;
    BigDecimal ask8 = null;
    BigDecimal ask9 = null;
    BigDecimal askSize0 = new BigDecimal(0);
    BigDecimal askSize1 = new BigDecimal(0);
    BigDecimal askSize2 = new BigDecimal(0);
    BigDecimal askSize3 = new BigDecimal(0);
    BigDecimal askSize4 = new BigDecimal(0);
    BigDecimal askSize5 = new BigDecimal(0);
    BigDecimal askSize6 = new BigDecimal(0);
    BigDecimal askSize7 = new BigDecimal(0);
    BigDecimal askSize8 = new BigDecimal(0);
    BigDecimal askSize9 = new BigDecimal(0);

}
