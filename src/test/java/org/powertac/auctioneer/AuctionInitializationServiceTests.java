package org.powertac.auctioneer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.powertac.common.Competition;
import org.powertac.common.PluginConfig;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.test.util.ReflectionTestUtils;

public class AuctionInitializationServiceTests
{
  PluginConfigRepo pluginConfigRepo;
  AuctionInitializationService svc;
  
  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    pluginConfigRepo = new PluginConfigRepo();
    svc = new AuctionInitializationService();
    ReflectionTestUtils.setField(svc, "pluginConfigRepo", pluginConfigRepo);
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
}
