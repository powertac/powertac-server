package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;

import org.apache.log4j.Logger;
import org.powertac.common.Broker;
import org.powertac.common.repo.BrokerRepo;
import org.springframework.beans.factory.annotation.Autowired;

public class BrokerConverter implements SingleValueConverter
{
  private static Logger log = Logger.getLogger(BrokerConverter.class.getName());
  
  @Autowired
  private BrokerRepo brokerRepo;
  
  public boolean canConvert (Class type)
  {
    if (Broker.class.isAssignableFrom(type)) {
      return true;
    }
    else {
      log.info("BrokerConverter cannot convert ${type}");
      return false;
    }
  }

  public Object fromString (String username)
  {
    if (brokerRepo == null) {
      log.warn("no autowire BrokerRepo - using singleton");
      brokerRepo = BrokerRepo.getInstance();
    }
    return brokerRepo.findByUsername(username);
  }

  public String toString (Object broker)
  {
    return ((Broker)broker).getUsername();
  }
}
