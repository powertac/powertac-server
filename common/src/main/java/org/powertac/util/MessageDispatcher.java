/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Static resource for dispatching messages. Helps avoid visitor or 
 * double-dispatch intrusions into domain types.
 * @author John Collins
 */
public class MessageDispatcher
{
  /**
   * Dispatches a call to methodName inside target based on the type of message.
   * Allows polymorphic method dispatch without the use of visitor or double
   * dispatch schemes, which produce nasty couplings with domain types.
   * <p>
   * Note that this scheme finds only exact matches between types of arguments
   * and declared types of formal parameters for declared or inherited methods.
   * So it will not call a method with formal parameter types of 
   * (Transaction, List) if the actual arguments are (Transaction, ArrayList).
   * </p>
   */
  static public Object dispatch (Object target, 
                                 String methodName, 
                                 Object... args)
  {
    Logger log = LogManager.getLogger(target.getClass().getName());
    Object result = null;
    try {
      Class<?>[] classes = new Class[args.length];
      for (int index = 0; index < args.length; index++) {
        //log.debug("arg class: " + args[index].getClass().getName());
        classes[index] = (args[index].getClass());
      }
      // see if we can find the method directly
      Method method = target.getClass().getMethod(methodName, classes);
      log.debug("found method " + method);
      if (!method.canAccess(target))
      {
        log.debug("Making {} accessible", methodName);
        if (!method.trySetAccessible())
        {
          log.error("Unable to make method {} accessible", methodName);
        }
      }
      result = method.invoke(target, args);
    }
    catch (NoSuchMethodException nsm) {
      log.debug("Could not find exact match: " + nsm.toString());
    }
    catch (InvocationTargetException ite) {
      Throwable thr = ite.getTargetException();
      log.error("Cannot call " + methodName
                   + ": " + thr + "\n  ..at "
                   + thr.getStackTrace()[0] + "\n  ..at "
                   + thr.getStackTrace()[1] + "\n  ..at "
                   + thr.getStackTrace()[2] + "\n  ..at "
                   + thr.getStackTrace()[3] + "\n  ..at ..."
                   );      
    }
    catch (Exception ex) {
      log.error("Exception calling " + methodName + " " + ex.toString() + "\n  ..at "
                + ex.getStackTrace()[0] + "\n  ..at "
                + ex.getStackTrace()[1] + "\n  ..at "
                + ex.getStackTrace()[2] + "\n  ..at "
                + ex.getStackTrace()[3] + "\n  ..at ...");
    }
    return result;
  }
}
