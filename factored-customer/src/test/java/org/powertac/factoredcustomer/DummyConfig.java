package org.powertac.factoredcustomer;

import org.apache.commons.configuration2.CompositeConfiguration;
import org.powertac.common.config.Configurator;
import org.powertac.common.interfaces.ServerConfiguration;

import java.util.Collection;
import java.util.List;


class DummyConfig implements ServerConfiguration
{
  private Configurator configurator;

  DummyConfig ()
  {
    super();
  }

  void initialize ()
  {
    configurator = new Configurator();
    CompositeConfiguration config = new CompositeConfiguration();

    String[] names = {"test-properties.xml", "BrooksideHomes.xml",
        "FrostyStorage.xml", "MedicalCenter.xml", "WindmillCoOp.xml"};

    try {
      for (String name : names) {
        config.addConfiguration(Configurator.readXML("config/" + name));
      }
      configurator.setConfiguration(config);
    }
    catch (Exception e) {
      System.out.println("Error loading configuration: " + e.toString());
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
