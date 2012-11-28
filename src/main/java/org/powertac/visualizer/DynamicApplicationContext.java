package org.powertac.visualizer;

import org.springframework.web.context.support.XmlWebApplicationContext;


public class DynamicApplicationContext extends XmlWebApplicationContext {

  protected String[] getDefaultConfigLocations () {
    String cmd = System.getProperty("sun.java.command");

    if (cmd.contains("jetty")) {
      return new String[] {
          DEFAULT_CONFIG_LOCATION_PREFIX + "spring/visualizer.embedded.xml"};
    } else {
      return new String[] {
          DEFAULT_CONFIG_LOCATION_PREFIX + "spring/visualizer.tournament.xml"};
    }
  }
}