/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.customer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Builds model components from configuration. This is essentially the same
 * code as what's in evcustomer.Config, but attempts to generalize it seen
 * to run into classloader problems.
 * 
 * @author jcollins
 */
public class ModelBuilder
{
  static private Logger log =
          LogManager.getLogger(ModelBuilder.class.getName());

  private static ModelBuilder instance = null;

  private ServerConfiguration serverConfiguration;

  @ConfigurableValue(valueType = "List",
      description = "classnames of bean types to be configured")
  private List<String> beanTypes;

  private Map<String, Collection<?>> beans;

  private ModelBuilder ()
  {
    super();
  }

  /**
   * Configures this singleton when it's needed. Not called during instance
   * creation to allow testing without a full Spring setup.
   */
  public void configure ()
  {
    if (null == serverConfiguration) {
      serverConfiguration = (ServerConfiguration)
          SpringApplicationContext.getBean("serverPropertiesService");
    }
    if (null == serverConfiguration) {
      // should not happen outside of testing
      log.warn("Cannot find serverPropertiesService");
    }
    else {
      serverConfiguration.configureMe(this);
    }
  }

  /**
   * Retrieves the list of configured beans, given a package prefix and a list
   * of classnames in that package. The final argument should be an instance in
   * the module where the bean classes are found. This is where the classloader
   * comes from.
   */
  public Map<String, Collection<?>> getBeans ()
  {
    if (null != beans)
      // already configured
      return beans;
    configure();
    beans = new HashMap<String, Collection<?>>();
    for (String classname : beanTypes) {
      Class<?> clazz;
      try {
        clazz = Class.forName("org.powertac.customer.model." + classname);
        Collection<?> list = serverConfiguration.configureInstances(clazz);
        beans.put(classname, list);
        log.info("Loaded " + list.size() + " instances of " + classname);
      }
      catch (ClassNotFoundException e) {
        log.error("Cannot find class " + classname);
      } 
    }
    return beans;
  }

  /**
   * Singleton accessor
   */
  public synchronized static ModelBuilder getInstance ()
  {
    
    if (null == instance)
    {
      instance = new ModelBuilder();
      instance.configure();
    }
    return instance;
  }

  public synchronized static void recycle ()
  {
    instance = null;
  }
}
