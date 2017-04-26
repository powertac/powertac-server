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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;


/**
 * Logs a broker into a tournament server, retrieving tournament-specific
 * and game-specific configuration data from the server. This data can then
 * be used to log into a simulation.
 * 
 * @author Erik Onarheim
 */
@Service
public class BrokerTournamentService
{
  static private Logger log = LogManager.getLogger(BrokerTournamentService.class);

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

  public void init ()
  {
    brokerPropertiesService.configureMe(this);
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
  private void spin (int seconds)
  {
    try {
      Thread.sleep(seconds * 1000);
    }
    catch (InterruptedException e) {
      // insomnia -- unable to sleep
      e.printStackTrace();
    }
  }

  private boolean loginMaybe (String tsUrl)
  {
    if (responseType.compareTo("xml") != 0) {
      // TODO Only xml is supported by TS -- get rid of responseType config?
      log.fatal("Error: invalid responseType " + this.responseType);
      return false;
    }

    try {
      // Build proper connection string to tournament scheduler for login
      String restAuthToken = "authToken=" + this.authToken;
      String restTourneyName = "requestJoin=" + this.tourneyName;
      String restResponseType = "type=" + this.responseType;
      String finalUrl = tsUrl + "?" + restAuthToken + "&" + restTourneyName
          + "&" + restResponseType;

      System.out.printf("Connecting to TS at %s\nTournament : %s\n",
          tsUrl, tourneyName);
      log.info("Connecting to TS at " + finalUrl);
      log.info("Tournament : " + tourneyName);

      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();

      // Get the response
      InputStream input = conn.getInputStream();

      System.out.println("Parsing message..");
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory
          .newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse(input);
      doc.getDocumentElement().normalize();

      // Three different message types
      Node retryNode = doc.getElementsByTagName("retry").item(0);
      Node loginNode = doc.getElementsByTagName("login").item(0);
      Node doneNode = doc.getElementsByTagName("done").item(0);

      if (retryNode != null) {
        String checkRetry = retryNode.getFirstChild().getNodeValue();
        System.out.println("No games available at this moment");
        System.out.println("Retry in " + checkRetry + " seconds\n");
        log.info("Retry message received for : " + checkRetry
            + " seconds");

        // Received retry message spin and try again
        spin(Integer.parseInt(checkRetry));
        return false;
      }

      if (loginNode != null) {
        jmsUrl = getValue(doc, "jmsUrl");
        brokerQueueName = getValue(doc, "queueName");
        serverQueueName = getValue(doc, "serverQueue");

        System.out.printf("Login response received!" +
                "\n  jmsUrl=%s\n  queueName=%s\n  serverQueue=%s\n",
                jmsUrl, brokerQueueName, serverQueueName);
        log.info("Login response received!");
        log.info("  jmsUrl=" + jmsUrl);
        log.info("  brokerQueueName=" + brokerQueueName);
        log.info("  serverQueueName=" + serverQueueName);

        return true;
      }

      if (doneNode != null) {
        System.out.println("Received Done Message, no more games!");
        log.info("Received Done Message, no more games!");
        maxTry = 0;
        return false;
      }

      log.fatal("Invalid message type received");
      return false;
    }
    catch (Exception e) { // exception hit return false
      maxTry--;
      System.out.println("Retries left: " + maxTry);
      e.printStackTrace();
      log.fatal("Error making connection to Tournament Scheduler");
      log.fatal(e.getMessage());
      spin(20);
      return false;
    }
  }

  // Returns true on success, dies on failure
  public boolean login (String tournamentName,
                        String tsUrl,
                        String authToken,
                        long quittingTime)
  {
    this.tourneyName = tournamentName;
    this.authToken = authToken;

    if (this.authToken != null && tsUrl != null) {
      System.out.println();
      while (maxTry > 0 &&
          (quittingTime == 0l || new Date().getTime() < quittingTime)) {
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

  private String getValue (Document doc, String tag)
  {
    return doc.getElementsByTagName(tag).item(0).getFirstChild().getNodeValue();
  }
}
