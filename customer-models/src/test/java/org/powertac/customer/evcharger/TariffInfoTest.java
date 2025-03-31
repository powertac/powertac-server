package org.powertac.customer.evcharger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Broker;
import org.powertac.common.CapacityProfile;
import org.powertac.common.Competition;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerServiceAccessor;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.springframework.test.util.ReflectionTestUtils;

class TariffInfoTest
{
  private Competition competition;
  private CustomerServiceAccessor serviceAccessor;
  private Instant start;
  private TimeService timeService;
  private TimeslotRepo timeslotRepo;
  private TariffRepo tariffRepo;
  private RandomSeedRepo mockSeedRepo;
  private RandomSeed seed;


  private Broker defaultBroker;
  private Tariff defaultConsumption;
  private Tariff evTariff;
  private Broker bob;
  private Tariff myTariff;

  private EvCharger evCharger;

  private TariffInfo uut;
  
  @BeforeEach
  void setUp () throws Exception
  {
    competition = Competition.newInstance("tariff-info-test");
    Competition.setCurrent(competition);
    timeService = new TimeService();
    timeService.setCurrentTime(competition.getSimulationBaseTime());
    timeslotRepo = new TimeslotRepo();
    ReflectionTestUtils.setField(timeslotRepo, "timeService", timeService);
    tariffRepo = new TariffRepo();

    // set up randomSeed mock
    mockSeedRepo = mock(RandomSeedRepo.class);
    seed = mock(RandomSeed.class);
    when(mockSeedRepo.getRandomSeed(anyString(),
                                    anyLong(),
                                    anyString())).thenReturn(seed);

    serviceAccessor = new ServiceAccessor();

    defaultBroker = new Broker("default");
    TariffSpecification dcSpec =
            new TariffSpecification(defaultBroker,
                                    PowerType.CONSUMPTION).
                                    addRate(new Rate().withValue(-0.6));
    defaultConsumption = new Tariff(dcSpec);
    initTariff(defaultConsumption);

    evCharger = new EvCharger("test-dummy");
    evCharger.setServiceAccessor(serviceAccessor);
    ArrayList<String> data =
            new ArrayList<>(Arrays.asList("3.0", "3.0", "3.0", "3.0", "3.0", "3.0",
                                          "3.0", "4.0", "4.0", "4.0", "3.0", "3.0",
                                          "4.0", "4.0", "4.0", "4.0", "4.0", "4.0",
                                          "5.0", "6.0", "7.0", "6.0", "5.0", "4.0"));
    evCharger.setDefaultCapacityData(data);
    //evCharger.setDefaultCapacityData("3.0-3.0-3.0-3.0-3.0-3.0-3.0-4.0-4.0-4.0-3.0-3.0-"
    //        + "4.0-4.0-4.0-4.0-4.0-4.0-5.0-6.0-7.0-6.0-5.0-4.0");
    evCharger.initialize();
  }

  // initializes a tariff. It needs dependencies injected
  private void initTariff (Tariff tariff)
  {
    ReflectionTestUtils.setField(tariff, "timeService", timeService);
    ReflectionTestUtils.setField(tariff, "tariffRepo", tariffRepo);
    tariff.init();
  }

  @Test
  void testGetCapacityProfileDefault ()
  {
    TariffInfo uut = new TariffInfo(evCharger, defaultConsumption);
    assertEquals(defaultConsumption, uut.getTariff());
    CapacityProfile cp = uut.getCapacityProfile();
    assertNotNull(cp);
    ArrayList<DemandElement>[] lists = new ArrayList[2];
    //ArrayList<DemandElement>
    //dummy.add(new ArrayList<String>("1.0", "1.5", "2.0"));
    //dummy.add("-2.0, -3.0, 4.0");
    //System.out.println(dummy.toString());
  }

  @Test
  void testGenerateTouProfile ()
  {
    //fail("Not yet implemented");
  }

  @Test
  void testComputeRegulationPremium ()
  {
    //fail("Not yet implemented");
  }

  @Test
  void testGenerateTouRegProfile ()
  {
    //fail("Not yet implemented");
  }

  @Test
  void testDetermineUsage ()
  {
    //fail("Not yet implemented");
  }

  class ServiceAccessor implements CustomerServiceAccessor
  {

    @Override
    public CustomerRepo getCustomerRepo ()
    {
      return null;
    }

    @Override
    public RandomSeedRepo getRandomSeedRepo ()
    {
      return mockSeedRepo;
    }

    @Override
    public TariffRepo getTariffRepo ()
    {
      return tariffRepo;
    }

    @Override
    public TariffSubscriptionRepo getTariffSubscriptionRepo ()
    {
      return null; //tariffSubscriptionRepo;
    }

    @Override
    public TimeslotRepo getTimeslotRepo ()
    {
      return timeslotRepo;
    }

    @Override
    public TimeService getTimeService ()
    {
      return timeService;
    }

    @Override
    public WeatherReportRepo getWeatherReportRepo ()
    {
      return null; //mockWeatherRepo;
    }

    @Override
    public ServerConfiguration getServerConfiguration ()
    {
      // Auto-generated method stub
      return null;
    }

    @Override
    public TariffMarket getTariffMarket ()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public XMLMessageConverter getMessageConverter ()
    {
      // TODO Auto-generated method stub
      return null;
    }
  }
}
