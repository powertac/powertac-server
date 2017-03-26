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

  ApplicationContext context;
  LogtoolCore core;

  /** Set up the Spring context */
  protected void initialize() {
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("logtool.xml");
    ctx.registerShutdownHook();
    setContext(ctx);
  }

  protected void setContext(ApplicationContext context) {
    this.context = context;
    // find the LogtoolCore bean
    this.core = (LogtoolCore)context.getBeansOfType(LogtoolCore.class).values().toArray()[0];
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
    if (context == null) {
      initialize();
    }
    return core;
  }

  /**
   * Retrieves a Spring component instance by name
   */
  protected Object getBean (String beanName)
  {
    return context.getBean(beanName);
  }
  
  /**
   * default command-line processor. We assume a single arg, a filename
   */
  protected void cli (String inputFile, Analyzer analyzer)
  {
    LogtoolCore core = getCore();
    core.readStateLog(inputFile, analyzer);
  }

  /**
   * Passthrough for event registration
   */
  protected void registerNewObjectListener (NewObjectListener listener,
                                            Class<?> type)
  {
    DomainObjectReader dor = (DomainObjectReader) getBean("domainObjectReader");
    dor.registerNewObjectListener(listener, type);
  }
}
