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
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ex.ConfigurationException;
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
  private PrintWriter configDumpFile = null;

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
    log.info("lazyInit");

    // Load custom (.xml and .properties) properties files
    // We need to do this before the default config and classpath props
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    Enumeration<URL> resources = null;
    try {
      resources = classloader.getResources("config/");
    } catch (IOException ioe) {
      log.error("Cannot fetch config resource", ioe.getMessage());
    }
    if (null == resources)
      return;
    URL first = resources.nextElement();
    FileFilter filter =
        file -> (file.exists() && !file.isDirectory() &&
                 !file.getName().equals("server.properties"));
    String configDir = first.getFile();
    File[] files =  new File(configDir).listFiles(filter);
    if (null == files)
      files = null;
    if (files != null) {
      for (File file : files) {
        try {
          if (file.toString().endsWith(".xml")) {
            log.debug("adding " + file.getName());
            config.addConfiguration(Configurator.readXML(file));
          }
          else if (file.toString().endsWith(".properties")) {
            log.debug("adding " + file.getName());
            config.addConfiguration(Configurator.readProperties(file));
          }
        }
        catch (Exception e) {
          log.warn("Unable to load properties file: " + file);
        }
      }
    }

    // find and load the default properties file
    try {
      File defaultProps = new File(configDir + "/server.properties");
      if (defaultProps.canRead()) {
        log.debug("adding " + defaultProps.getName());
        config.addConfiguration(Configurator.readProperties(defaultProps));
      }
    }
    catch (Exception e1) {
      log.warn("config/server.properties not found: " + e1.toString());
    }

    // set up the classpath props
    try {
      Resource[] xmlResources = context.getResources("classpath*:config/*.xml");
      for (Resource xml : xmlResources) {
        if (validXmlResource(xml)) {
          log.info("loading config from " + xml.getURI());
          config.addConfiguration(Configurator.readXML(xml.getURL()));
        }
      }
      Resource[] propResources = context.getResources("classpath*:config/*.properties");
      for (Resource prop : propResources) {
        if (validPropResource(prop)) {
          log.info("loading config from " + prop.getURI());
          config.addConfiguration(Configurator.readProperties(prop.getURL()));
        }
      }
    }
    catch (Exception e) {
      log.error("Error loading configuration: " + e.toString());
    }

    // set up the configurator
    configurator.setConfiguration(config);
    if (null != configDumpFile) {
      configurator.setConfigOutput(new ConfigurationDumper(configDumpFile));
    }
  }

  public void setUserConfig (URL userConfigURL)
          throws ConfigurationException, IOException
  {
    // then load the user-specified config
    config.addConfiguration(Configurator.readProperties(userConfigURL));
    log.debug("setUserConfig " + userConfigURL);
    lazyInit();
  }

  // Called only if we wish to dump the full config.
  // Only works if called before initialization.
  public void setConfigOutput (String filename)
  {
    if (null == filename) {
      log.error("Null filename for config dump");
      return;
    }
    try {
      configDumpFile = new PrintWriter(new File(filename));
    }
    catch(FileNotFoundException fnf) {
      log.error("Could not open config dump file {}", filename);
    }
  }

  public void finishConfigOutput ()
  {
    if (null != configDumpFile) {
      configDumpFile.close();
    }
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
      if (value instanceof String str && str.startsWith("[")) {
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
    Collection<?> result = configurator.configureInstances(target);
    return result;
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
    //thing can be a list
    //log.info("saveBootstrapState");
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
    {".*/test-classes/.*", ".*/log4j2*.xml"};

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
    excludedPaths = new String[] { ".*/log4j2*.xml" };
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

  class ConfigurationDumper implements ConfigurationRecorder
  {
    PrintWriter destination;

    ConfigurationDumper (PrintWriter output)
    {
      destination = output;
    }

    @Override
    public void recordItem (String key, Object value)
    {
      String val = value == null? "null" : value.toString();
      destination.format("%s = %s\n", key, val);
      // handy for debugging, remove for performance
      destination.flush();
    }

    @Override
    public void recordMetadata (String key, String description,
                                String valueType,
                                boolean publish, boolean bootstrapState)
    {
      destination.format("# description: %s\n", description);
      destination.format("# value type: %s\n", valueType);
      destination.format("# publish %s; bootstrap-state %s\n",
                         Boolean.toString(publish),
                         Boolean.toString(bootstrapState));
    }

    @Override
    public void recordInstanceList (String key, List<String> names)
    {
      destination.format("%s = ", key);
      String delimiter = "";
      for (String name : names) {
        destination.format("%s%s", delimiter, name);
        delimiter = ", ";
      }
      destination.println();
      // handy for debugging, remove for performance
      destination.flush();
    }
  }
}
