import org.apache.activemq.broker.TransportConnector
import org.apache.activemq.xbean.XBeanBrokerService
import org.codehaus.groovy.grails.commons.ConfigurationHolder

// Place your Spring DSL code here

beans = {
  //--- JMS Configuration section ---
  jmsBroker(XBeanBrokerService) {
    useJmx = 'true'
    persistent = 'false'
    tmpDataDirectory = "/tmp"
    transportConnectors = [
        new TransportConnector(name: 'tcp', uri: new URI(ConfigurationHolder.config.powertac.broker.url))
    ]
  }

  jmsConnectionFactory(org.apache.activemq.pool.PooledConnectionFactory) {bean ->
    bean.destroyMethod = "stop"
    connectionFactory = {org.apache.activemq.ActiveMQConnectionFactory cf ->
      brokerURL = ConfigurationHolder.config.powertac.broker.url
    }
  }

  messageConverter(org.powertac.common.MessageConverter) { bean ->
    bean.initMethod = 'afterPropertiesSet'
  }
}
