/*
 * Copyright (c) 2012-2021 by John Collins
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
package org.powertac.logtool.common;

import static org.powertac.util.MessageDispatcher.dispatch;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.RandomSeed;
import org.powertac.common.TimeService;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.msg.BalanceReport;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.state.StateLogging;
import org.powertac.common.xml.PowerTypeConverter;
import org.powertac.du.DefaultBroker;
import org.powertac.logtool.LogtoolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Processor for state log entries; creates domain object instances,
 * stores them in repositories as well as in a master repo indexed by
 * id value.
 * 
 * @author John Collins
 */
@Service
public class DomainObjectReader
{
  static private Logger log = LogManager.getLogger(DomainObjectReader.class.getName());
  
  @Autowired
  private TimeService timeService;
  
  HashMap<String, String[]> schema;
  HashMap<Long, Object> idMap;
  HashMap<Class<?>, Class<?>> ifImplementors;
  HashMap<String, Class<?>> substitutes;
  HashSet<String> ignores;
  HashSet<String> includesOnly;
  HashSet<Class<?>> noIdTypes;
  //HashSet<Class<?>> argModTypes;
  PowerTypeConverter ptConverter = new PowerTypeConverter();

  // listeners can be the old-style NewObjectListeners, or they can be
  // LogtoolContext instances with handleMessage() methods
  HashMap<Class<?>, ArrayList<NewObjectListener>> newObjectListeners;
  HashMap<Class<?>, ArrayList<LogtoolContext>> messageListeners;

  //per-timeslot pause in msec"
  private int timeslotPause = 0;

  // If false, then don't instantiate objects in the current environment
  private boolean instantiate = true;

  /**
   * Default constructor
   */
  public DomainObjectReader ()
  {
    super();
    reset();
  }

  /**
   * Restores the Reader to initial conditions
   */
  public void reset ()
  {
    idMap = new HashMap<Long, Object>();

    // Set up the interface defaults
    ifImplementors = new HashMap<>();
    ifImplementors.put(List.class, ArrayList.class);
    
    // set up substitute list to handle inner classes in a reasonable way
    substitutes = new HashMap<>();
    substitutes.put("org.powertac.du.DefaultBrokerService$LocalBroker",
                    DefaultBroker.class);

    // set up the ignore list
    ignores = new HashSet<>();
    ignores.add("org.powertac.common.Tariff");
    //ignores.add("org.powertac.common.TariffSubscription");
    //ignores.add("org.powertac.genco.Genco");
    ignores.add("org.powertac.common.Rate$ProbeCharge");
    ignores.add("org.powertac.common.msg.SimPause");
    ignores.add("org.powertac.common.msg.SimResume");
    ignores.add("org.powertac.common.msg.PauseRequest");
    ignores.add("org.powertac.common.msg.PauseRelease");
    ignores.add("org.powertac.common.RandomSeed");
    //ignores.add("org.powertac.common.WeatherReport");
    //ignores.add("org.powertac.common.WeatherForecast");
    //ignores.add("org.powertac.common.WeatherForecastPrediction");
    ignores.add("org.powertac.factoredcustomer.DefaultUtilityOptimizer$DummyTariffSubscription");

    // set up the no-id list
    noIdTypes = new HashSet<>();
    noIdTypes.add(TimeService.class);
    noIdTypes.add(BalanceReport.class);
    noIdTypes.add(SimStart.class);
    noIdTypes.add(SimEnd.class);

    // set up the list of types that might need to modify their args
    //argModTypes.add(TariffSpecification.class);

    // set up listener list
    newObjectListeners = new HashMap<Class<?>, ArrayList<NewObjectListener>>();
    messageListeners = new HashMap<Class<?>, ArrayList<LogtoolContext>>();
  }

  /**
   * Sets the per-timeslot pause value in msec
   */
  public void setTimeslotPause (int msec)
  {
    timeslotPause = msec;
  }

  public int getTimeslotPause ()
  {
    return timeslotPause;
  }
  
  /**
   * Adds classname to list of classes to be included by this reader, after first ensuring
   * that it's not in the ignores list
   */
  public void addIncludesOnly (String classname)
  {
    if (null == includesOnly) {
      includesOnly = new HashSet<>();
    }
    if (ignores.contains(classname))
      ignores.remove(classname);
    includesOnly.add(classname);
  }

  /**
   * Sets the instantiation flag. If true, then objects read from the log are instantiated in the
   * current running environment. This is the normal case for re-running games.
   */
  public void setInstantiate (boolean flag)
  {
    instantiate = flag;
  }

  public boolean getInstantiate ()
  {
    return instantiate;
  }

  /**
   * Registers a NewObjectListener. The listener will be called with
   * each newly-created object of the given type. If type is null, then
   * the listener will be called for each new object. 
   */
  public void registerNewObjectListener (NewObjectListener listener,
                                         Class<?> type)
  {
    ArrayList<NewObjectListener> list = newObjectListeners.get(type);
    if (null == list) {
      list = new ArrayList<NewObjectListener>();
      newObjectListeners.put(type, list);
    }
    list.add(listener);
  }

  /**
   * Registers the given LogtoolContext as a messageListener. Incoming messages
   * must be dispatched using util.MessageDispatcher
   */
  public void registerMessageListener (LogtoolContext listener, Class<?> type)
  {
    ArrayList<LogtoolContext> list = messageListeners.get(type);
    if (null == list) {
      list = new ArrayList<LogtoolContext>();
      messageListeners.put(type, list);
    }
    list.add(listener);
  }
  
  /**
   * Sets the schema for this log.
   */
  public void setSchema (HashMap<String, String[]> schema)
  {
    this.schema = schema;
  }

  /**
   * Converts a line from the log to an object.
   * Each line is of the form<br>
   * &nbsp;&nbsp;<code>ms:class::id::method{::arg}*</code>
   * 
   * Note that some objects cannot be resolved in the order they appear
   * in a logfile, because they have forward dependencies. This means
   * that a failure to resolve an object does not necessarily mean it's bogus,
   * but could mean that it could be resolved at a later time, typically
   * within one or a very few input lines. 
   * @throws MissingDomainObject 
   */
  public Object readObject (String line)
  throws MissingDomainObject
  {
    log.debug("readObject(" + line + ")");
    String body = line.substring(line.indexOf(':') + 1);
    String[] tokens = body.split("::");
    Class<?> clazz;
    String classname = StateLogging.unabbreviate(tokens[0]);
    if (ignores.contains(classname)) {
      log.debug("ignoring " + classname);
      return null;
    }
    else if (null != includesOnly && ! includesOnly.contains(classname)) {
      log.debug("not including " + classname);
      return null;
    }
    try {
      clazz = Class.forName(classname);
    }
    catch (ClassNotFoundException e) {
      Class<?> subst = substitutes.get(classname);
      if (null == subst) {
        log.warn("class " + classname + " not found");
        return null;
      }
      else {
        clazz = subst;
        //log.info("substituting " + clazz.getName() + " for " + classname);
      }
    }

    long id = -1;
    try {
      id = Long.parseLong(tokens[1]);
    }
    catch (NumberFormatException nfe) {
      if (clazz == TimeService.class) {
        // normal case - timeService does not have an id
        updateTime(tokens[3]);
        return null;
      }
      else if (noIdTypes.contains(clazz)) {
        id = 0;
      }
      else {
        log.debug("Number format exception reading id");
        return null;
      }
    }
    String methodName = tokens[2];
    log.debug("methodName=" + methodName);
    //hack to fix Issue #1106
    if (methodName.equals("new")
            || (clazz == RandomSeed.class && methodName.equals("init"))) {
      // maybe pause before handling TimeslotUpdate msg
      if (instantiate && clazz == TimeslotUpdate.class && timeslotPause > 0) {
        try {
          Thread.sleep(timeslotPause);
        }
        catch (InterruptedException e) {
          // ignore
        }
      }
      // constructor
      Object newInst =
              constructInstance(clazz, Arrays.copyOfRange(tokens, 3,
                                                          tokens.length));
      if (null != newInst) {
        if (!noIdTypes.contains(clazz)) {
          setId(newInst, id);
          idMap.put(id, newInst);
        }
        log.debug("Created new instance " + id + " of class " + tokens[0]);
        fireNewObjectEvent(newInst);
      }
      return newInst;
    }
    else if (methodName.equals("-rr")) {
      // readResolve
      Object newInst =
              restoreInstance(clazz, Arrays.copyOfRange(tokens, 3,
                                                        tokens.length));
      if (null != newInst) {
        setId(newInst, id);
        idMap.put(id, newInst);
        log.debug("Restored instance " + id + " of class " + tokens[0]);
        fireNewObjectEvent(newInst);
      }
      return newInst;      
    }
    else {
      // don't call methods if we are not instantiating objects in the current environment
      // other method calls -- object should already exist
      Object inst = idMap.get(id);
      if (null == inst) {
        log.warn("Cannot find instance for id " + id
                 + " of type " + clazz.getCanonicalName());
        return null;
      }
      Method[] methods = clazz.getMethods();
      ArrayList<Method> candidates = new ArrayList<>();
      for (Method method : methods) {
        if (method.getName().equals(methodName)) {
          candidates.add(method);
        }
      }
      // We now have a list of candidate methods.
      if (0 == candidates.size()) {
        log.error("Cannot find method " + methodName
                  + " for class " + clazz.getName());
        return null;
      }
      if (1 == candidates.size()) {
        // there's one candidate, probably it is the correct one
        if (!tryMethodCall(inst, candidates.get(0),
                           Arrays.copyOfRange(tokens, 3, tokens.length))) {
          log.error("Failed to invoke method " + methodName
                    + " on instance of " + clazz.getName());
        }
      }
      else {
        // multiple candidates -- try them until we get success
        boolean success = false;
        for (Method candidate : candidates) {
          success = tryMethodCall(inst, candidate,
                                  Arrays.copyOfRange(tokens, 3, tokens.length));
          if (success)
            break;
        }
        if (!success) {
          log.error("Failed to find viable candidate for " + methodName
                    + " on instance of " + clazz.getName());
        }
      }
    }
    return null;
  }
  
  public Object getById (long id)
  {
    return idMap.get(id);
  }
  
  private void updateTime (String time)
  {
    if (!instantiate)
      return;
    Instant value = Instant.parse(time);
    timeService.setCurrentTime(value);
    log.debug("time set to " + time);
  }
  
  private void fireNewObjectEvent (Object thing)
  {
    dispatchNewObjectListeners(thing);
    dispatchMessageListeners(thing);
  }

  private void dispatchNewObjectListeners(Object thing)
  {
    ArrayList<NewObjectListener> listeners =
            newObjectListeners.get(thing.getClass());
    if (null == listeners)
      // try one up the tree to catch local subclasses like the default broker
      listeners = newObjectListeners.get(thing.getClass().getSuperclass());
    if (null != listeners) {
      for (NewObjectListener li : listeners) {
        li.handleNewObject(thing);
      }
    }
    // check for promiscuous listener
    listeners = newObjectListeners.get(null);
    if (null != listeners) {
      for (NewObjectListener li : listeners) {
        li.handleNewObject(thing);
      }
    }
  }

  private void dispatchMessageListeners(Object thing)
  {
    ArrayList<LogtoolContext> listeners =
        messageListeners.get(thing.getClass());
    if (null != listeners) {
      for (Object target: listeners) {
        dispatch(target, "handleMessage", thing);
      }
    }
  }

  private Object constructInstance (Class<?> clazz, String[] args)
          throws MissingDomainObject
  {
    //if (clazz.getName().equals("org.powertac.common.Order"))
    //  System.out.println("Order");
    //else if (clazz.getName().equals("org.powertac.common.TariffSpecification"))
    //  System.out.println("TariffSpecification");
    Constructor<?>[] potentials = clazz.getDeclaredConstructors();
    Constructor<?> target = null;
    Object[] params = null;
    for (Constructor<?> cons : potentials) {
      Type[] types = cons.getGenericParameterTypes();
      if (types.length != args.length)
        // not this one
        continue;
      // correct length of parameter list -
      // now try to resolve the types.
      // If we get a MissingDomainObject exception, keep going.
      
      try {
        params = resolveArgs(types, args);
      }
      catch (MissingDomainObject mdo) {
        // ignore
      }
      if (null == params)
        // no match
        continue;
      else {
        target = cons;
        break;
      }
    }
    // if we found one, use it, then update the id value
    if (null != target) {
      Object result = null;
      try {
        target.setAccessible(true);
        result = target.newInstance(params);
      }
      catch (InvocationTargetException ite) {
        // arg-constructor mismatch
        return restoreInstance(clazz, args);
      }
      catch (Exception e) {
        log.error("could not construct instance of " + clazz.getName()
                  + ": " + e.toString());
        return null;
      }
      return result;
    }
    else {
      // otherwise, try to use the readResolve method
      return restoreInstance(clazz, args);
    }
  }

  // restores an instance from a readResolve record.
  // Fields are given in the @Domain annotation.
  private Object restoreInstance (Class<?> clazz, String[] args)
          throws MissingDomainObject
  {
    // 1056 - modify args if needed
    //Class<?>[] params = {String[].class};
    try {
      Method mod = clazz.getDeclaredMethod("modifyLogArgs", String[].class);
      mod.invoke(null, (Object) args);
    } catch (NoSuchMethodException nsm) {
      // class lacks the method, nothing to do here
    } catch (Exception ex) {
      log.error("Exception {} modifying log args ({}) for {}",
                ex.toString(), args, clazz.getCanonicalName());
    }

    String[] fieldNames = schema.get(clazz.getName());
    if (null != fieldNames) {
      // only do this for @Domain classes that are in the recorded schema
      Object thing = null;
      try {
        Constructor<?> cons = clazz.getDeclaredConstructor();
        cons.setAccessible(true);
        thing = cons.newInstance();
      }
      catch (Exception e) {
        log.warn("No default constructor for " + clazz.getName()
                  + ": " + e.toString());
        return null;
      }
      // #1016 -- String[] fieldNames = domain.fields();
      Field[] fields = new Field[fieldNames.length];
      Class<?>[] types = new Class<?>[fieldNames.length];
      for (int i = 0; i < fieldNames.length; i++) {
        fields[i] = ReflectionUtils.findField(clazz,
                                              resolveDoubleCaps(fieldNames[i]));
        if (null == fields[i]) {
          log.warn("No field in " + clazz.getName()
                   + " named " + fieldNames[i]);
          types[i] = null;
        }
        else {
          types[i] = fields[i].getType();
        }
      }
      if (types.length != args.length)
        log.error("RR arg mismatch class {}, {} fields, args {}",
                  clazz.getName(), fields.length, args);
      Object[] data = resolveArgs(types, args);
      if (null == data) {
        log.error("Could not resolve args for " + clazz.getName());
        return null;
      }
      else {
        for (int i = 0; i < fields.length; i++) {
          if (null == fields[i])
            continue;
          fields[i].setAccessible(true);
          try {
            fields[i].set(thing, data[i]);
          }
          catch (Exception e) {
            log.error("Exception setting field: " + e.toString());
            return null;
          }
        }
      }
      return thing;
    }
    return null;
  }
  
  private String resolveDoubleCaps (String name)
  {
    // lowercase first char of field name with two initial caps
    if (Character.isUpperCase(name.charAt(0)) &&
            Character.isUpperCase(name.charAt(1))) {
      char[] chars = name.toCharArray();
      chars[0] = Character.toLowerCase(chars[0]);
      return (String.valueOf(chars));
    }
    return name;
  }

  // attempts to call a method by reconstructing its args and invoking it
  private boolean tryMethodCall (Object thing, Method method, String[] args)
  {
    Type[] argTypes = method.getGenericParameterTypes();
    if (argTypes.length != args.length)
      // bail if arglist lengths do not match
      return false;
    Object[] realArgs;
    if (0 == argTypes.length) {
      // no args
      realArgs = null;
    }
    else {
      try {
        realArgs = resolveArgs(argTypes, args);
        if (null == realArgs || realArgs.length != args.length) {
          log.debug("Could not resolve args: method " + method.getName()
                    + ", class = " + thing.getClass().getName()
                    + ", args = " + args);
          return false;
        }
      }
      catch (MissingDomainObject mdo) {
        return false;
      }
    }
    try {
      method.invoke(thing, realArgs);
      return true;
    }
    catch (Exception e) {
      StringBuilder argsString = new StringBuilder();
      for (Object arg : realArgs) {
        argsString.append("(" + arg.getClass().getName() + ") " + arg.toString() + ", ");
      }
      log.error(e.getClass().getName() + " calling method " + thing.getClass().getName()
                + "." + method.getName()
                + " on args " + argsString.toString());
    }
    return false;
  }

  // attempts to match a set of types with a set of String arguments
  // from the logfile. They match if the strings can be resolved to
  // the corresponding types. 
  private Object[] resolveArgs (Type[] types, String[] args)
          throws MissingDomainObject
  {
    // for each type, we attempt to resolve the corresponding arg
    // as an instance of that type.
    Object[] result = new Object[types.length];
    for (int i = 0; i < args.length; i++) {
      result[i] = resolveArg(types[i], args[i]);
    }
    return result;
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Object resolveArg (Type type, String arg)
  throws MissingDomainObject
  {
    // type can be null in a few cases - nothing to be done about it?
    if (null == type) {
      return null;
    }

    // check for non-parameterized types
    if (type instanceof Class) {
      Class<?> clazz = (Class<?>)type;
      if (clazz.isEnum()) {
        return Enum.valueOf((Class<Enum>)type, arg);
      }
      else if (PowerType.class == clazz) {
        //System.out.println("Class: " + clazz.getCanonicalName());
        return ptConverter.fromString(arg);
      }
      else {
        return resolveSimpleArg(clazz, arg);
      }
    }

    // check for collection, denoted by leading (
    else if (type instanceof ParameterizedType) {
      ParameterizedType ptype = (ParameterizedType)type;
      Class<?> clazz = (Class<?>)ptype.getRawType();
      boolean isCollection = false;
      if (clazz.equals(Collection.class))
        isCollection = true;
      else {
        Class<?>[] ifs = clazz.getInterfaces();
        for (Class<?> ifc : ifs) {
          if (ifc.equals(Collection.class)) {
            isCollection = true;
            break;
          }
        }
      }
      if (isCollection) {
        // expect arg to start with "("
        log.debug("processing collection " + clazz.getName());
        if (arg.charAt(0) != '(') {
          log.error("Collection arg " + arg + " does not start with paren");
          return null;
        }
        // extract element type and resolve recursively
        Type[] tas = ptype.getActualTypeArguments();
        if (1 == tas.length) {
          Class<?> argClazz = (Class<?>) tas[0];
          // create an instance of the collection
          Collection<Object> coll;
          // resolve interfaces into actual classes
          if (clazz.isInterface())
            clazz = ifImplementors.get(clazz);
          try {
            coll = (Collection<Object>)clazz.newInstance();
          }
          catch (Exception e) {
            log.error("Exception creating collection: " + e.toString());
            return null;
          }
          // at this point, we can split the string and resolve recursively
          String body = arg.substring(1, arg.indexOf(')'));
          String[] items = body.split(",");
          for (String item : items) {
            coll.add(resolveSimpleArg(argClazz, item));
          }
          return coll;
        }
      }
    }

    // if we get here, no resolution
    log.error("unresolved arg: type = " + type
              + ", arg = " + arg);
    return null;
  }

  private Object resolveSimpleArg (Class<?> clazz, String arg)
  throws MissingDomainObject
  {
    // handle the simplest case first
    if (arg.equals("null"))
      return null;
    
    if (clazz.getName().startsWith("org.powertac")) {
      Method getId;
      try {
        getId = clazz.getMethod("getId");
        if (getId.getReturnType() == long.class) {
          // this is a domain type; it may or may not be in the map
          Long key = Long.parseLong(arg);
          Object value = idMap.get(key);
          if (null != value && clazz.isAssignableFrom(value.getClass())) {
            return value;
          }
          else {
            // it's a domain object, but we cannot resolve it
            // -- this can be an error, or a symptom of using the wrong
            //    constructor or method.
            throw new MissingDomainObject("missing object id=" + key);
          }
        }
      }
      catch (SecurityException e) {
        log.error("Exception on getId(): " + e.toString());
        return null;
      }
      catch (NoSuchMethodException e) {
        // normal result of no getId() method
      }
      catch (NumberFormatException e) {
        // normal result of non-integer id value
      }
    }
    
    // arg is not an id value - check if it's supposed to be a primitive
    if (clazz.getName().equals("boolean")) {
      boolean value = Boolean.parseBoolean(arg);
      if (value) {
        return true; // resolved as boolean
      }
      else if (arg.equalsIgnoreCase("false")) {
        return false; // resolved as boolean
      }
      else
        return null; // does not resolve
    }
    
    if (clazz.getName().equals("long")) {
      try {
        long value = Long.parseLong(arg);
        return value;
      }
      catch (NumberFormatException nfe) {
        // not a long
        return null;
      }
    }
    
    if (clazz.getName().equals("int")) {
      try {
        int value = Integer.parseInt(arg);
        return value;
      }
      catch (NumberFormatException nfe) {
        // not an int
        return null;
      }        
    }
    
    if (clazz.getName().equals("double") || clazz == Double.class) {
      try {
        double value = Double.parseDouble(arg);
        return value;
      }
      catch (NumberFormatException nfe) {
        // not a double
        return null;
      }        
    }
    
    if (clazz.getName() == "java.lang.Double") {
      
    }
    
    // check for time value
    if (clazz.getName() == "org.joda.time.Instant") {
      try {
        Instant value = Instant.parse(arg);
        return value;
       }
      catch (IllegalArgumentException iae) {
        // make Instant from Long
        try {
          Long msec = Long.parseLong(arg);
          return new Instant(msec);
        }
        catch (Exception e) {
          // Long parse failure
          log.error("could not parse Long " + arg);
          return null;
        }
      }
      catch (Exception e) {
        // Instant parse failure
        log.error("could not parse Instant " + arg);
        return null;
      }
    }
    
    // check for type with String constructor
    try {
      Constructor<?> cons = clazz.getConstructor(String.class);
      return cons.newInstance(arg);
    }
    catch (NoSuchMethodException e) {
      // normal result of failure - fall through and try something else
    }
    catch (Exception e) {
      log.error("Exception looking up constructor for "
                + clazz.getName() + ": " + e.toString());
      return null;
    }
    // no type matched
    return null;
  }
  
  // Sets the id field of a newly-constructed thing
  private void setId (Object thing, Long id)
  {
    Class<?> clazz = thing.getClass();
    Method setId;
    try {
      setId = clazz.getMethod("setId", long.class);
      setId.setAccessible(true);
      setId.invoke(thing, (long)id);
    }
    catch (SecurityException e) {
      log.error("Exception on setId(): " + e.toString());
    }
    catch (NoSuchMethodException e) {
      // normal result of no setId() method
      ReflectionTestUtils.setField(thing, "id", id);
    }
    catch (Exception e) {
      log.error("Error setting id value " + e.toString());
    }
  }

  class WrongArgType extends Exception {

    private static final long serialVersionUID = 7044658729956229376L;
  }
}
