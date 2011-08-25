package org.powertac.common.exceptions;

/**
 * Thrown if the creation of a new shout fails
 *
 * @author Carsten Block
 * @version 1.0, Date: 10.01.11
 */
public class ShoutCreationException extends PowerTacException {

  //TODO: Add property that is able to hold spring validation errors

  public ShoutCreationException() {
  }

  public ShoutCreationException(String s) {
    super(s);
  }

  public ShoutCreationException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public ShoutCreationException(Throwable throwable) {
    super(throwable);
  }
}
