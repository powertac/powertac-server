/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.logtool;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.logtool.common.DomainObjectReader;
import org.powertac.logtool.common.NewObjectListener;
import org.powertac.logtool.ifc.Analyzer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Abstract class to hook an Analyzer into the Spring Context. For this
 * to work, the Analyzer must first call getCore() to retrieve the
 * LogtoolCore reference, then call the readStateLog() method on the
 * LogtoolCore to do the analysis.
 * 
 * @author John Collins
 */
public abstract class LogtoolContext
{
  static Logger log = LogManager.getLogger(LogtoolContext.class);

  protected ApplicationContext context;
  protected LogtoolCore core;
  protected DomainObjectReader dor;
  
  // Common number formatting in subclasses
  protected DecimalFormat df;


  /** Set up the Spring context, initialize common number formatting setup */
  protected void initialize() {
    ClassPathXmlApplicationContext ctx =
        new ClassPathXmlApplicationContext("logtool.xml");
    ctx.registerShutdownHook();
    setContext(ctx);
    
    df = (DecimalFormat)NumberFormat.getInstance(Locale.US);
    df.setMaximumFractionDigits(4);
    df.setGroupingUsed(false);
  }

  protected void setContext(ApplicationContext context) {
    if (this.context != null) {
      log.warn("Resetting application context!");
      log.debug("Current context " + this.context.getClass().getName());
      log.debug("New context " + context.getClass().getName());
    }
    this.context = context;
    // register handlers
    registerMessageHandlers();
  }

  /**
   * Finds all the handleMessage() methods and registers them.
   */
  protected void registerMessageHandlers ()
  {
    Class<?> thingClass = this.getClass();
    log.info("Analyzing class {}", thingClass.getName());
    Method[] methods = thingClass.getMethods();
    for (Method method : methods) {
      if (method.getName().equals("handleMessage")) {
        Class<?>[] args = method.getParameterTypes();
        if (1 == args.length) {
          log.info("Register " + this.getClass().getSimpleName()
                   + ".handleMessage(" + args[0].getSimpleName() + ")");
          registerMessageListener(args[0]);
        }
      }
    }
  }

  /**
   * Return the ApplicationContext
   */
  protected ApplicationContext getContext () {
    return context;
  }

  /**
   * Returns LogtoolCore instance
   */
  protected LogtoolCore getCore ()
  {
    if (core == null) {
      core = (LogtoolCore) getBean("logtoolCore");
    }
    return core;
  }

  /**
   * Returns DomainObjectReader instance
   */
  protected DomainObjectReader getDomainObjectReader ()
  {
    if (dor == null) {
      dor = (DomainObjectReader) getBean("domainObjectReader");
    }
    return dor;
  }

  /**
   * Retrieves a Spring component instance by name
   */
  protected Object getBean (String beanName)
  {
    if (context == null) {
      initialize();
    }
    return context.getBean(beanName);
  }
  
  /**
   * default command-line processor. We assume a single arg, a filename
   */
  protected void cli (String inputFile, Analyzer analyzer)
  {
    LogtoolCore core = getCore();
    core.recycleRepos();
    core.readStateLog(inputFile, analyzer);
  }

  /**
   * Passthrough for event registration
   */
  protected void registerNewObjectListener (NewObjectListener listener,
                                            Class<?> type)
  {
    DomainObjectReader dor = getDomainObjectReader();
    dor.registerNewObjectListener(listener, type);
  }

  protected void registerMessageListener (Class<?> type)
  {
    DomainObjectReader dor = getDomainObjectReader();
    dor.registerMessageListener(this, type);
  }
}
