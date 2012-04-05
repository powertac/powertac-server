package org.powertac.visualizer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

/**
 * Modified version of a org.powertac.samplebroker.core.MessageDispatcher class. It is
 * used for registering handlers for specific message types and message routing.
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class MessageDispatcher {

	static private Logger log = Logger.getLogger(MessageDispatcher.class);

	private HashMap<Class<?>, Set<Object>> registrations;
	
	 public MessageDispatcher ()
	  {
	    super();
	    registrations = new HashMap<Class<?>, Set<Object>>();
	  }

	// ------------- incoming messages ----------------
	/**
	 * Sets up handlers for incoming messages by message type.
	 */
	public void registerMessageHandler(Object handler, Class<?> messageType) {
		Set<Object> reg = registrations.get(messageType);
		if (reg == null) {
			reg = new HashSet<Object>();
			registrations.put(messageType, reg);
		}
		reg.add(handler);
	}

	/**
	 * Routes incoming messages from the server
	 */
	public void routeMessage(Object message) {
		Class<?> clazz = message.getClass();
		log.debug("Route " + clazz.getName());
		Set<Object> targets = registrations.get(clazz);
		if (targets == null) {
			log.warn("no targets for message of type " + clazz.getName());
			return;
		}
		for (Object target : targets) {
			log.trace("dipatching to:"+target.getClass().getName());
			dispatch(target, "handleMessage", message);
		}
	}

	static public Object dispatch(Object target, String methodName, Object... args) {
		Logger log = Logger.getLogger(target.getClass().getName());
		Object result = null;
		try {
			Class[] classes = new Class[args.length];
			for (int index = 0; index < args.length; index++) {
				// log.debug("arg class: " + args[index].getClass().getName());
				classes[index] = (args[index].getClass());
			}
			// see if we can find the method directly
			Method method = target.getClass().getMethod(methodName, classes);
			log.debug("found method " + method);
			result = method.invoke(target, args);
		} catch (NoSuchMethodException nsm) {
			log.debug("Could not find exact match: " + nsm.toString());
		} catch (InvocationTargetException ite) {
			Throwable thr = ite.getTargetException();

			if (thr.getStackTrace().length > 3) {
				log.error("Cannot call " + methodName + ": " + thr + "\n  ..at " + thr.getStackTrace()[0] + "\n  ..at "
						+ thr.getStackTrace()[1] + "\n  ..at " + thr.getStackTrace()[2] + "\n  ..at "
						+ thr.getStackTrace()[3] + "\n  ..at ");
			} else {
				log.error("Cannot call " + methodName + ", StackTrace size is:" + thr.getStackTrace().length);
			}
		} catch (Exception ex) {
			log.error("Exception calling message processor: " + ex.toString());
		}
		return result;
	}

}
