/*
 * Copyright (c) 2011-2013 by the original author
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
package org.powertac.common.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.Broker;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.spring.SpringApplicationContext;

import com.thoughtworks.xstream.converters.SingleValueConverter;

/**
 * Converts a Broker instance to its username
 * 
 * @author John Collins
 */
public class BrokerConverter implements SingleValueConverter
{
  private static Logger log = LogManager.getLogger(BrokerConverter.class.getName());
  
  private BrokerRepo brokerRepo;
  
  public BrokerConverter ()
  {
    super();
  }
  
  @Override
  @SuppressWarnings("rawtypes")
  public boolean canConvert (Class type)
  {
    if (Broker.class.isAssignableFrom(type)) {
      return true;
    }
    else {
      log.info("BrokerConverter cannot convert " + type.getName());
      return false;
    }
  }

  @Override
  public Object fromString (String username)
  {
    if (brokerRepo == null) {
      brokerRepo = (BrokerRepo) SpringApplicationContext.getBean("brokerRepo");
    }
    if (brokerRepo == null) {
      // get here if no Spring context
      log.warn("no autowire BrokerRepo - using singleton");
      brokerRepo = BrokerRepo.getInstance();
    }
    return brokerRepo.findOrCreateByUsername(username);
  }

  @Override
  public String toString (Object broker)
  {
    return ((Broker)broker).getUsername();
  }
}
