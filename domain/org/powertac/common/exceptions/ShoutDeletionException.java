package org.powertac.common.exceptions;

/**
 * Thrown if a shout deletion fails
 *
 * @author Carsten Block
 * @version 1.0, Date: 10.01.11
 */
public class ShoutDeletionException extends PowerTacException {
  private static final long serialVersionUID = 2121908704033420504L;

  public ShoutDeletionException() {
  }

  public ShoutDeletionException(String s) {
    super(s);
  }

  public ShoutDeletionException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public ShoutDeletionException(Throwable throwable) {
    super(throwable);
  }
}
