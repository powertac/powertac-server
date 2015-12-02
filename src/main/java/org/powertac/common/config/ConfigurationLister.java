/**
 * 
 */
package org.powertac.common.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is a helper class for generating a sample configuration file from configured instances.
 * To use it, you need to create and configure a set of instances, then open an output stream
 * and run an instance of this class across the configured instances to generate the output
 * text.
 * @author John Collins
 */
public class ConfigurationLister
{
  static private Logger log = LogManager.getLogger(ConfigurationLister.class);

}
