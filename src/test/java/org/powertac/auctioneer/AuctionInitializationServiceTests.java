package org.powertac.auctioneer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.interfaces.ServerProperties;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.test.util.ReflectionTestUtils;

public class AuctionInitializationServiceTests
{
  PluginConfigRepo pluginConfigRepo;
  AuctionInitializationService svc;

  @Before
  public void setUp () throws Exception
  {
    pluginConfigRepo = new PluginConfigRepo();
    svc = new AuctionInitializationService();
    ReflectionTestUtils.setField(svc, "pluginConfigRepo", pluginConfigRepo);
    ReflectionTestUtils.setField(svc, "serverProps", new LocalServerProperties());
  }

  @Test
  public void testSetDefaults ()
  {
    svc.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("Auctioneer");
    assertNotNull("found config", config);
    assertEquals("correct surplus", "0.5",
                 config.getConfigurationValue("sellerSurplus"));
  }

  @Test
  public void testInitialize ()
  {
    svc.setDefaults();
    PluginConfig config = pluginConfigRepo.findByRoleName("Auctioneer");
    AuctionService auction = mock(AuctionService.class);
    Competition competition = mock(Competition.class);
    ReflectionTestUtils.setField(svc, "auctionService", auction);
    String result = svc.initialize(competition, new ArrayList<String>());
    assertEquals("correct result", "Auctioneer", result);
    verify(auction).init(config);
  }
  
  // local SystemProperties implementation
  class LocalServerProperties implements ServerProperties
  {
    @Override
    public String getProperty (String name)
    {
      return null;
    }

    @Override
    public String getProperty (String name, String defaultValue)
    {
      return defaultValue;
    }

    @Override
    public Integer getIntegerProperty (String name, Integer defaultValue)
    {
      return defaultValue;
    }

    @Override
    public Double getDoubleProperty (String name, Double defaultValue)
    {
      return defaultValue;
    }
    
  }
}
