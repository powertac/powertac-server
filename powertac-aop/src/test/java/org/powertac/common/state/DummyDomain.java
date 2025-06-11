package org.powertac.common.state;

@Domain
class DummyDomain extends XStreamStateLoggable
{
  private long id = 0;
  private int number = 0;
  private String value = "";
  
  // constructor without id value
  DummyDomain (int a1, String a2)
  {
    number = a1;
    value = a2;
  }

  //constructor with id value
  DummyDomain (int idval, int a1, String a2)
  {
    number = a1;
    value = a2;
    id = idval;
  }

  public long getId ()
  {
    return id;
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
