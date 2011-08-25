package org.powertac.common.exceptions;

/**
 * Thrown if a position Update fails
 *
 * @author Carsten Block
 * @version 1.0, Date: 10.01.11
 */
public class PositionUpdateException extends Exception 
{
  public PositionUpdateException() {
  }

  public PositionUpdateException(String s) {
    super(s);
  }

  public PositionUpdateException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public PositionUpdateException(Throwable throwable) {
    super(throwable);
  }
}
