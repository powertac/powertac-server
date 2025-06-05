/*
 * Copyright 2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.powertac.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class PropertiesUtil extends PropertyPlaceholderConfigurer
{
  private Map<String, String> propertiesMap;

  @Override
  protected void processProperties (
      ConfigurableListableBeanFactory beanFactory, Properties props)
      throws BeansException
  {
    super.processProperties(beanFactory, props);

    propertiesMap = new HashMap<String, String>();
    for (Object key : props.keySet()) {
      String keyStr = key.toString();
      propertiesMap.put(keyStr,
          resolvePlaceholder(props.getProperty(keyStr), props));
    }
  }

  public String getProperty (String name)
  {
    return propertiesMap.get(name);
  }

  public String getProperty (String name, String defaultValue)
  {
    String value = propertiesMap.get(name);
    return (value != null) ? value : defaultValue;
  }
}