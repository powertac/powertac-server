package org.powertac.visualizer.service_ptac;

import org.powertac.common.interfaces.VisualizerMessageListener;
import org.powertac.logtool.common.NewObjectListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import jakarta.annotation.PostConstruct;

/**
 * ADAPTED FROM broker core
 * <p>
 * Routes incoming messages to broker components, and outgoing messages to the
 * server. Components must register for specific message types with the broker,
 * which passes the registrations to this router. For this to work, registered
 * components must implement a handleMessage(msg) method that takes the
 * specified type as its single argument.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class MessageDispatcher
implements VisualizerMessageListener, NewObjectListener {

    static private Logger log = LoggerFactory.getLogger(MessageDispatcher.class);

    @Autowired
    private ApplicationContext context;

    private HashMap<Class<?>, Set<Object>> registrations;

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        registrations = new HashMap<>();
        registerAllHandlers();
    }

    // ------------- incoming messages ----------------

    @Override
    public void receiveMessage(Object message) {
        Class<?> clazz = message.getClass();

        Set<Object> targets = registrations.get(clazz);
        if (targets == null) {
            log.trace("no targets for message of type " + clazz.getName());
            return;
        }
        for (Object target : targets) {
            dispatch(target, "handleMessage", message);
        }
    }

    @Override
    public void handleNewObject(Object obj) {
        receiveMessage(obj);
    }

    /**
     * Dispatches a call to methodName inside target based on the type of
     * message. Allows polymorphic method dispatch without the use of visitor or
     * double dispatch schemes, which produce nasty couplings with domain types.
     * <p>
     * Note that this scheme finds only exact matches between types of arguments
     * and declared types of formal parameters for declared or inherited
     * methods. So it will not call a method with formal parameter types of
     * (Transaction, List) if the actual arguments are (Transaction, ArrayList).
     * </p>
     */
    static public Object dispatch(Object target, String methodName, Object... args) {
        Logger log = LoggerFactory.getLogger(target.getClass().getName());
        Object result = null;
        try {
            Class<?>[] classes = new Class[args.length];
            for (int index = 0; index < args.length; index++) {
                classes[index] = (args[index].getClass());
            }
            // see if we can find the method directly
            Method method = target.getClass().getMethod(methodName, classes);
            log.trace("found method " + method);
            result = method.invoke(target, args);
        } catch (NoSuchMethodException nsm) {
            log.debug("Could not find exact match: " + nsm.toString());
        } catch (InvocationTargetException ite) {
            Throwable thr = ite.getTargetException();
            log.error("Cannot call " + methodName + ": " + thr + "\n", ite);
        } catch (Exception ex) {
            log.error("Exception calling message processor: " + ex.toString(), ex);
        }
        return result;
    }

    // ------------- registering handlers ----------------

    public void registerAllHandlers() {
        Collection<MessageHandler> handlers = context.getBeansOfType(MessageHandler.class).values();

        log.info("Ready to initialize " + handlers.size() + " objects");
        for (MessageHandler handler : handlers) {
            handler.initialize();
            registerMessageHandlers(handler, this);
        }
    }

    /**
     * COPIED FROM: org.powertac.samplebroker.core.PowerTacBroker
     * <p>
     * Finds all the handleMessage() methods and registers them.
     *
     * @param router
     */
    private void registerMessageHandlers(Object thing, MessageDispatcher router) {
        try {
            thing = getTargetObject(thing, MessageHandler.class);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Class<?> thingClass = thing.getClass();
        Method[] methods = thingClass.getMethods();

        log.info(thingClass.getSimpleName() + " has " + methods.length + " methods");
        Arrays.asList(methods).forEach((method) -> log.info(method.getName()));

        for (Method method : methods) {
            if (method.getName().equals("handleMessage")) {
                Class<?>[] args = method.getParameterTypes();
                if (1 == args.length) {
                    log.info("Register " + thing.getClass().getSimpleName()
                            + ".handleMessage(" + args[0].getSimpleName() + ")");
                    router.registerMessageHandler(thing, args[0]);
                }
            }
        }
    }

    /**
     * Sets up handlers for incoming messages by message type.
     */
    private void registerMessageHandler(Object handler, Class<?> messageType) {
        log.info("Registering " + handler.toString() + " for " + messageType.getSimpleName());
        Set<Object> reg = registrations.get(messageType);
        if (reg == null) {
            reg = new HashSet<>();
            registrations.put(messageType, reg);
        }
        reg.add(handler);
    }

    @SuppressWarnings("unchecked")
    private <T> T getTargetObject(Object proxy, Class<?> targetClass) throws Exception {
        // TODO Replace with if?
        while ((AopUtils.isJdkDynamicProxy(proxy))) {
            return (T) getTargetObject(
                    ((Advised) proxy).getTargetSource().getTarget(),
                    targetClass);
        }
        // expected to be cglib proxy then, which is simply a specialized class
        return (T) proxy;
    }
}
