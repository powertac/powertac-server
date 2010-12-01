package org.powertac.common.enumerations;

/**
 * Defines buy or sell flaf used to mark orders (shouts) as buy or sell orders
 *
 * @author Carsten Block
 * @since 0.5
 * @version 1
 * First created: 02.05.2010
 * Last Updated: 02.05.2010
 */
public enum BuySellIndicator {
  BUY(1),
  SELL(-1);

  private final int idVal;

  BuySellIndicator(int id) {this.idVal = id; }
  public int getId() { return idVal; }
}
