package org.powertac.common.state;

public class XStreamStateLoggable
{  
  protected Object readResolve() {
    return this;
  }
}
