import org.apache.activemq.broker.TransportConnector
import org.apache.activemq.xbean.XBeanBrokerService
import org.codehaus.groovy.grails.commons.ConfigurationHolder

// Place your Spring DSL code here

beans = {
  //--- JMS Configuration section ---

  switch (grails.util.GrailsUtil.environment) {
    case "test":
      jmsBroker(XBeanBrokerService) {
        useJmx = 'true'
        persistent = 'false'
        transportConnectors = [new TransportConnector(uri: new URI(ConfigurationHolder.config.powertac.broker.url))]
      }
      jmsFactory(org.apache.activemq.ActiveMQConnectionFactory) {
        brokerURL = ConfigurationHolder.config.powertac.broker.url
      }
      break
    case "development":
      jmsBroker(XBeanBrokerService) {
        useJmx = 'true'
        persistent = 'false'
        transportConnectors = [
            new TransportConnector(name: 'tcp', uri: new URI(ConfigurationHolder.config.powertac.broker.url))
        ]
      }
      jmsFactory(org.apache.activemq.ActiveMQConnectionFactory) {
        brokerURL = ConfigurationHolder.config.powertac.broker.url
      }
      break
    case "production":
      jmsBroker(XBeanBrokerService) {
        useJmx = 'true'
        persistent = 'false'
        transportConnectors = [
            new TransportConnector(name: 'tcp', uri: new URI(ConfigurationHolder.config.powertac.broker.url))
        ]
      }
      jmsFactory(org.apache.activemq.ActiveMQConnectionFactory) {
        brokerURL = ConfigurationHolder.config.powertac.broker.url
      }
      break
  }

  jmsConnectionFactory(org.apache.activemq.pool.PooledConnectionFactory) {
    connectionFactory = ref('jmsFactory')
  }

  defaultJmsTemplate(org.springframework.jms.core.JmsTemplate) {
    connectionFactory = ref("jmsConnectionFactory")
  }

  // ---  Spring Integration Configuration ---
}
