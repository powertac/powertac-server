package org.powertac.visualizer;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;

public class VisualizerApplicationContext implements ApplicationContextAware
{

  private static ApplicationContext CONTEXT;

  /**
   * This method is called from within the ApplicationContext once it is done
   * starting up, it will stick a reference to itself into this bean.
   * 
   * @param context
   *          a reference to the ApplicationContext.
   */
  public void setApplicationContext (ApplicationContext context)
    throws BeansException
  {
    CONTEXT = context;
  }

  /**
   * This is about the same as context.getBean("beanName"), except it has its
   * own static handle to the Spring context, so calling this method
   * statically will give access to the beans by name in the Spring
   * application context. As in the context.getBean("beanName") call, the
   * caller must cast to the appropriate target class. If the bean does not
   * exist, then a Runtime error will be thrown.
   * 
   * @param beanName
   *          the name of the bean to get.
   * @return an Object reference to the named bean.
   */
  public static Object getBean (String beanName)
  {
    return CONTEXT.getBean(beanName);
  }

  /**
   * Returns all the Spring beans that are instances of the given type.
   */
  public static <T> List<T> listBeansOfType (Class<T> type)
  {
    if (null == CONTEXT)
      System.out.println("Context is null!");
    return new ArrayList<T>(CONTEXT.getBeansOfType(type).values());
  }
}
