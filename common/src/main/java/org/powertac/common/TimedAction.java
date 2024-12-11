package org.powertac.common;

import java.time.Instant;

/**
 * Generic action type that takes time as an argument.
 * @author John Collins
 */
public interface TimedAction
{
  public void perform (Instant theTime);
}
