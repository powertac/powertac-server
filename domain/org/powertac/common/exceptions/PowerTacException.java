package org.powertac.common.exceptions;

/**
 * Generic PowerTAC Exception
 *
 * @author Carsten Block
 * @version 1.0, Date: 10.01.11
 */
public class PowerTacException extends Exception {
  public PowerTacException() {
  }

  public PowerTacException(String s) {
    super(s);
  }

  public PowerTacException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public PowerTacException(Throwable throwable) {
    super(throwable);
  }
}
