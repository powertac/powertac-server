package org.powertac.common.exceptions;

/**
 * Thrown if a shout update fails
 *
 * @author Carsten Block
 * @version 1.0, Date: 10.01.11
 */
public class ShoutUpdateException extends PowerTacException {

  private static final long serialVersionUID = 2692046525449061353L;

  public ShoutUpdateException() {
  }

  public ShoutUpdateException(String s) {
    super(s);
  }

  public ShoutUpdateException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public ShoutUpdateException(Throwable throwable) {
    super(throwable);
  }
}
