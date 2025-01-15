/**
 * 
 */
package org.powertac.factoredcustomer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.AbstractConfiguration;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.RandomSeed;
import org.powertac.common.Rate;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.Timeslot;
import org.powertac.common.WeatherReport;
import org.powertac.common.config.Configurator;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.factoredcustomer.interfaces.FactoredCustomer;
import org.powertac.factoredcustomer.interfaces.StructureInstance;

/**
 * @author John Collins
 *
 */
public class FrostyStorageTest
{
  AbstractConfiguration configuration;
  Configurator config;
  FactoredCustomerService fcs;
  ServerConfiguration serverConfig;
  List<FactoredCustomer> customers;
  Broker broker;
  RandomSeedRepo randomSeedRepo;
  CustomerRepo customerRepo;
  TariffMarket tariffMarket;
  Tariff tariff;
  TariffSubscriptionRepo tariffSubscriptionRepo;
  TimeslotRepo timeslotRepo;
  TimeService timeService;
  WeatherReportRepo weatherReportRepo;

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  public void setUp () throws Exception
  {
    File fs = new File("src/test/resources/config/FrostyStorage.xml");
    configuration = Configurator.readXML(fs);
    config = new Configurator();
    config.setConfiguration(configuration);
    customers = new ArrayList<>();
    fcs = mock(FactoredCustomerService.class);

    randomSeedRepo = mock(RandomSeedRepo.class);
    when(fcs.getRandomSeedRepo()).thenReturn(randomSeedRepo);
    RandomSeed rs = mock(RandomSeed.class);
    when(randomSeedRepo.getRandomSeed(anyString(),
                                      anyLong(),
                                      anyString()))
    .thenReturn(rs);
    when(rs.nextDouble()).thenReturn(0.5);

    timeService = mock(TimeService.class);
    when(fcs.getTimeService()).thenReturn(timeService);

    timeslotRepo = mock(TimeslotRepo.class);
    when(fcs.getTimeslotRepo()).thenReturn(timeslotRepo);

    customerRepo = new CustomerRepo();
    when(fcs.getCustomerRepo()).thenReturn(customerRepo);

    tariffMarket = mock(TariffMarket.class);
    when(fcs.getTariffMarket()).thenReturn(tariffMarket);

    weatherReportRepo = mock(WeatherReportRepo.class);
    when(fcs.getWeatherReportRepo()).thenReturn(weatherReportRepo);

    broker = mock(Broker.class);
    TariffSpecification defaultConsumption =
            new TariffSpecification(broker, PowerType.CONSUMPTION)
            .addRate(new Rate().withValue(-0.40));
    tariff = new Tariff(defaultConsumption);
    //tariff.init();
    when(tariffMarket.getDefaultTariff(PowerType.CONSUMPTION)).thenReturn(tariff);
    tariffSubscriptionRepo = mock(TariffSubscriptionRepo.class);
    when(fcs.getTariffSubscriptionRepo()).thenReturn(tariffSubscriptionRepo);

    // mock serverConfig
    serverConfig = new LocalConfig();

    // setup code from FactoredCustomerService
    CustomerFactory customerFactory = new CustomerFactory();
    customerFactory.registerDefaultCreator(DefaultFactoredCustomer.getCreator());

    Config.initializeInstance(serverConfig);
    Config config = Config.getInstance();
    config.configure();

    Map<String, StructureInstance> customerStructures =
        config.getStructures().get("CustomerStructure");

    for (StructureInstance instance : customerStructures.values()) {
      CustomerStructure customerStructure = (CustomerStructure) instance;
      FactoredCustomer customer = customerFactory.processStructure(customerStructure);
      if (customer != null) {
        customer.initialize(fcs);
        customers.add(customer);
      }
      else {
        System.out.println("Failed to create customer instance");
        throw new Error("Failed to create customer instance");
      }
    }

  }

  /**
   * @throws java.lang.Exception
   */
  @AfterEach
  public void tearDown () throws Exception
  {
  }

  @Test
  public void testConfig ()
  {
    assertEquals(3, configuration.getInt("factoredcustomer.defaultCapacityBundle.FrostyStorage.count"), "correct count");
  }

  @Test
  public void testSetup ()
  {
    assertEquals(1, customers.size(), "one customer created");
    DefaultFactoredCustomer fs = (DefaultFactoredCustomer)customers.get(0);
    assertEquals(fs.getName(), "FrostyStorage", "correct name");
  }

  @Test
  public void testUsageOneSub ()
  {
    // retrieve the CustomerInfo
    DefaultFactoredCustomer fs = (DefaultFactoredCustomer)customers.get(0);
    CustomerInfo ci = fs.getCapacityBundles().get(0).getCustomerInfo();
    
    // create a subscription
    TariffSubscription sub = mock(TariffSubscription.class);
    when(sub.getTariff()).thenReturn(tariff);
    when(sub.getCustomersCommitted()).thenReturn(3);
    sub.setCustomersCommitted(3);
    List<TariffSubscription> subs = new ArrayList<>();
    subs.add(sub);
    when(tariffSubscriptionRepo.findActiveSubscriptionsForCustomer(ci))
    .thenReturn(subs);

    List<Double> usePowerArgs = new ArrayList<>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        usePowerArgs.add((Double)args[0]);
        return null;
      }
    }).when(sub).usePower(anyDouble());
    List<RegulationCapacity> regCapacityArgs = new ArrayList<>();
    doAnswer(new Answer<Object>() {
      @Override
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        regCapacityArgs.add((RegulationCapacity)args[0]);
        return null;
      }
    }).when(sub).setRegulationCapacity(any(RegulationCapacity.class));

    // Monday 1:00
    ZonedDateTime start = ZonedDateTime.of(2018, 2, 5, 1, 0, 0, 0, ZoneOffset.UTC);
    Timeslot ts = new Timeslot(300, start.toInstant());
    when(timeslotRepo.currentSerialNumber()).thenReturn(300);
    when(timeService.getCurrentDateTime()).thenReturn(start);
    WeatherReport weather = new WeatherReport(300, 20.0, 0.0, 0.0, 0.0);
    when(weatherReportRepo.currentWeatherReport()).thenReturn(weather);
    fs.getUtilityOptimizer().step();
    assertEquals(3412.17, usePowerArgs.get(0), 1e-4, "correct power usage");
    //System.out.println("FrostyStorageTest usePower: " + usePowerArgs.toString());
  }

  // ------------------------------------------------------------------------
  class LocalConfig implements ServerConfiguration
  {

    @Override
    public void configureMe (Object target)
    {
      config.configureSingleton(target);
    }

    @Override
    public Collection<?> configureInstances (Class<?> target)
    {
      return config.configureInstances(target);
    }

    @Override
    public Collection<?> configureNamedInstances (List<?> instances)
    {
      return config.configureNamedInstances(instances);
    }

    @Override
    public void publishConfiguration (Object target)
    {
    }

    @Override
    public void saveBootstrapState (Object thing)
    {
    }
    
  }
}
