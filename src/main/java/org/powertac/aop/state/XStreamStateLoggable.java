package org.powertac.aop.state;

public class XStreamStateLoggable
{  
  protected Object readResolve() {
    return this;
  }
}
