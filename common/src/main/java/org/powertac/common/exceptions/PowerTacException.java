package org.powertac.common.exceptions;

import java.io.Serial;

/**
 * Generic PowerTAC Exception
 *
 * @author Carsten Block
 * @version 1.0, Date: 10.01.11
 */
public class PowerTacException extends RuntimeException {
  @Serial
  private static final long serialVersionUID = 3756680475439684129L;

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
