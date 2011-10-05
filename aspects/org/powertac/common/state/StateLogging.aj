package org.powertac.common.state;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;
//import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;

public aspect StateLogging
{
  private Logger stateLog = Logger.getLogger("State");

  // state-change methods
  public pointcut setState() :
    execution (@StateChange * * (..));
  
  public pointcut newState() :
    execution ((@Domain *).new (..));
  
  after() returning : setState()
  {
    Object thing = thisJoinPoint.getTarget();
    Object[] args = thisJoinPoint.getArgs();
    Signature sig = thisJoinPoint.getSignature();
    Long id = findId(thing);
    writeLog(thing.getClass().getName(), id, sig.getName(), args);
  }
  
  after() returning : newState()
  {
    Object thing = thisJoinPoint.getTarget();
    Object[] args = thisJoinPoint.getArgs();
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
