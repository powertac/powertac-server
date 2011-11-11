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
  private String brokerQueueName;
  private String serverQueueName;
  
  public LoginResponseCmd (StatusCode stat, String addr, String serverQueueName,
      String brokerQueueName)
  {
    this.status = stat;
    this.serverAddress = addr;
    this.setServerQueueName(serverQueueName);
    this.setBrokerQueueName(brokerQueueName);
  }

  public StatusCode getStatus ()
  {
    return status;
  }

  public String getServerAddress ()
  {
    return serverAddress;
  }

  /**
   * @return the brokerQueueName
   */
  public String getBrokerQueueName ()
  {
    return brokerQueueName;
  }

  /**
   * @param brokerQueueName the brokerQueueName to set
   */
  public void setBrokerQueueName (String brokerQueueName)
  {
    this.brokerQueueName = brokerQueueName;
  }

  /**
   * @return the serverQueueName
   */
  public String getServerQueueName ()
  {
    return serverQueueName;
  }

  /**
   * @param serverQueueName the serverQueueName to set
   */
  public void setServerQueueName (String serverQueueName)
  {
    this.serverQueueName = serverQueueName;
  }
  
}
