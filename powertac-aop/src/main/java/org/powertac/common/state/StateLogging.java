/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.common.state;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Implement uniform state-logging using aspects. This scheme depends on two
 * annotations: @Domain labels a class for which calls to the constructor are
 * logged. @StateChange labels a method that must be logged (with its arguments)
 * when it is called. Log output is a single text line consisting of the
 * following fields, separated by double colon :: strings 
 * (assuming the log4j config puts out the msec data):
 * <ol>
 *  <li>milliseconds from start of log</li>
 *  <li>class name</li>
 *  <li>instance id value</li>
 *  <li>method name ("new" for constructor)</li>
 *  <li>method arguments, separated by ::</li>
 * </ol>
 * @author John Collins
 */
@Aspect
@Component
public class StateLogging
{
  static private Logger log = LogManager.getLogger(StateLogging.class);
  private Logger stateLog = LogManager.getLogger("State");

  // package-prefix abbreviation map
  static private LinkedHashMap<String, String> abbreviations = null;
  static boolean classnameAbbreviation = false;

  // state-change methods
  @Pointcut ("execution (@StateChange * * (..))")
  public void setState () {}

  @Pointcut ("execution ((@Domain *).new (..))")
  public void domainConstructor() {}

  @Pointcut ("execution (Object XStreamStateLoggable.readResolve())")
  public void readResolveMethod() {}

  @Pointcut ("execution (@ChainedConstructor *.new (..))")
  public void chainedConstructor() {}

  @Pointcut ("(domainConstructor() && !chainedConstructor()) || readResolveMethod()")
  public void newState() {}

  @AfterReturning ("setState()")
  public void setstate (JoinPoint jp)
  {
    Object thing = jp.getTarget();
    Object[] args = jp.getArgs();
    Signature sig = jp.getSignature();
    Long id = findId(thing);
    writeLog(thing.getClass().getName(), id, sig.getName(), args);
  }

  @AfterReturning ("newState()")
  public void newstate (JoinPoint jp)
  {
    Object thing = jp.getTarget();
    Class<?> clazz = thing.getClass();
    Object[] args = jp.getArgs();
    Signature sig = jp.getSignature();
    Long id = findId(thing);
    if ("readResolve".equals(sig.getName())) {
      args = collectProperties(thing);
      writeLog(clazz.getName(), id, "-rr", args);
    }
    else if (clazz.isAnnotationPresent(Domain.class)) {
      // Runtime check annotation to prevent logging subclasses of @Domain
      writeLog(clazz.getName(), id, "new", args);
    }
  }

  private Object[] collectProperties(Object thing) {
    ArrayList<Object> properties = new ArrayList<Object>();
    try {
      //TODO: 
      // - use XStream annotation to figure out fields to log instead
      // - cache fields list to reduce lookup
      Domain domain = thing.getClass().getAnnotation(Domain.class);
      if (domain instanceof Domain) {
        String[] fields = domain.fields();
        for (String field : fields) { 
          Object obj = PropertyUtils.getSimpleProperty(thing, field);
          properties.add(obj);
        }
      }
    }
    catch (IllegalAccessException e) {
      log.error("Failed to introspect " + thing.getClass().getSimpleName(), e);
    }
    catch (InvocationTargetException e) {
      log.error("Failed to introspect " + thing.getClass().getSimpleName(), e);
    }
    catch (NoSuchMethodException e) {
      log.error("Failed to introspect " + thing.getClass().getSimpleName(), e);
    }
    return properties.toArray();
  }

  // writes out a single log entry
  private void writeLog (String className, Long id,
                         String methodName, Object[] args)
  {
    StringBuffer buf = new StringBuffer();
    buf.append(abbreviate(className)).append("::");
    buf.append((id == null) ? "null" : id.toString()).append("::");
    buf.append(methodName);
    for (Object arg : args) {
      buf.append("::");
      writeArg(buf, arg);
    }
    stateLog.info(buf.toString());
  }

  @SuppressWarnings("rawtypes")
  private void writeArg (StringBuffer buf, Object arg)
  {
    Long argId = findId(arg);
    if (argId != null)
      buf.append(argId.toString());
    else if (arg == null)
      buf.append("null");
    else if (arg instanceof Collection collection) {
      buf.append("(");
      String delimiter = "";
      for (Object item : collection) {
        buf.append(delimiter);
        writeArg(buf, item);
        delimiter = ",";
      }
      buf.append(")");
    }
    else if (arg.getClass().isArray()) {
      buf.append("[");
      int length = Array.getLength(arg);
      for (int index = 0; index < length; index++) {
        writeArg(buf, Array.get(arg, index));
        if (index < length - 1) {
          buf.append(",");
        }
      }
      buf.append("]");
    }
    else {
      buf.append(arg.toString());
    }
  }

  Long findId (Object thing)
  {
    Long id = null;
    try {
      Method getId = thing.getClass().getMethod("getId");
      id = (Long)getId.invoke(thing);
    }
    catch (Exception ex) {
    }
    return id;
  }

  // --------- static methods to handle classname abbreviation

  /**
   * Sets up the classname abbreviation feature. If the parameter is true,
   * then classnames will be abbreviated. This should obviously be called
   * before any logging happens.
   */
  public static void setClassnameAbbreviation (boolean abbreviation)
  {
    classnameAbbreviation = abbreviation;
  }

  // abbreviates a classname 
  private static String abbreviate (String classname)
  {
    String result = classname;
    if (!classnameAbbreviation)
      return classname;
    else {
      ensureAbbreviations();
      for (Map.Entry<String, String> abbr : abbreviations.entrySet()) {
        if (classname.startsWith(abbr.getKey())) {
          result = classname.substring(abbr.getKey().length());
          result = abbr.getValue() + result;
          break;
        }
      }
      return result;
    }
  }

  private static void ensureAbbreviations ()
  {
    if (null == abbreviations) {
      abbreviations = new LinkedHashMap<>();
      abbreviations.put("org.powertac.common.msg.", "cm.");
      abbreviations.put("org.powertac.common.", "c.");
      abbreviations.put("org.powertac.", "");
    }
  }

  /**
   * Given a possibly abbreviated classname, returns
   * the unabbreviated version.
   */
  public static String unabbreviate (String origClassname)
  {
    ensureAbbreviations();
    String result = origClassname;
    if (origClassname.startsWith("org.powertac."))
      return result;
    try {
      Class clazz = Class.forName(origClassname);
    } catch (ClassNotFoundException cnf) {
      // If the class doesn't exist, assume the name is abbreviated
      for (Map.Entry<String, String> abbr : abbreviations.entrySet()) {
        if (abbr.getValue().length() > 0 &&
                origClassname.startsWith(abbr.getValue())) {
          // non-empty abbreviation, replace with expansion
          result = origClassname.substring(abbr.getValue().length());
          result = abbr.getKey() + result;
          break;
        }
        else if (abbr.getValue().length() == 0) {
          // last is org.powertac type, no abbreviation
          result = abbr.getKey() + result;
        }
      }
    }
    return result;
  }
}
