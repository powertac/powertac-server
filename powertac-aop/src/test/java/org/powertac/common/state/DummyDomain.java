package org.powertac.common.state;

@Domain
class DummyDomain extends XStreamStateLoggable
{
  private int number = 0;
  private String value = "";
  
  DummyDomain (int a1, String a2)
  {
    number = a1;
    value = a2;
  }

  @StateChange
  void setNumber (int arg)
  {
    number = arg;
  }

  int getNumber ()
  {
    return number;
  }

  @StateChange
  void setValue (String arg)
  {
    value = arg;
  }

  String getValue ()
  {
    return value;
  }
}
