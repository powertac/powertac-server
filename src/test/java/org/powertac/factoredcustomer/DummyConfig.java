package org.powertac.factoredcustomer;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.ServerConfiguration;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.fail;


class DummyConfig implements ServerConfiguration
{
  private Configurator configurator;

  DummyConfig ()
  {
    super();
  }

  void initialize ()
  {
    CompositeConfiguration config = new CompositeConfiguration();
    configurator = new Configurator();
    InputStream stream =
        ConfigTest.class.getResourceAsStream("/config/test-properties.xml");
    XMLConfiguration xconfig = new XMLConfiguration();
    try {
      xconfig.load(stream);
      config.addConfiguration(xconfig);
      configurator.setConfiguration(config);
    }
    catch (ConfigurationException e) {
      e.printStackTrace();
      fail(e.toString());
    }
  }

  @Override
  public void configureMe (Object target)
  {
    configurator.configureSingleton(target);
  }

  @Override
  public Collection<?> configureInstances (Class<?> target)
  {
    return configurator.configureInstances(target);
  }

  @Override
  public void publishConfiguration (Object target)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void saveBootstrapState (Object thing)
  {
    // TODO Auto-generated method stub

  }

  @Override
  public Collection<?> configureNamedInstances (List<?> instances)
  {
    // Auto-generated method stub
    return null;
  }
}
