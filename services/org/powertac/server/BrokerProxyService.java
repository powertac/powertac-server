/**
 * 
 */
package org.powertac.server;

import java.util.Collection;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.powertac.common.Broker;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.BrokerProxy;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

@Service
public class BrokerProxyService implements BrokerProxy {
  @SuppressWarnings("unused")
  private static final Log log = LogFactory.getLog(BrokerProxyService.class);
  
  @Autowired
  private JmsTemplate template;
  
  @Autowired
  private XMLMessageConverter converter;
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  @Autowired
  private MessageRouter router;
  
  @Autowired
  private MessageListenerRegistrar registrar;
  
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#sendMessage(org.powertac.common.Broker, java.lang.Object)
	 */
	public void sendMessage(Broker broker, Object messageObject) {
	  if (broker.isLocal()) {
	    broker.receiveMessage(messageObject);
	  } else {
	    final String text = converter.toXML(messageObject);
	    final String queueName = broker.toQueueName();

  	  template.send(queueName, new MessageCreator() {
        public Message createMessage(Session session) throws JMSException {
            TextMessage message = session.createTextMessage(text);
            return message;
        }
      });
	  }
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#sendMessages(org.powertac.common.Broker, java.util.List)
	 */
	public void sendMessages(Broker broker, List<?> messageObjects) {
	  for (Object message : messageObjects) {
	    sendMessage(broker, message);
	  }
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#broadcastMessage(java.lang.Object)
	 */
	public void broadcastMessage(Object messageObject) {
	  Collection<Broker> brokers = brokerRepo.list();
	  for (Broker broker : brokers) {
	    // let's be JMS provider neutral and not take advance of special queues in ActiveMQ
	    // if we have JMS performance issue, we will look into optimization using
	    // ActiveMQ special queues.
	    sendMessage(broker, messageObject);
	  }
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#broadcastMessages(java.util.List)
	 */
	public void broadcastMessages(List<?> messageObjects) {
		for (Object message : messageObjects) {
		  broadcastMessage(message);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#routeMessage(java.lang.Object)
	 */
	public void routeMessage(Object message) {
	  router.route(message);
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#registerBrokerMarketListener(org.powertac.common.interfaces.BrokerMessageListener)
	 */
	public void registerBrokerMarketListener(BrokerMessageListener listener) {
	  registrar.registerBrokerMarketListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#registerBrokerTariffListener(org.powertac.common.interfaces.BrokerMessageListener)
	 */
	public void registerBrokerTariffListener(BrokerMessageListener listener) {
	  registrar.registerBrokerTariffListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#registerSimListener(org.powertac.common.interfaces.BrokerMessageListener)
	 */
	public void registerSimListener(BrokerMessageListener listener) {
	  registrar.registerSimListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#setDeferredBroadcast(boolean)
	 */
	public void setDeferredBroadcast(boolean b) {
		// TODO Auto-generated method stub
	
	}
	
	/* (non-Javadoc)
	 * @see org.powertac.common.interfaces.BrokerProxy#broadcastDeferredMessages()
	 */
	public void broadcastDeferredMessages() {
		// TODO Auto-generated method stub
	
	}
}
