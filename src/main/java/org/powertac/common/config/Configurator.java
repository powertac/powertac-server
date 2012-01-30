/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.common.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

/**
 * Fills in configured values from configuration source based on 
 * annotations in the source code.
 * <p>
 * Configuration clauses are assumed to be of the form<br/>
 *   &nbsp; &nbsp; pkg.class[.instance].property = value<br/>
 * where pkg is the package name without org.powertac, class is the
 * classname, instance is the optional instance name, and property is the
 * property name.</p>
 * <p>
 * In the server, an instance of this class is typically created by a service
 * that collects configuration data, creates and initializes a Configurator,
 * then waits for each service to ask for its configuration.</p>
 * @author John Collins
 */
public class Configurator
{
  static private Logger log = Logger.getLogger(Configurator.class);

  private Configuration config;
  
  // For each class we have encountered, we keep a mapping between the
  // property names represented by the configurable setter methods 
  // (those having a prefix of "set" or "with") and their 
  // ConfigurableValue annotations.
  @SuppressWarnings("rawtypes")
  private HashMap<Class, HashMap<String, ConfigurableProperty>> annotationMap;
  
  @SuppressWarnings("rawtypes")
  public Configurator ()
  {
    super();
    annotationMap = new HashMap<Class, HashMap<String, ConfigurableProperty>>();
  }
  
  /**
   * Loads a Configuration into this Configurator.
   */
  public void setConfiguration (Configuration config)
  {
    this.config = config;
  }
  
  /**
   * Configures the given thing, using the pre-loaded Configuration.
   */
  public void configureSingleton (Object thing)
  {
    // If we don't have a configuration, we cannot do much.
    if (config == null) {
      log.error("Cannot configure - not Configuration set");
      return;
    }
    
    // first, compute the property prefix for things of this type
    String classname = thing.getClass().getName();
    log.debug("configuring object of type " + classname);
    String[] classnamePath = classname.split("\\.");
    if (!classnamePath[0].equals("org") || !classnamePath[1].equals("powertac")) {
      log.error("Cannot set properties for instance of type " + classname);
      return;
    }
    // Next, turn the remainder of the classname into a properties prefix.
    Configuration subset = extractConfigForClass(classnamePath);
    // At this point, each key is either a property name, or an
    // instance name, or an instance name followed by a property name.
    // If they are all property names, we can proceed with the configuration.
    if (isSingletonConfig(subset)) {
      configureInstance(thing, subset);
    }
    else {
      // multi-instance config - should not get here
    }
  }
  
  /**
   * Creates and configures instances of the given class. In the configuration,
   * we expect to see a property of the form pkg.class.instances = a, b, c, ...
   * where a, b, c, etc. are the names of instances of the class to be created
   * and configured. These names are provided to the instances through their
   * constructors, and are expected to show up in the configuration as
   * instance names in clauses of the form
   * pkg.class.name.property = value.
   * Returns null in case instances cannot be created.
   */
  public Collection<?> configureInstances (Class<?> type)
  {
    // If we don't have a configuration, we cannot do much.
    if (config == null) {
      log.error("Cannot configure - not Configuration set");
      return null;
    }
    
    // compute the key for the instance list
    String classname = type.getName();
    log.debug("configuring instances for type " + classname);
    String[] classnamePath = classname.split("\\.");
    if (!classnamePath[0].equals("org") || !classnamePath[1].equals("powertac")) {
      log.error("Cannot configure instances of type " + classname);
      return null;
    }
    Configuration subset = extractConfigForClass(classnamePath);
    // we should have a class with the key "instances" giving the item
    // names, and a set of clauses for each item
    List<?> names = subset.getList("instances");
    if (names.size() == 0) {
      log.error("No instance names specified for class " + classname);
      return null;
    }
    // for each name, create an instance, add it to the result, and
    // configure it.
    HashMap<String, Object> itemMap = new HashMap<String, Object>();
    for (Object name : names) {
      try {
        Constructor<?> constructor = type.getConstructor(String.class);
        Object item = constructor.newInstance((String)name);
        itemMap.put((String)name, item);
      }
      catch (Exception e) {
        log.error("Unable to create instance " + name + " of class " + type +
                  ": " + e.toString());
      }
    }
    // We now have a map of names and items to be configured.
    // We iterate through the map to avoid problems with items that did
    // not get created, although that seems far-fetched.
    for (String name : itemMap.keySet()) {
      configureInstance(itemMap.get(name), subset.subset(name));
    }
    return itemMap.values();
  }

  private Configuration extractConfigForClass (String[] classnamePath)
  {
    // Note that the classname must be lower-cased.
    StringBuilder sb = new StringBuilder();
    for (int i = 2; i < (classnamePath.length - 1); i++) {
      sb.append(classnamePath[i]).append(".");
    }
    sb.append(decapitalize(classnamePath[classnamePath.length - 1]));
    String prefix = sb.toString();
    log.debug("config prefix " + prefix);
    // pull in the subset config starting with this prefix
    Configuration subset = config.subset(prefix);
    log.debug("config subset empty: " + subset.isEmpty());
    return subset;
  }
  
  private boolean isSingletonConfig (Configuration conf)
  {
    boolean result = true;
    for (Iterator<?> keys = conf.getKeys(); keys.hasNext() && result; ) {
      String key = (String)keys.next();
      if (key.contains("."))
        result = false;
    }
    return result;
  }
  
  /**
   * Configures a single instance with a Configuration in which each
   * key is a simple property name.
   */
  private void configureInstance (Object thing, Configuration conf)
  {
    Map<String, ConfigurableProperty> analysis = getAnalysis(thing.getClass());
    for (Iterator<?> keys = conf.getKeys(); keys.hasNext(); ) {
      String key = (String)keys.next();
      log.debug("Configuring property " + key);
      // key is a property name. Let's find the property.
      ConfigurableProperty cp = analysis.get(key);
      if (cp == null) {
        log.error("no configurable property named " + key +
                  " on class " + thing.getClass().getName());
      }
      else {
        // Convert the value to the correct type, then call the method
        ConfigurableValue cv = cp.cv;
        String type = cv.valueType();
        try { // lots of exceptions possible here
          Class<?> clazz = Class.forName("java.lang." + type);
          Object defaultValue = cp.getter.invoke(thing);
          String extractorName = "get" + type;
          Method extractor = conf.getClass().getMethod(extractorName, String.class, clazz);
          Object configValue = extractor.invoke(conf, key, defaultValue);
          cp.setter.invoke(thing, configValue);
        }
        catch (ClassNotFoundException cnf) {
          log.error("Class " + type + " not found");
        }
        catch (IllegalArgumentException e) {
          log.error(e.toString());
        }
        catch (IllegalAccessException e) {
          log.error(e.toString());
        }
        catch (InvocationTargetException e) {
          log.error(e.toString());
        }
        catch (NoSuchMethodException e) {
          log.error(e.toString());
        }
      }
    }
  }
  
  /**
   * Analyzes a class by mapping its configurable properties.
   */
  private Map<String, ConfigurableProperty>
  getAnalysis (Class<? extends Object> clazz)
  {
    HashMap<String, ConfigurableProperty> result = annotationMap.get(clazz);
    if (result == null) {
      result = new HashMap<String, ConfigurableProperty>();
      annotationMap.put(clazz, result);
      // here's where we do the analysis
      log.debug("Analyzing class " + clazz.getName());
      Method[] methods = clazz.getMethods();
      Method getter = null;
      for (int i = 0; i < methods.length; i++) {
        ConfigurableValue cv = methods[i].getAnnotation(ConfigurableValue.class);
        if (cv != null) {
          // This method is annotated
          log.debug("ConfigurableValue method found on " + methods[i].getName());
          String propertyName = cv.name();
          if ("".equals(propertyName)) {
            // property name not in annotation, extract from method name
            propertyName = extractPropertyName(methods[i]);            
          }
          // locate the getter
          String getterName = cv.getter();
          if ("".equals(getterName)) {
            // compose getter name from property name
            StringBuilder sb = new StringBuilder();
            sb.append("get").append(capitalize(propertyName));
            getterName = sb.toString();
            log.debug("getter name " + getterName);
            try {
              getter = clazz.getMethod(getterName);
            }
            catch (NoSuchMethodException nsm) {
              log.error("No getter method " + getterName +
                        " for " + clazz.getName());
            }
          }
          result.put(propertyName,
                     new ConfigurableProperty(methods[i], getter, cv));
        }
      }
    }
    return result;
  }
  
  /**
   * Extracts a property name from a method description
   */
  private String extractPropertyName (Method method)
  {
    String name = method.getName();
    if (name.startsWith("set")) {
      log.debug ("removing 'set' from " + name);
      name = name.substring(3);
    }
    else if (name.startsWith("with")) {
      log.debug ("removing 'with' from " + name);
      name = name.substring(4);
    }
    return decapitalize(name);
  }

  private String decapitalize (String name)
  {
    char first = name.charAt(0);
    StringBuilder sb = new StringBuilder();
    sb.append(Character.toLowerCase(first)).append(name.substring(1));
    return sb.toString();
  }

  private String capitalize (String name)
  {
    char first = name.charAt(0);
    StringBuilder sb = new StringBuilder();
    sb.append(Character.toUpperCase(first)).append(name.substring(1));
    return sb.toString();
  }
  
  // Data holder - association between Method and ConfigurableValue
  class ConfigurableProperty
  {
    Method setter;
    Method getter;
    ConfigurableValue cv;
    
    ConfigurableProperty (Method setter, Method getter, ConfigurableValue cv)
    {
      super();
      this.setter = setter;
      this.getter = getter;
      this.cv = cv;
    }
  }
}
