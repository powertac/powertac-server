package org.powertac.visualizer;

import org.apache.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffSpecification;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Modified version of a org.powertac.samplebroker.core.MessageDispatcher class.
 * It is used for registering handlers for specific message types and message
 * routing.
 * 
 * @author Jurica Babic, John Collins
 * 
 */
@Service
public class MessageDispatcher
{
  static private Logger log = Logger.getLogger(MessageDispatcher.class);

  @Autowired
  private CustomerRepo customerRepo;
  
  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private TariffRepo tariffRepo;
  
  @Autowired
  private TimeslotRepo timeslotRepo;

  // handler registrations
  private HashMap<Class<?>, Set<Object>> registrations;

  // persistence registrations
  private HashMap<Class<?>, Method> persisters;

  private Boolean tournamentMode = false;
	
  public MessageDispatcher ()
  {
    super();
    registrations = new HashMap<Class<?>, Set<Object>>();
  }

  // ------------- incoming messages ----------------
  /**
   * Sets up handlers for incoming messages by message type.
   */
  public void registerMessageHandler (Object handler, Class<?> messageType)
  {
    Set<Object> reg = registrations.get(messageType);
    if (reg == null) {
      reg = new HashSet<Object>();
      registrations.put(messageType, reg);
    }
    reg.add(handler);
  }

  /**
   * Routes incoming messages from the server, after potentially persisting
   * them.
   */
  public void routeMessage (Object message)
  {
    Class<?> clazz = message.getClass();
    if (tournamentMode && persisters.get(clazz) != null) {
      persist(message);
    }
    
    log.debug("Route " + clazz.getName());
    Set<Object> targets = registrations.get(clazz);
    if (targets == null) {
      log.warn("no targets for message of type " + clazz.getName());
      return;
    }
    for (Object target: targets) {
      log.trace("dispatching to:" + target.getClass().getName());
      dispatch(target, "handleMessage", message);
    }
  }

  static public Object dispatch (Object target, String methodName,
                                 Object... args)
  {
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
    }
    catch (NoSuchMethodException nsm) {
      log.debug("Could not find exact match: " + nsm.toString());
    }
    catch (InvocationTargetException ite) {
      Throwable thr = ite.getTargetException();

      if (thr.getStackTrace().length > 3) {
        log.error("Cannot call " + methodName
                  + "(" + args[0].getClass().getName() + "): " + thr.toString()
                  + "\n  ..at " + thr.getStackTrace()[0]
                  + "\n  ..at " + thr.getStackTrace()[1]
                  + "\n  ..at " + thr.getStackTrace()[2]
                  + "\n  ..at " + thr.getStackTrace()[3]);
      }
      else {
        log.error("Cannot call " + methodName
                  + "(" + args[0].getClass().getName() + "): " + thr.toString());
      }
    }
    catch (Exception ex) {
      log.error("Exception calling message processor: " + ex.toString());
    }
    return result;
  }

  // ------------ Message persistence adapters ---------------
  public void persistMessage (Competition competition)
  {
    // comp needs to be the "current competition"
    Competition.setCurrent(competition);

    // record the customers and brokers
    for (CustomerInfo customer: competition.getCustomers()) {
      customerRepo.add(customer);
    }
    for (String username: competition.getBrokers()) {
      brokerRepo.findOrCreateByUsername(username);
    }
  }
  
  public void persistMessage (TariffSpecification spec)
  {
    log.info("persisting spec " + spec.getId());
    tariffRepo.addSpecification(spec);
  }
  
  public void persistMessage (TimeslotUpdate tu)
  {
    // update timeslotRepo
    timeslotRepo.findOrCreateBySerialNumber(tu.getLastEnabled());
  }
  
  // persistence dispatcher
  private void persist (Object message)
  {
    Method method = persisters.get(message.getClass());
    try {
      method.invoke(this, message);
    }
    catch (IllegalArgumentException e) {
      e.printStackTrace();
    }
    catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  // index all the persistMessage methods in this class
  public void initialize ()
  {
    registrations = new HashMap<Class<?>, Set<Object>>();
    persisters = new HashMap<Class<?>, Method>();
    Method[] methods = this.getClass().getDeclaredMethods();
    for (Method method: methods) {
      if (method.getName().equals("persistMessage")) {
        Class<?>[] params = method.getParameterTypes();
        persisters.put(params[0], method);
      }
    }
  }

  public Boolean getTournamentMode() {
    return tournamentMode;
  }
  public void setTournamentMode(Boolean tournamentMode) {
    this.tournamentMode = tournamentMode;
  }
}
