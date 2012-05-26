package org.powertac.server;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
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
  static private Logger log = Logger.getLogger(VisualizerProxyService.class);

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
  public void setDefaults ()
  {
    // stub
  }

  @Override
  public String
    initialize (Competition competition, List<String> completedInits)
  {
    serverConfig.configureMe(this);
    if (remoteVisualizer) {
      // set up the output queue
      //jmsManagementService.createQueue(visualizerQueueName);
    }
    return "VisualizerProxy";
  }
  
  // handle ping request from remote visualizer
  void respondToPing ()
  {
    if (remoteVisualizer) {
      forwardMessage(new VisualizerStatusRequest());
    }
  }
}
