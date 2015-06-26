/*
 * Copyright (c) 2012 by the original author
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.samplebroker.core;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
/**
 * @author Erik Onarheim
 */
@Service
public class BrokerTournamentService
{
  static private Logger log = Logger.getLogger(BrokerTournamentService.class);

  @Autowired
  private BrokerPropertiesService brokerPropertiesService;

  // The game specific token is only valid for one game
  //private String gameToken = null;
  private String jmsUrl = null;
  private String brokerQueueName = null;
  private String serverQueueName = null;

  // Configurable parameters
  private String authToken = null;
  private String tourneyName = null;

  @ConfigurableValue(valueType = "String",
          description = "Response type to receive from the TS xml or json")
  private String responseType = "xml";

  // If set to negative number infinite retries
  @ConfigurableValue(valueType = "Integer",
          description = "Maximum number of tries to connect to Tournament Scheduler")
  private int maxTry = 50;

  public void init()
  {
    brokerPropertiesService.configureMe(this);
  }

  public String getResponseType()
  {
    return responseType;
  }

  public int getMaxTry()
  {
    return maxTry;
  }
  
  public String getJmsUrl ()
  {
    return jmsUrl;
  }
  
  public String getServerQueueName ()
  {
    return serverQueueName;
  }
  
  public String getBrokerQueueName ()
  {
    return brokerQueueName;
  }

  // Spins current login attemt for n seconds and url to retry
  private void spin(int seconds)
  {
    try {
      Thread.sleep(seconds * 1000);
    } catch (InterruptedException e) {
      // insomnia -- unable to sleep
      e.printStackTrace();
    }
  }

  private boolean loginMaybe(String tsUrl)
  {
    try {
      // Build proper connection string to tournament scheduler for
      // login
      String restAuthToken = "authToken=" + this.authToken;
      String restTourneyName = "requestJoin=" + this.tourneyName;
      String restResponseType = "type=" + this.responseType;
      String finalUrl = tsUrl + "?" + restAuthToken + "&" + restTourneyName + "&"
              + restResponseType;
      log.info("Connecting to TS with " + finalUrl);
      log.info("Tournament : " + this.tourneyName);

      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();

      // Get the response
      InputStream input = conn.getInputStream();

      if (this.responseType.compareTo("xml") == 0) {
        System.out.println("Parsing message..");
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder docBuilder = docBuilderFactory
                .newDocumentBuilder();
        Document doc = docBuilder.parse(input);

        doc.getDocumentElement().normalize();
        System.out.println("login response: " + doc.toString());

        // Three different message types
        Node retryNode = doc.getElementsByTagName("retry").item(0);
        Node loginNode = doc.getElementsByTagName("login").item(0);
        Node doneNode = doc.getElementsByTagName("done").item(0);

        if (retryNode != null) {
          String checkRetry = retryNode.getFirstChild()
                  .getNodeValue();
          log.info("Retry message received for : " + checkRetry
                   + " seconds");
          System.out.println("Retry message received for : "
                  + checkRetry + " seconds");
          // Received retry message spin and try again
          spin(Integer.parseInt(checkRetry));
          return false;
        }
        else if (loginNode != null) {
          System.out.println("Login response received!");
          log.info("Login response received! ");

          String checkJmsUrl = doc.getElementsByTagName("jmsUrl").item(0).getFirstChild().getNodeValue();
          jmsUrl = checkJmsUrl;
          log.info("jmsUrl=" + checkJmsUrl);

          String checkBrokerQueue = doc.getElementsByTagName("queueName").item(0).getFirstChild().getNodeValue();
          brokerQueueName = checkBrokerQueue;
          log.info("brokerQueueName=" + checkBrokerQueue);

          String checkServerQueue = doc.getElementsByTagName("serverQueue").item(0).getFirstChild().getNodeValue();
          serverQueueName = checkServerQueue;
          log.info("serverQueueName=" + checkServerQueue);

          System.out.printf("Login message receieved!\n  jmsUrl=%s\n  queueName=%s\n  serverQueue=%s\n",
                            checkJmsUrl, checkBrokerQueue, checkServerQueue);
          return true;
        }
        else if (doneNode != null) {
          System.out.println("Recieved Done Message no more games!");
          maxTry=0;
          return false;
        }
        else {
          log.fatal("Invalid message type recieved");
          return false;
        }
      }
      else { // response type was json parse accordingly
        String jsonTxt = IOUtils.toString(input);

        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonTxt);
        int retry = json.getInt("retry");
        System.out.println("Retry message received for : " + retry
                           + " seconds");
        spin(retry);
        return false;

        // TODO: Good Json Parsing
        // JEC: not sure why this is important...
      }
    }
    catch (Exception e) { // exception hit return false
      maxTry--;
      System.out.println("Retries left: " + maxTry);
      e.printStackTrace();
      log.fatal("Error making connection to Tournament Scheduler");
      log.fatal(e.getMessage());
      // Sleep and wait for network
      try {
        Thread.sleep(20000);
      }
      catch (InterruptedException e1) {
        e1.printStackTrace();
        return false;
      }
      return false;
    }
  }

  // Returns true on success, dies on failure
  public boolean login(String tournamentName,
                       String tsUrl,
                       String authToken,
                       long quittingTime)
  {
    this.tourneyName = tournamentName;
    this.authToken = authToken;
    
    if (this.authToken != null && tsUrl != null) {
      while (maxTry > 0 &&
              (quittingTime == 0l || new Date().getTime() < quittingTime)) {
        System.out.println("Connecting to TS at " + tsUrl);
        System.out.println("Tournament : " + tourneyName);

        if (loginMaybe(tsUrl)) {
          log.info("Login Successful!");
          return true;
        }
      }
      System.out.println("Max attempts reached...shutting down");
      log.fatal("Max attempts to log in reached");
      System.exit(0);
    }
    else {
      log.fatal("Incorrect Tournament Scheduler URL or Broker Auth Token");
      System.exit(0);
    }
    return false;
  }
}
