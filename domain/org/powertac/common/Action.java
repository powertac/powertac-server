package org.powertac.common;

/**
 * This is a simple interface intended to be implemented by
 * anonymous classes that need to create a deferred action of
 * some sort.
 * @author John Collins
 */
public interface Action
{
  public void perform();
}
