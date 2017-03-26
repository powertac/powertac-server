package org.powertac.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.Competition;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.InitializationService;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.VisualizerMessageListener;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.common.msg.VisualizerStatusRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

/**
 * Connects the server with a Visualizer
 * @author John Collins
 */
@Service
public class VisualizerProxyService
implements VisualizerProxy, InitializationService
{
  static private Logger log = LogManager.getLogger(VisualizerProxyService.class);

  @Autowired
  private ServerConfiguration serverConfig;
  
  @Autowired
  private JmsManagementService jmsManagementService;

  @Autowired
  private JmsTemplate template;

  @Autowired
  private XMLMessageConverter converter;

  private ArrayList<VisualizerMessageListener> listeners =
      new ArrayList<VisualizerMessageListener>();
  
  @ConfigurableValue(valueType = "Boolean",
          description = "true to operate with remote visualizer")
  private boolean remoteVisualizer = false;
  
  @ConfigurableValue(valueType = "String",
          description = "name of queue for remote visualizer")
  private String visualizerQueueName = "remote-visualizer";

  private boolean remoteVizActive = false;
  
  /**
   * @param remoteVisualizer the remoteVisualizer to set
   */
  public void setRemoteVisualizer (boolean remoteVisualizer)
  {
    this.remoteVisualizer = remoteVisualizer;
  }

  /**
   * @return the visualizerQueueName
   */
  public String getVisualizerQueueName ()
  {
    return visualizerQueueName;
  }

  @Override
  public void registerVisualizerMessageListener (VisualizerMessageListener listener)
  {
    if (!listeners.contains(listener))
      listeners.add(listener);
  }

  @Override
  public void forwardMessage (Object message)
  {
    for (VisualizerMessageListener listener : listeners)
      listener.receiveMessage(message);
    if (remoteVisualizer) {
      // send messages to queue
      final String text = converter.toXML(message);
      //log.info("send " + text);

      template.send(visualizerQueueName, new MessageCreator() {
        @Override
        public Message createMessage (Session session) throws JMSException
        {
          TextMessage message = session.createTextMessage(text);
          return message;
        }
      });
    }
  }

  @Override
  public String
    initialize (Competition competition, List<String> completedInits)
  {
    serverConfig.configureMe(this);
    if (remoteVisualizer) {
      // set up the output queue
      log.info("Remote visualizer on queue " + visualizerQueueName);
      jmsManagementService.createQueue(visualizerQueueName);
    }
    return "VisualizerProxy";
  }
  
  // handle ping request from remote visualizer
  public void respondToPing ()
  {
    if (remoteVisualizer) {
      forwardMessage(new VisualizerStatusRequest());
      synchronized(this) {
        remoteVizActive  = true;
        log.info("ping received from remote viz");
        notifyAll();
      }
    }
  }
  
  /**
   * True just in case a remote visualizer has pinged this server
   */
  public boolean isActive ()
  {
    return remoteVizActive;
  }
  
  /**
   * Waits at most maxDelay for a remote visualizer to check in with a ping.
   */
  public synchronized void waitForRemoteViz (long maxDelay)
  {
    if (!remoteVisualizer || remoteVizActive)
      return;
    long start = new Date().getTime();
    boolean complete = false;
    try {
      while (!complete) {
        wait(maxDelay);
        log.info("woke up");
        if (remoteVizActive) {
          complete = true;
        }
        else {
          long now = new Date().getTime();
          if ((now - start) >= maxDelay) {
            // kill off the remote viz
            remoteVisualizer = false;
            complete = true;
          }
        }
      }
    }
    catch (InterruptedException ie) {
      log.warn("failed to hear from remote visualizer");
      remoteVisualizer = false;
    }
  }
}
