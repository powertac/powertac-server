/*
 * Copyright (c) 2012-2013 by the original author
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

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.convert.ListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.BasePathLocationStrategy;
import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.CombinedLocationStrategy;
import org.apache.commons.configuration2.io.FileLocationStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.XMLMessageConverter;
import org.powertac.util.Predicate;

/**
 * Fills in configured values from configuration source based on 
 * annotations in the source code.
 * <p>
 * Configuration clauses are assumed to be of the form<br/>
 *   &nbsp; &nbsp; pkg.class[.instance].property = value<br/>
 * where pkg is the package name without org.powertac
 * (or the full classname for classes outside the org.powertac hierarchy), 
 * class is the classname (but the first character is lowercase),
 * instance is an optional instance name,
 * and property is the property name. 
 * The target property must be annotated with {@link ConfigurableValue},
 * either on the property itself, or on a setter method.
 * For example, the following property will set the jmsBrokerUrl property
 * of the class JmsManagementService.</p>
 * <p>
 * <code>server.jmsManagementService.jmsBrokerUrl = tcp://localhost:61616</code>
 * </p>
 * <p>
 * In the server, an instance of this class is typically created by a service
 * that collects configuration data, creates and initializes a Configurator,
 * then waits for each service to ask for its configuration. There are many
 * examples of configuration settings in the server properties file.</p>
 * <p>
 * When used in a broker, classes outside the org.powertac tree can be used
 * if the full package prefix is specified. For example, the following
 * property specification will set the foo property of class edu.umn.Bar:</p>
 * <p>
 * <code>edu.umn.bar.foo = 42</code></p>
 * 
 * @author John Collins
 */
public class Configurator
{
  static private Logger log = LogManager.getLogger(Configurator.class);

  private Configuration config;

  // For each class we have encountered, we keep a mapping between the
  // property names represented by the configurable setter methods 
  // (those having a prefix of "set" or "with") and their 
  // ConfigurableValue annotations.
  //@SuppressWarnings("rawtypes")
  private HashMap<Class<?>, HashMap<String, ConfigurableProperty>> annotationMap;
  private HashSet<String> metadataSet;
  private ConfigurationRecorder configDump = null;

  // To avoid duplication, we keep track of the names of instances we have
  // already configured.
  private HashMap<Class<?>, Set<String>> createdInstances;
  private HashMap<Class<?>, Set<String>> configuredProps;

  private static final FileLocationStrategy fileLocationStrategy;
  private static final ListDelimiterHandler listDelimiterHandler;
  static {
    LinkedList<FileLocationStrategy> strategies = new LinkedList<>();
    strategies.add(new ClasspathLocationStrategy());
    strategies.add(new BasePathLocationStrategy());
    fileLocationStrategy = new CombinedLocationStrategy(strategies);
    listDelimiterHandler = new DefaultListDelimiterHandler(',');
  }

  public final static XMLConfiguration readXML (String path)
  throws ConfigurationException
  {
    return new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
      .configure(
        new Parameters().xml()
        .setBasePath(".")
        .setFileName(path)
        .setLocationStrategy(fileLocationStrategy)
        .setListDelimiterHandler(listDelimiterHandler)
      )
      .getConfiguration();
  }

  public final static XMLConfiguration readXML (File file)
  throws ConfigurationException
  {
    return new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
      .configure(
        new Parameters().xml()
        .setFile(file)
        .setLocationStrategy(fileLocationStrategy)
        .setListDelimiterHandler(listDelimiterHandler)
      )
      .getConfiguration();
  }

  public final static XMLConfiguration readXML (URL url)
  throws ConfigurationException
  {
    return new FileBasedConfigurationBuilder<>(XMLConfiguration.class)
      .configure(
        new Parameters().xml()
        .setURL(url)
        .setListDelimiterHandler(listDelimiterHandler)
      )
      .getConfiguration();
  }

  public final static PropertiesConfiguration readProperties (String path)
  throws ConfigurationException
  {
    return new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
      .configure(
        new Parameters().fileBased()
        .setBasePath(".")
        .setFileName(path)
        .setLocationStrategy(fileLocationStrategy)
        .setListDelimiterHandler(listDelimiterHandler)
      )
      .getConfiguration();
  }

  public final static PropertiesConfiguration readProperties (URL url)
  throws ConfigurationException
  {
    return new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
      .configure(
        new Parameters().fileBased()
        .setURL(url)
        .setListDelimiterHandler(listDelimiterHandler)
      )
      .getConfiguration();
  }

  public final static PropertiesConfiguration readProperties (File file)
  throws ConfigurationException
  {
    return new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
      .configure(
        new Parameters().fileBased()
        .setFile(file)
        .setLocationStrategy(fileLocationStrategy)
        .setListDelimiterHandler(listDelimiterHandler)
      )
      .getConfiguration();
  }

  public Configurator ()
  {
    super();
    annotationMap = new HashMap<>();
    metadataSet = new HashSet<>();
    createdInstances = new HashMap<>();
    configuredProps = new HashMap<>();
  }

  /**
   * Loads a Configuration into this Configurator.
   */
  public void setConfiguration (AbstractConfiguration config)
  {
    // https://commons.apache.org/proper/commons-configuration/userguide/upgradeto2_0.html
    if (config.getListDelimiterHandler() instanceof DisabledListDelimiterHandler) {
      config.setListDelimiterHandler(listDelimiterHandler);
    }
    this.config = config;
  }

  /**
   * Sets up an output stream for config dump.
   */
  public void setConfigOutput (ConfigurationRecorder configOutput)
  {
    configDump = configOutput;
  }

  /**
   * Configures the given thing, using the pre-loaded Configuration.
   */
  public void configureSingleton (Object thing)
  {
    // If we don't have a configuration, we cannot do much.
    if (config == null) {
      log.error("Cannot configure - no Configuration set");
      return;
    }
    
    // first, compute the property prefix for things of this type
    String[] classnamePath = extractClassnamePath(thing);
    if (classnamePath == null)
      return;

    // Next, turn the remainder of the classname into a properties prefix.
    Configuration subset = extractConfigForClass(classnamePath);
    // At this point, each key is either a property name, or an
    // instance name, or an instance name followed by a property name.
    // If they are all property names, we can proceed with the configuration.
    if (isSingletonConfig(subset)) {
      configureInstance(thing, subset, null);
    }
    else {
      // multi-instance config - should not get here
      log.error("Attempt to configure Singleton with multi-instance config data");
    }
  }

  private String[] extractClassnamePath (Object thing)
  {
    String classname = thing.getClass().getName();
    log.debug("configuring object of type " + classname);
    String[] classnamePath = classname.split("\\.");
    //if (!classnamePath[0].equals("org") || !classnamePath[1].equals("powertac")) {
    //  log.error("Cannot set properties for instance of type " + classname);
    //  return null;
    //}
    return classnamePath;
  }

  /**
   * Creates and configures instances of the given class. In the configuration,
   * we expect to see a property of the form pkg.class.instances = a, b, c, ...
   * where a, b, c, etc. are the names of instances of the class to be created
   * and configured. These names are provided to the instances through their
   * constructors, and are expected to show up in the configuration as
   * instance names in clauses of the form
   * pkg.class.name.property = value.
   * Returns empty list in case instances cannot be created.
   */
  public Collection<?> configureInstances (Class<?> type)
  {
    // If we don't have a configuration, we cannot do much.
    if (config == null) {
      log.error("Cannot configure - no Configuration set");
      return new ArrayList<Object>();
    }

    // compute the key for the instance list
    String classname = type.getName();
    log.debug("configuring instances for type " + classname);
    Configuration subset = extractSubsetForClassname(classname);
    // we should have a clause with the key "instances" giving the item
    // names, and a set of clauses for each item
    if (null == createdInstances.get(type)) {
      createdInstances.put(type, new HashSet<>());
    }
    Set<String> existingNames = createdInstances.get(type);
    List<Object> rawNames = subset.getList("instances");
    List<String> names =
        rawNames.stream()
          .map(n -> n.toString())
          .filter(n -> !n.isEmpty())
          .collect(Collectors.toList());
    if (names.size() == 0) {
      log.warn("No instance names specified for class " + classname);
      return names;
    }
    dumpInstanceListMaybe(type, existingNames, names);

    // for each name, create an instance, add it to the result, and
    // configure it.
    LinkedHashMap<String, Object> itemMap = new LinkedHashMap<String, Object>();
    for (String name : names) {
      existingNames.add(name);
      try {
        Constructor<?> constructor = type.getConstructor(String.class);
        Object item = constructor.newInstance(name);
        itemMap.put(name, item);
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
      configureInstance(itemMap.get(name), subset.subset(name), name);
    }
    return itemMap.values();
  }

  // if we are dumping configuration, we need to dump this list, but only
  // for the names not already dumped
  private void dumpInstanceListMaybe (Class<?> type,
                                      Set<String> existingNames,
                                      List<String> names)
  {
    List<String> dumpNames =
        names.stream()
        .filter(n -> !existingNames.contains(n))
        .collect(Collectors.toList());
    if (dumpNames.size() > 0) {
      dumpInstanceListMaybe(type, dumpNames);
    }
  }

  /**
   * Configures a set of instances of some class. In the configuration,
   * we expect to see properties of the form
   * pkg.class.name.property = value
   * where name is the name of an instance in the list to be
   * configured. These names must be accessible through a getName() method
   * on each instance.
   * It is an error if instances are not of the same class; in fact, the
   * class of the first instance in the list is used to form the pkg.class.
   * It also does not work for types that lack a getName() method returning
   * a String, or if the name does not match the name in the properties.
   * portion of the property names.
   */
  public Collection<?> configureNamedInstances (List<?> instances)
  {
    // If we don't have a configuration, we cannot do much.
    if (config == null) {
      log.error("Cannot configure - no Configuration set");
      return null;
    }

    // We need a non-empty list of instances for this to work
    if (null == instances || 0 == instances.size()) {
      log.error("Cannot configure empty instance list");
      return null;
    }

    // compute the key for the instance list from the first element of the list
    String classname = instances.get(0).getClass().getName();
    log.debug("configuring instances for type " + classname);
    Configuration subset = extractSubsetForClassname(classname);

    // for each given instance, get it's name and configure it
    try {
      Method getNameMethod = instances.get(0).getClass().getMethod("getName");
      for (Object item : instances) {
        Object result = getNameMethod.invoke(item);
        String name = (String)result;
        configureInstance(item, subset.subset(name), name);
      }
    }
    catch (Exception e) {
      log.error("Could not get name of item");
      return null;
    }
    return instances;
  }

  private Configuration extractSubsetForClassname (String classname)
  {
    String[] classnamePath = classname.split("\\.");
    Configuration subset = extractConfigForClass(classnamePath);
    return subset;
  }

  /**
   * Pulls the "published" ConfigurableValues out of object thing, adds them to
   * config.
   */
  public void gatherPublishedConfiguration (Object thing,
                                            ConfigurationRecorder recorder)
  {
    gatherConfiguration(thing, null, recorder,
                        prop -> prop.cv.publish());
  }

  /**
   * Pulls the "bootstrapState" ConfigurableValues out of object thing, adds
   * them to config.
   */
  @SuppressWarnings("unchecked")
  public void gatherBootstrapState (Object thing,
                                    ConfigurationRecorder recorder)
  {
    if (thing instanceof List) {
      gatherBootstrapList((List<Object>)thing, recorder);
    }
    else {
    gatherConfiguration(thing, null, recorder,
                        prop -> prop.cv.bootstrapState());
    }
  }

  /**
   * Pulls "bootstrapState" ConfigurationValues out of objects in the given
   * list, adds them to config with their names. This requires that the
   * instances in the list each have a getName() method that returns a String.
   * Any instance lacking such a method will not be recorded, and an error
   * message logged.
   */
  public void gatherBootstrapList(List<Object> things,
                                  ConfigurationRecorder recorder)
  {
    for (Object thing : things) {
      Class<?> thingClass = thing.getClass();
      try {
        Method getNameMethod = thingClass.getMethod("getName");
        Object name = getNameMethod.invoke(thing);
        if (null == name) {
          log.error("Null name for " + thing.toString());
        }
        else {
          //log.info("gathering bootstrap state for " + thingClass.getName()
          //         + " " + name);
          gatherConfiguration(thing, (String)name, recorder,
                              prop -> prop.cv.bootstrapState());
        }
      }
      catch (NoSuchMethodException e) {
        log.error("Cannot extract bootstrap state from "
                  + thing.toString() + ": no getName() method");
      }
      catch (SecurityException e) {
        log.error("Cannot extract bootstrap state from "
            + thing.toString() + ": security exception");
      }
      catch (Exception e) {
        log.error(e.toString() + " calling thing.getName()");
      }
    }
  }

  private void gatherConfiguration (Object thing, String name,
                                    ConfigurationRecorder recorder,
                                    Predicate<ConfigurableProperty> p)
  {
    String prefix =
        extractClassnamePrefix(extractClassnamePath(thing));
    Map<String, ConfigurableProperty> analysis = getAnalysis(thing.getClass());
    for (Iterator<Entry<String, ConfigurableProperty>> props = analysis.entrySet().iterator();
        props.hasNext(); ) {
      Map.Entry<String, ConfigurableProperty> prop = props.next();
      ConfigurableProperty cp = prop.getValue();
      if (p.apply(cp)) {
        String key = prefix;
        if (null != name) {
          // handle named instances
          // TODO - this is probably a bad way to detect these...
          key = key + "." + name;
        }
        key = key + "." + prop.getKey();
        log.debug("Recording property " + key);
        Object value = null;
        try {
          if (cp.field != null) {
            // extract value from field
            cp.field.setAccessible(true);
            value = cp.field.get(thing);
          }
          else if (cp.getter != null) {
            value = cp.getter.invoke(thing);
          }
          else {
            // cannot do much
            throw new Exception("field and getter both null");
          }
          recorder.recordItem(key, value);
          recorder.recordMetadata(key, cp.cv.description(),
                                  cp.cv.valueType(),
                                  cp.cv.publish(),
                                  cp.cv.bootstrapState());
        }
        catch (IllegalArgumentException e) {
          log.error("cannot read published value: " + e.toString());
        }
        catch (IllegalAccessException e) {
          log.error("cannot read published value: " + e.toString());
        }
        catch (InvocationTargetException e) {
          log.error("cannot read published value: " + e.toString());
        }
        catch (Exception e) {
          log.error("cannot read published value: " + e.toString());
        }
      }
    }
  }

  private Configuration extractConfigForClass (String[] classnamePath)
  {
    String prefix = extractClassnamePrefix(classnamePath);
    log.debug("config prefix " + prefix);
    // pull in the subset config starting with this prefix
    Configuration subset = config.subset(prefix);
    log.debug("config subset empty: " + subset.isEmpty());
    return subset;
  }

  private String extractClassnamePrefix (String[] classnamePath)
  {
    // Note that the classname must be lower-cased.
    StringBuilder sb = new StringBuilder();
    // discard the "org" and "powertac" elements
    int startIndex = 2;
    if (!(classnamePath[0].equals("org") && classnamePath[1].equals("powertac"))) {
      // handle classes outside org.powertac
      startIndex = 0;
    }
    for (int i = startIndex; i < (classnamePath.length - 1); i++) {
      sb.append(classnamePath[i]).append(".");
    }
    sb.append(decapitalize(classnamePath[classnamePath.length - 1]));
    String prefix = sb.toString();
    return prefix;
  }
  
  private boolean isSingletonConfig (Configuration conf)
  {
    boolean result = true;
    for (Iterator<?> keys = conf.getKeys(); keys.hasNext() && result; ) {
      String key = (String)keys.next();
      if (key.contains("instances"))
        result = false;
    }
    return result;
  }
  
  /**
   * Configures a single instance with a Configuration in which each
   * key is a simple property name. For named instances the name is expected
   * to be non-null; it will be used to compose property keys in the case where
   * we are dumping the full configuration.
   */
  private void configureInstance (Object thing, Configuration conf, String name)
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
        configureValue(thing, conf, key, cp);
      }
    }
    dumpConfigMaybe(thing, analysis, name);
  }

  // Here's where we actually set the value
  private void configureValue (Object thing, Configuration conf, String key,
                               ConfigurableProperty cp)
  {
    ConfigurableValue cv = cp.cv;
    String type = cv.valueType();
    try { // lots of exceptions possible here
      Object defaultValue = null;
      if (cp.field != null) {
        // handle configurable field
        cp.field.setAccessible(true);
        defaultValue = cp.field.get(thing);
        Object configValue = extractConfigValue(conf, key, type, defaultValue);
        cp.field.set(thing, configValue);
      }
      else if (cp.setter != null) {
        if (cp.getter != null)
          defaultValue = cp.getter.invoke(thing);
        Object configValue = extractConfigValue(conf, key, type, defaultValue);
        cp.setter.invoke(thing, configValue);
      }
      else {
        // no field, no setter - should not happen
        log.error("No field, no method to set cv " + key);
      }
    }
    catch (ClassNotFoundException cnf) {
      log.error("Class " + type + " not found");
    }
    catch (IllegalArgumentException e) {
      log.error("cannot configure {}: {}", key, getExceptionDetails(e));
    }
    catch (IllegalAccessException e) {
      log.error("cannot configure {}: {}", key, getExceptionDetails(e));
    }
    catch (InvocationTargetException e) {
      log.error("cannot configure {}: {}, {}",
                key, e.getCause(), getExceptionDetails(e));
    }
    catch (NoSuchMethodException e) {
      log.error("cannot configure {}: {}", key, getExceptionDetails(e));
    }
  }

  private Object extractValue (Object thing, ConfigurableProperty cp)
  {
    ConfigurableValue cv = cp.cv;
    Object result = null;
    String key = cp.cv.name();
    try { // lots of exceptions possible here
      if (cp.field != null) {
        // handle configurable field
        cp.field.setAccessible(true);
        result = cp.field.get(thing);
      }
      else if (cp.getter != null) {
        result = cp.getter.invoke(thing);
      }
      else {
        // no field, no setter - should not happen
        log.error("No field, no method to extract cv {}", key);
      }
    }
    catch (IllegalArgumentException e) {
      log.error("cannot extract {}: {}", key, getExceptionDetails(e));
    }
    catch (IllegalAccessException e) {
      log.error("cannot configure {}: {}", key, getExceptionDetails(e));
    }
    catch (InvocationTargetException e) {
      log.error("cannot configure {}: {}", key, getExceptionDetails(e));
    }
    return result;
  }

  // Dumps configuration if configDump is non-null.
  // Note that the key needs to include the name of a named instance
  private void dumpConfigMaybe (Object thing,
                                Map<String, ConfigurableProperty> analysis,
                                String name)
  {
    if (null != configDump) {
      Class<?> clazz = thing.getClass();
      for (Iterator<?> keys = analysis.keySet().iterator(); keys.hasNext(); ) {
        String key = (String)keys.next();
        if (null == name) {
          name = "";
        }
        // Don't dump props for named instances we've already seen
        String nameKey = name + "." + key;
        Set<String> seen = configuredProps.get(clazz);
        if (null == seen) {
          seen = new HashSet<>();
          configuredProps.put(clazz, seen);
        }
        else {
          if (seen.contains(nameKey)) {
            continue;
          }
        }
        seen.add(nameKey);
        ConfigurableProperty cp = analysis.get(key);

        // metadata first -- we only need it once for each class-item
        String mkey = composeKey(clazz.getName(), key, "");
        ConfigurableValue cv = cp.cv;
        if (cv.dump()) {
          if (!metadataSet.contains(mkey)) {
            configDump.recordMetadata(mkey,
                                      cv.description(),
                                      cv.valueType(),
                                      cv.publish(), cv.bootstrapState());
            metadataSet.add(mkey);
          }

          configDump.recordItem(composeKey(clazz.getName(),
                                           key, name),
                                extractValue(thing, cp));
        }
      }
    }
  }

  // Dumps an instance list
  private void dumpInstanceListMaybe (Class<?> clazz, List<String> names)
  {
    // see #967
    return;
    //if (null != configDump) {
    //  configDump.recordInstanceList(classname2Key(clazz.getName()) + ".instances",
    //                                names);
    //}
  }

  // Composes a config key from class, instance name, and property name
  String composeKey (String classname, String key, String name)
  {
    //String cn = classname.replaceFirst("org.powertac.", "");
    //return decapitalizeClassname(cn) + "." + name + "." + key;
    if (null == name || name.equals("")) {
      return classname2Key(classname) + "." + key;
    }
    else {
      return classname2Key(classname) + "." + name + "." + key;
    }
  }

  private String getExceptionDetails (Exception e)
  {
    StringBuffer sb = new StringBuffer();
    sb.append(e.toString()).append("\n");
    for (int i = 0; i < 5; i++) {
      sb.append("...").append(e.getStackTrace()[i].toString()).append("\n");
    }
    return sb.toString();
  }

  private Object extractConfigValue (Configuration conf, String key,
                                     String type, Object defaultValue)
    throws ClassNotFoundException, NoSuchMethodException,
    IllegalArgumentException, IllegalAccessException, InvocationTargetException
  {
    if (type.equals("List")) {
      // the list type does not always work for some reason,
      // and type-checking is getting really annoying here
      List<String> def = new ArrayList<String>();
      if (null != defaultValue)
        for (Object thing : (List<?>)defaultValue)
          def.add((String)thing);
      return conf.getList(key, def);
    }
    else if (type.equals("XML")) {
      // deserialize the xml to produce a value
      XMLMessageConverter converter = new XMLMessageConverter();
      converter.afterPropertiesSet();
      return converter.fromXML(conf.getString(key));
    }
    else {
      // do it by reflection
    Class<?> clazz = findNamedClass(type);
    String extractorName = "get" + type;
    Method extractor = conf.getClass().getMethod(extractorName, String.class, clazz);
    //log.info("Extract " + conf.getClass().getName() + "." + extractorName
    //         + "(" + clazz.getName() + ")");
    return extractor.invoke(conf, key, defaultValue);
    }
  }

  private Class<?> findNamedClass (String type) throws ClassNotFoundException
  {
    Class<?> clazz;
    if (type.equals("List")) {
      clazz = Class.forName("java.util.List");
    }
    else {
      clazz = Class.forName("java.lang." + type);
    }
    return clazz;
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
      
      // extract configurable fields
      Field[] fields = clazz.getDeclaredFields();
      for (Field field : fields) {
        ConfigurableValue cv = field.getAnnotation(ConfigurableValue.class);
        if (cv != null) {
          // found an annotated field
          log.debug("ConfigurableValue field " + field.getName());
          String propertyName = cv.name();
          if ("".equals(propertyName)) {
            // property name not in annotation, use field name
            propertyName = field.getName();
          }
          result.put(propertyName,
                     new ConfigurableProperty(field, cv));
        }
      }
      
      // then extract configurable methods
      Method[] methods = clazz.getMethods();
      Method getter = null;
      for (Method method : methods) {
        ConfigurableValue cv = method.getAnnotation(ConfigurableValue.class);
        if (cv != null) {
          // This method is annotated
          log.debug("ConfigurableValue method found on " + method.getName());
          String propertyName = cv.name();
          if ("".equals(propertyName)) {
            // property name not in annotation, extract from method name
            propertyName = extractPropertyName(method);
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
              if (getter != null) {
                // check for type compatibility
                Class<?> valueClass = findNamedClass(cv.valueType());
                if (!getter.getReturnType().isPrimitive() &&
                    !valueClass.isAssignableFrom(getter.getReturnType())) {
                  log.warn("Type mismatch, class {}: cannot use default value ({}) for {} ({})", 
                           clazz.getName(), getter.getReturnType().getName(),
                           cv.name(), valueClass.getName());
                  getter = null;
                }
              }
            }
            catch (NoSuchMethodException nsm) {
              log.error("No getter method " + getterName +
                        " for " + clazz.getName());
            }
            catch (ClassNotFoundException e) {
              log.error("Could not find value class: " + e.toString());
            }
          }
          result.put(propertyName,
                     new ConfigurableProperty(method, getter, cv));
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

  private String classname2Key (String classname)
  {
    String cn = classname.replaceFirst("org.powertac.", "");
    int cnpos = cn.lastIndexOf('.');
    return cn.substring(0, cnpos) + "."
        + decapitalize (cn.substring(cnpos + 1));
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
    Field field;
    ConfigurableValue cv;
    
    ConfigurableProperty (Method setter, Method getter, ConfigurableValue cv)
    {
      super();
      this.setter = setter;
      this.getter = getter;
      this.field = null;
      this.cv = cv;
    }
    
    ConfigurableProperty (Field field, ConfigurableValue cv)
    {
      super();
      this.setter = null;
      this.getter = null;
      this.field = field;
      this.cv = cv;
    }
  }
}
