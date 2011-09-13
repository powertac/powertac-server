package org.powertac.common.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringApplicationContext implements ApplicationContextAware
{
  private static ApplicationContext context;
  
  public void setApplicationContext (ApplicationContext appContext)
  throws BeansException
  {
    context = appContext;    
  }
  
  public static Object getBean (String beanName)
  {
    return context.getBean(beanName);
  }
}
