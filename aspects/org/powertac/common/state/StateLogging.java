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
import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

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
public class StateLogging
{
  private Logger stateLog = Logger.getLogger("State");

  // state-change methods
  //public pointcut setState() :
  //  execution (@StateChange * * (..));
  @Pointcut ("execution (@StateChange * * (..))")
  public void setState () {}
  
  //public pointcut newState() :
  //  execution ((@Domain *).new (..));
  @Pointcut ("execution ((@Domain *).new (..))")
  public void newState () {}
  
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
    Object[] args = jp.getArgs();
    Long id = findId(thing);
    writeLog(thing.getClass().getName(), id, "new", args);
  }

  private void writeLog (String className, Long id,
                         String methodName, Object[] args)
  {
    StringBuffer buf = new StringBuffer();
    buf.append(className).append("::");
    buf.append((id == null) ? "null" : id.toString()).append("::");
    buf.append(methodName);
    for (Object arg : args) {
      buf.append("::");
      writeArg(buf, arg);
    }
    stateLog.info(buf.toString());
  }

  private void writeArg (StringBuffer buf, Object arg)
  {
    Long argId = findId(arg);
    if (argId != null)
      buf.append(argId.toString());
    else if (arg == null)
      buf.append("null");
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
}
