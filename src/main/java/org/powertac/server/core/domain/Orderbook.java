package org.powertac.server.core.domain;

import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class Orderbook {
  Competition competition;
  LocalDateTime dateExecuted;
  Long transactionID;
  Product product;
  Boolean outdated = false;
  BigDecimal bid1 = null;
  BigDecimal bid3 = null;
  BigDecimal bid2 = null;
  BigDecimal bid4 = null;
  BigDecimal bid5 = null;
  BigDecimal bid6 = null;
  BigDecimal bid7 = null;
  BigDecimal bid8 = null;
  BigDecimal bid9 = null;
  BigDecimal bid10 = null;
  BigDecimal bidSize1 = new BigDecimal(0);
  BigDecimal bidSize2 = new BigDecimal(0);
  BigDecimal bidSize3 = new BigDecimal(0);
  BigDecimal bidSize4 = new BigDecimal(0);
  BigDecimal bidSize5 = new BigDecimal(0);
  BigDecimal bidSize6 = new BigDecimal(0);
  BigDecimal bidSize7 = new BigDecimal(0);
  BigDecimal bidSize8 = new BigDecimal(0);
  BigDecimal bidSize9 = new BigDecimal(0);
  BigDecimal bidSize10 = new BigDecimal(0);
  BigDecimal ask1 = null;
  BigDecimal ask2 = null;
  BigDecimal ask3 = null;
  BigDecimal ask4 = null;
  BigDecimal ask5 = null;
  BigDecimal ask6 = null;
  BigDecimal ask7 = null;
  BigDecimal ask8 = null;
  BigDecimal ask9 = null;
  BigDecimal ask10 = null;
  BigDecimal askSize1 = new BigDecimal(0);
  BigDecimal askSize2 = new BigDecimal(0);
  BigDecimal askSize3 = new BigDecimal(0);
  BigDecimal askSize4 = new BigDecimal(0);
  BigDecimal askSize5 = new BigDecimal(0);
  BigDecimal askSize6 = new BigDecimal(0);
  BigDecimal askSize7 = new BigDecimal(0);
  BigDecimal askSize8 = new BigDecimal(0);
  BigDecimal askSize9 = new BigDecimal(0);
  BigDecimal askSize10 = new BigDecimal(0);

  int compareTo(Object o) {
    if (!(o instanceof Orderbook)) {
      return 1;
    }
    Orderbook other = (Orderbook) o;
    return other.dateExecuted.compareTo(this.dateExecuted); //compare other to this on
  }
}

