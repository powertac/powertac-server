package org.powertac.common.exceptions;

/**
 * Thrown if the market clearing fails
 *
 * @author Carsten Block
 * @version 1.0, Date: 10.01.11
 */
public class MarketClearingException extends Exception
{
  public MarketClearingException() {
  }

  public MarketClearingException(String s) {
    super(s);
  }

  public MarketClearingException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public MarketClearingException(Throwable throwable) {
    super(throwable);
  }
}
