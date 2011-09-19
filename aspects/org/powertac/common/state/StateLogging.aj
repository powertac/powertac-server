package org.powertac.common.state;

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
    buf.append(className).append(":");
    buf.append((id == null) ? "null" : id.toString()).append(":");
    buf.append(methodName);
    for (Object arg : args) {
      buf.append(":");
      Long argId = findId(arg);
      if (argId != null)
        buf.append(argId.toString());
      else
        buf.append(arg.toString());
    }
    stateLog.info(buf.toString());
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
