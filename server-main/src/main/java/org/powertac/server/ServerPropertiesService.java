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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.powertac.common.interfaces.ServerProperties;
import org.springframework.stereotype.Service;

/**
 * Implementation of the ServerProperties interface.
 * @author jcollins
 */
@Service
public class ServerPropertiesService implements ServerProperties
{
  static private Logger log = Logger.getLogger(ServerProperties.class);

  private Properties classpathProps = null;
  private Properties defaultFileProps = null;
  private Properties userFileProps = null;
  
  private boolean initialized = false;
  
  /**
   * Default constructor
   */
  public ServerPropertiesService ()
  {
    super();
  }
  
  /**
   * Loads the properties from classpath, default config file,
   * and user-specified config file, just in case it's not already been
   * loaded. This is done when properties are first requested, to ensure
   * that the logger has been initialized.
   */
  void init ()
  {
    if (initialized)
      return;
    initialized = true;
    
    // set up the classpath props
    classpathProps = new Properties();
    InputStream propStream =
        ClassLoader.getSystemResourceAsStream("server.properties");
    if (propStream != null) {
      try {
        classpathProps.load(propStream);
      }
      catch (IOException ioe) {
        log.error("Error loading server.properties: " + ioe.toString());
      }
    }
    else {
      log.error("Cannot find server.properties on classpath");
    }
    
    // see if we can find the default config file
    defaultFileProps = new Properties(classpathProps);
    try {
      FileInputStream fileStream = new FileInputStream("config/server.properties");
      defaultFileProps.load(fileStream);
    }
    catch (FileNotFoundException fnf) {
      log.warn("Cannot find config/server.properties");
    }
    catch (IOException e) {
      log.error("Error loading file config/server.properties: " + e.toString());
    }
    
    // set up user-specified config, but don't load it here
    userFileProps = new Properties(defaultFileProps);
  }
  
  public void setUserConfig (String userConfigFile)
  {
    // make sure we are initialized
    init();
    
    // then load the user-specified config
    try {
      FileInputStream fileStream = new FileInputStream(userConfigFile);
      userFileProps.load(fileStream);
    }
    catch (FileNotFoundException fnf) {
      log.error("Cannot find server config file " + userConfigFile);
    }
    catch (IOException e) {
      log.error("Error loading file " + userConfigFile + ": " + e.toString());
    }
    
  }
  
  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.ServerProperties#getProperty(java.lang.String)
   */
  public String getProperty (String name)
  {
    init();
    return userFileProps.getProperty(name);
  }

  /* (non-Javadoc)
   * @see org.powertac.common.interfaces.ServerProperties#getProperty(java.lang.String, java.lang.String)
   */
  public String getProperty (String name, String defaultValue)
  {
    init();
    return userFileProps.getProperty(name, defaultValue);
  }

  public Integer getIntegerProperty (String name, Integer defaultValue)
  {
    String value = getProperty(name);
    // error check
    Integer number = null;
    if (value != null) {
      try {
        number = Integer.parseInt(value);
      }
      catch (NumberFormatException nfe) {
        log.error("Cannot parse property " + name + "=" + value + " as Integer");
      }
    }
    if (value == null || number == null) {
      log.warn("property " + name + " not given in config");
      return defaultValue;
    }
    return number;
  }

  public Double getDoubleProperty (String name, Double defaultValue)
  {
    String value = getProperty(name);
    // error check
    Double number = null;
    if (value != null) {
      try {
        number = Double.parseDouble(value);
      }
      catch (NumberFormatException nfe) {
        log.error("Cannot parse property " + name + "=" + value + " as Double");
      }
    }
    if (value == null || number == null) {
      log.warn("property " + name + " not given in config");
      return defaultValue;
    }
    return number;
  }

}
