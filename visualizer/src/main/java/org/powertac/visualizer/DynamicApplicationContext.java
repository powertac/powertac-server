package org.powertac.visualizer;

import org.powertac.util.ProxyAuthenticator;
import org.springframework.web.context.support.XmlWebApplicationContext;


public class DynamicApplicationContext extends XmlWebApplicationContext
{
  protected String[] getDefaultConfigLocations ()
  {
    // Check for proxy settings, useSocks=false for visaulizer
    new ProxyAuthenticator(false);

    String cmd = System.getProperty("sun.java.command");
    if (cmd.contains("jetty") || cmd.contains("web")) {
      return new String[]{
          DEFAULT_CONFIG_LOCATION_PREFIX + "spring/visualizer.embedded.xml"};
    }
    else {
      try {
        String contextPath = getServletContext().getContextPath();
        String[] parts = contextPath.split("/");
        System.setProperty("s", "s");
        System.setProperty("simNode", parts[parts.length - 1] + "-");
      }
      catch (Exception ignore) {
      }

      return new String[]{
          DEFAULT_CONFIG_LOCATION_PREFIX + "spring/visualizer.tournament.xml"};
    }
  }
}
