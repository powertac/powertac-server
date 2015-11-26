/*
 * Copyright (c) 2011 by the original author
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
package org.powertac.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.config.ConfigurationRecorder;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.ServerProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Implementation of the ServerProperties and ServerConfiguration interfaces.
 * @author jcollins
 */
@Service
public class ServerPropertiesService
implements ServerProperties, ServerConfiguration, ApplicationContextAware
{
  static private Logger log = LogManager.getLogger(ServerPropertiesService.class);

  private ApplicationContext context;
  private CompositeConfiguration config;
  private Configurator configurator;
  private ConfigurationPublisher publisher;
  private ConfigurationPublisher bootstrapStateRecorder = null;
  
  private boolean initialized = false;
  
  /**
   * Default constructor
   */
  public ServerPropertiesService ()
  {
    super();
    recycle();
  }

  /**
   * Come here to re-configure from scratch. This is not done as an 
   * InitializationService because it has to happen before module
   * initialization happens.
   */
  public void recycle ()
  {
    // set up the config instance
    config = new CompositeConfiguration();
    configurator = new Configurator();
    publisher = new ConfigurationPublisher();
    bootstrapStateRecorder = null;
    initialized = false;
  }
  
  /**
   * Loads the properties from classpath, default config file,
   * and user-specified config file, just in case it's not already been
   * loaded. This is done when properties are first requested, to ensure
   * that the logger has been initialized. Because the CompositeConfiguration
   * treats its config sources in FIFO order, this should be called <i>after</i>
   * any user-specified config is loaded.
   */
  void lazyInit ()
  {
    // only do this once
    if (initialized)
      return;
    initialized = true;

    // find and load the default properties file
    log.info("lazyInit");
    try {
      File defaultProps = new File("config/server.properties");
      if (defaultProps.canRead()) {
        log.debug("adding " + defaultProps.getName());
        config.addConfiguration(new PropertiesConfiguration(defaultProps));
      }
    }
    catch (Exception e1) {
      log.warn("config/server.properties not found: " + e1.toString());
    }
    
    // set up the classpath props
    try {
      Resource[] xmlResources = context.getResources("classpath*:config/properties.xml");
      for (Resource xml : xmlResources) {
        if (validXmlResource(xml)) {
          log.info("loading config from " + xml.getURI());
          XMLConfiguration xconfig = new XMLConfiguration();
          xconfig.load(xml.getInputStream());
          config.addConfiguration(xconfig);
        }
      }
      Resource[] propResources = context.getResources("classpath*:config/*.properties");
      for (Resource prop : propResources) {
        if (validPropResource(prop)) {
          log.info("loading config from " + prop.getURI());
          PropertiesConfiguration pconfig = new PropertiesConfiguration();
          pconfig.load(prop.getInputStream());
          config.addConfiguration(pconfig);
        }
      }
    }
    catch (ConfigurationException e) {
      log.error("Error loading configuration: " + e.toString());
    }
    catch (Exception e) {
      log.error("Error loading configuration: " + e.toString());
    }
    
    // set up the configurator
    configurator.setConfiguration(config);
  }
  
  public void setUserConfig (URL userConfigURL)
          throws ConfigurationException, IOException
  {
    // then load the user-specified config
    PropertiesConfiguration pconfig = new PropertiesConfiguration();
    pconfig.load(userConfigURL.openStream());
    config.addConfiguration(pconfig);
    log.debug("setUserConfig " + userConfigURL.toExternalForm());
    lazyInit();
  }

  /**
   * Adds the properties in props to the current configuration. Note that
   * it's not enough to just add the props as an additional configuration,
   * because configurations are handled in FIFO order with respect to individual
   * properties. Therefore, it's necessary to iterate through the properties
   * and set them individually.
   */
  public void addProperties (Properties props)
  {
    lazyInit();
    for (Object key : props.keySet()) {
      Object value = props.get(key);
      if (value instanceof String && ((String) value).startsWith("[")) {
        // clean up list format
        String str = (String)value;
        value = str.substring(1, str.length() - 2);
      }
      config.setProperty((String)key, value);
    }
  }

  @Override
  public void configureMe (Object target)
  {
    lazyInit();
    configurator.configureSingleton(target);
  }

  @Override
  public Collection<?> configureInstances (Class<?> target)
  {
    lazyInit();
    return configurator.configureInstances(target);
  }

  @Override
  public Collection<?> configureNamedInstances (List<?> instances)
  {
    lazyInit();
    return configurator.configureNamedInstances(instances);
  }

  @Override
  public void publishConfiguration (Object target)
  {
    //lazyInit();
    configurator.gatherPublishedConfiguration(target, publisher);
  }

  /**
   * Returns the published configuration as a Properties instance
   */
  public Properties getPublishedConfiguration ()
  {
    log.debug("published config: " + publisher.getConfig());
    return publisher.getConfig();
  }

  @Override
  public void saveBootstrapState (Object thing)
  {
    if (null == bootstrapStateRecorder)
      bootstrapStateRecorder = new ConfigurationPublisher();
    configurator.gatherBootstrapState(thing, bootstrapStateRecorder);
  }

  public Properties getBootstrapState ()
  {
    if (null == bootstrapStateRecorder)
      return new Properties();
    else 
      return bootstrapStateRecorder.getConfig();
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.ServerProperties#getProperty(java.lang.String)
   */
  @Override
  public String getProperty (String name)
  {
    lazyInit();
    return config.getString(name);
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.ServerProperties#getProperty(java.lang.String, java.lang.String)
   */
  @Override
  public String getProperty (String name, String defaultValue)
  {
    lazyInit();
    return config.getString(name, defaultValue);
  }

  @Override
  public Integer getIntegerProperty (String name, Integer defaultValue)
  {
    lazyInit();
    return config.getInteger(name, defaultValue);
  }

  @Override
  public Double getDoubleProperty (String name, Double defaultValue)
  {
    lazyInit();
    return config.getDouble(name, defaultValue);
  }

  @Override
  public void setApplicationContext (ApplicationContext context)
      throws BeansException
  {
     this.context = context;
  }
  
  /**
   * Changes the value of a property (or adds a property).
   */
  public void setProperty (String key, Object value)
  {
    lazyInit();
    config.setProperty(key, value);
  }

  // -- valid configuration resources --
  private String[] excludedPaths =
    {".*/test-classes/.*", ".*/log4j.properties"};

  private boolean validXmlResource (Resource xml)
  {
    log.debug("resource class: " + xml.getClass().getName());
    try {
      String path = xml.getURI().toString();
      for (String regex : excludedPaths) {
        if (path.matches(regex)) {
          log.debug("invalid path " + path);
          return false;
        }
      }
      return true;
    }
    catch (IOException e) {
      log.error("Should not happen: " + e.toString());
      return false;
    }
  }

  private boolean validPropResource (Resource prop)
  {
    return validXmlResource(prop);
  }

  // call this to allow test-classes to be included in valid paths
  void allowTestPaths ()
  {
    excludedPaths = new String[] { ".*/log4j.properties" };
  }

  // test support
  Configuration getConfig ()
  {
    return config;
  }

  /**
   * Configuration recorder for publishing config info to brokers
   */
  class ConfigurationPublisher implements ConfigurationRecorder
  {
    Properties publishedConfig;

    ConfigurationPublisher ()
    {
      publishedConfig = new Properties();
    }

    @Override
    public void recordItem (String key, Object value)
    {
      publishedConfig.put(key, value);      
    }

    Properties getConfig ()
    {
      return publishedConfig;
    }
  }
}
