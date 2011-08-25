package org.powertac.common.msg;

/**
 * Command object sent back in response to a LoginRequestCmd to brokers
 * This object contains either a valid competition server url or an error reason
 * @author David Dauer
 */

public class LoginResponseCmd
{
  public enum StatusCode {
    OK, // Login accepted, the broker can connect to the URL in serverAddress
    OK_BUSY, // Login accepted, but no competition is scheduled. The broker should check back at a later time
    ERR_USERNAME_NOT_FOUND, // Error at login because the username was not found
    ERR_INVALID_APIKEY // Error at login because the apiKey didn't match the username
  };

  private StatusCode status;
  private String serverAddress;
  private String queueName;
  
  public LoginResponseCmd (StatusCode stat, String addr, String queue)
  {
    status = stat;
    serverAddress = addr;
    queueName = queue;
  }

  public StatusCode getStatus ()
  {
    return status;
  }

  public String getServerAddress ()
  {
    return serverAddress;
  }

  public String getQueueName ()
  {
    return queueName;
  }
  
}
