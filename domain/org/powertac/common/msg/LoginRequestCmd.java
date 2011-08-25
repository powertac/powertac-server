package org.powertac.common.msg;

/**
 * Command object to be used by brokers to log in to the web-app
 * @author David Dauer
 */
public class LoginRequestCmd
{
  private String username;
  private String password;
  
  public LoginRequestCmd (String un, String pw)
  {
    super();
    username = un;
    password = pw;
  }

  public String getUsername ()
  {
    return username;
  }

  public String getPassword ()
  {
    return password;
  }
  
}





