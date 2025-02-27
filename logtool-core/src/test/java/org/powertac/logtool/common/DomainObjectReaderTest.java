package org.powertac.logtool.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.util.HashMap;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.DistributionTransaction;
import org.powertac.common.HourlyCharge;
import org.powertac.common.Order;
import org.powertac.common.Rate;
import org.powertac.common.RegulationRate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TariffTransaction;
import org.powertac.common.msg.BalancingOrder;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.TariffRepo;
import org.springframework.test.util.ReflectionTestUtils;


public class DomainObjectReaderTest
{
  private DomainObjectReader dor;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    HashMap<String, String[]> schema = new HashMap<>();
    schema.put("org.powertac.common.TariffSpecification",
               "broker,powerType,expiration,minDuration,signupPayment,earlyWithdrawPayment,periodicPayment,supersedes"
               .split(","));
    schema.put("org.powertac.common.Rate",
               "tariffId,weeklyBegin,weeklyEnd,dailyBegin,dailyEnd,tierThreshold,fixed,minValue,maxValue,noticeInterval,expectedMean,maxCurtailment"
               .split(","));
    schema.put("org.powertac.common.RegulationRate",
               "tariffId,response,upRegulationPayment,downRegulationPayment"
               .split(","));
    schema.put("org.powertac.common.HourlyCharge",
               "rateId,value,atTime".split(","));
    schema.put("org.powertac.common.msg.BalancingOrder",
               "exerciseRatio,price,tariffId,broker".split(","));
            
    dor = new DomainObjectReader();
    dor.setSchema(schema);
  }

  @Test
  public void testReadSingleObject ()
  {
    String aston = "144669:org.powertac.common.Broker::603::new::AstonTAC";
    try {
      Object result = dor.readObject(aston);
      assertNotNull(result, "created an instance");
      assertEquals(result.getClass().getName(), "org.powertac.common.Broker", "correct class");
      assertEquals(603, ((Broker)result).getId(), "correct id");
      assertEquals(result, dor.getById(603), "object stored in map");
    }
    catch (MissingDomainObject mdo) {
      fail("bad exception " + mdo.toString());
    }
  }

  @Test
  public void testReadSingleObjectAbbr ()
  {
    String aston = "144669:c.Broker::603::new::AstonTAC";
    try {
      Object result = dor.readObject(aston);
      assertNotNull(result, "created an instance");
      assertEquals(result.getClass().getName(), "org.powertac.common.Broker", "correct class");
      assertEquals(603, ((Broker)result).getId(), "correct id");
      assertEquals(result, dor.getById(603), "object stored in map");
    }
    catch (MissingDomainObject mdo) {
      fail("bad exception " + mdo.toString());
    }
  }

  @Test
  public void read2Objects ()
  {
    String aston = "144669:org.powertac.common.Broker::603::new::AstonTAC";
    String dt = "189426:org.powertac.common.DistributionTransaction::3459::new::603::42::-0.0::0.0";
    try {
      Broker broker = (Broker)dor.readObject(aston);
      Object result = dor.readObject(dt);
      assertEquals(result.getClass().getName(), "org.powertac.common.DistributionTransaction", "correct class");
      DistributionTransaction dtx = (DistributionTransaction)result;
      assertEquals(3459, dtx.getId(), "correct id");
      assertEquals(result, dor.getById(3459), "object stored in map");
      assertEquals(broker, dtx.getBroker(), "broker stored");
      assertEquals(broker, dor.getById(603), "broker stored in map");
      assertEquals(dtx, dor.getById(3459), "tx stored in map");
    }
    catch (MissingDomainObject mdo) {
      fail("bad exception " + mdo.toString());
    }
  }
  
  @Test
  public void readRRObject ()
  {
    String aston = "144669:org.powertac.common.Broker::603::new::AstonTAC";
    //String ts = "13678:org.powertac.common.Timeslot::579::new::362::2009-01-03T02:00:00.000Z::null";
    String order = "180915:org.powertac.common.Order::400000393::new::603::42::2.109375::-31.835472671068615";
    try {
      Broker broker = (Broker)dor.readObject(aston);
      //Timeslot timeslot = (Timeslot)dor.readObject(ts);
      //assertNotNull(timeslot, "timeslot created");
      Object result = dor.readObject(order);
      assertNotNull(result, "order created");
      assertEquals(result.getClass().getName(), "org.powertac.common.Order", "correct class");
      Order o = (Order)result;
      assertEquals(400000393, o.getId(), "correct id");
      assertEquals(o, dor.getById(400000393), "order in map");
      assertEquals(broker, o.getBroker(), "correct broker");
      assertEquals(2.109375, o.getMWh(), 1e-6, "correct mwh");
      assertEquals(-31.835472671068615, o.getLimitPrice(), 1e-6, "correct price");
    }
    catch (MissingDomainObject mdo) {
      fail("bad exception " + mdo.toString());
    }
  }

  @Test
  public void readRRTariffSpec ()
  {
    String brokerLog = "42476:org.powertac.common.Broker::4773::new::Bunnie";
    String regRateLog = "88002:org.powertac.common.RegulationRate::501428076::-rr::501428079::SECONDS::0.6::-0.15";
    String rateLog = "88003:org.powertac.common.Rate::501428077::-rr::501428079::-1::-1::-1::-1::0.0::true::-0.4::0.0::0::0.0::0.0";
    String specLog = "88003:org.powertac.common.TariffSpecification::501428079::-rr::4773::BATTERY_STORAGE::0::14::10.0::-20.0::0.0::null";

    // We need the builder here to connect rates to tariff specs
    DomainBuilder builder = new DomainBuilder();
    TariffRepo trepo = mock(TariffRepo.class);
    ReflectionTestUtils.setField(builder, "tariffRepo", trepo);
    BrokerRepo brepo = mock(BrokerRepo.class);
    ReflectionTestUtils.setField(builder, "brokerRepo", brepo);    
    ReflectionTestUtils.setField(builder, "dor", dor);
    builder.setup();

    try {
      Object result = dor.readObject(brokerLog);
      assertNotNull(result, "broker created");

      result = dor.readObject(regRateLog);
      assertNotNull(result, "regRate created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.RegulationRate",
                   "regRate correct class");
      RegulationRate regRate = (RegulationRate)result;

      result = dor.readObject(rateLog);
      assertNotNull(result, "rate created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.Rate",
                   "rate correct class");
      Rate rate = (Rate)result;

      result = dor.readObject(specLog);
      assertNotNull(result, "spec created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.TariffSpecification",
                   "spec correct class");
      TariffSpecification spec = (TariffSpecification)result;
      
      assertEquals(1, spec.getRates().size(), "one rate");
      assertEquals(501428077, spec.getRates().get(0).getId(), "correct rate ID");
      assertEquals(1, spec.getRegulationRates().size(), "one regRate");
      assertEquals(501428076, spec.getRegulationRates().get(0).getId(), "correct regRate ID");
    }
    catch (MissingDomainObject mdo) {
      fail("bad exception " + mdo.toString());
    }
  }

  @Test
  public void readRRTariffSpec_18 ()
  {
    String brokerLog = "42476:org.powertac.common.Broker::4773::new::Bunnie";
    String regRateLog = "88002:org.powertac.common.RegulationRate::501428076::-rr::501428079::SECONDS::0.6::-0.15";
    String rateLog = "88003:org.powertac.common.Rate::501428077::-rr::501428079::-1::-1::-1::-1::0.0::true::-0.4::0.0::0::0.0::0.0";
    String specLog = "88003:org.powertac.common.TariffSpecification::501428079::-rr::4773::BATTERY_STORAGE::null::14::10.0::-20.0::0.0::null";

    // We need the builder here to connect rates to tariff specs
    DomainBuilder builder = new DomainBuilder();
    TariffRepo trepo = mock(TariffRepo.class);
    ReflectionTestUtils.setField(builder, "tariffRepo", trepo);
    BrokerRepo brepo = mock(BrokerRepo.class);
    ReflectionTestUtils.setField(builder, "brokerRepo", brepo);    
    ReflectionTestUtils.setField(builder, "dor", dor);
    builder.setup();

    try {
      Object result = dor.readObject(brokerLog);
      assertNotNull(result, "broker created");

      result = dor.readObject(regRateLog);
      assertNotNull(result, "regRate created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.RegulationRate",
                   "regRate correct class");
      RegulationRate regRate = (RegulationRate)result;

      result = dor.readObject(rateLog);
      assertNotNull(result, "rate created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.Rate",
                   "rate correct class");
      Rate rate = (Rate)result;

      result = dor.readObject(specLog);
      assertNotNull(result, "spec created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.TariffSpecification",
                   "spec correct class");
      TariffSpecification spec = (TariffSpecification)result;
      
      assertEquals(1, spec.getRates().size(), "one rate");
      assertEquals(501428077, spec.getRates().get(0).getId(), "correct rate ID");
      assertEquals(1, spec.getRegulationRates().size(), "one regRate");
      assertEquals(501428076, spec.getRegulationRates().get(0).getId(), "correct regRate ID");
    }
    catch (MissingDomainObject mdo) {
      fail("bad exception " + mdo.toString());
    }
  }

  @Test
  public void readRRTariffSpecInstant ()
  {
    String brokerLog = "42476:org.powertac.common.Broker::4773::new::Bunnie";
    String regRateLog = "88002:org.powertac.common.RegulationRate::501428076::-rr::501428079::SECONDS::0.6::-0.15";
    String rateLog = "88003:org.powertac.common.Rate::501428077::-rr::501428079::-1::-1::-1::-1::0.0::true::-0.4::0.0::0::0.0::0.0";
    String specLog = "88003:org.powertac.common.TariffSpecification::501428079::-rr::4773::SOLAR_PRODUCTION::2020-06-06T09:21:43.384Z::14::10.0::-20.0::0.0::null";

    // We need the builder here to connect rates to tariff specs
    DomainBuilder builder = new DomainBuilder();
    TariffRepo trepo = mock(TariffRepo.class);
    ReflectionTestUtils.setField(builder, "tariffRepo", trepo);
    BrokerRepo brepo = mock(BrokerRepo.class);
    ReflectionTestUtils.setField(builder, "brokerRepo", brepo);    
    ReflectionTestUtils.setField(builder, "dor", dor);
    builder.setup();

    try {
      Object result = dor.readObject(brokerLog);
      assertNotNull(result, "broker created");

      result = dor.readObject(regRateLog);
      assertNotNull(result, "regRate created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.RegulationRate",
                   "regRate correct class");
      RegulationRate regRate = (RegulationRate)result;

      result = dor.readObject(rateLog);
      assertNotNull(result, "rate created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.Rate",
                   "rate correct class");
      Rate rate = (Rate)result;

      result = dor.readObject(specLog);
      assertNotNull(result, "spec created");
      assertEquals(result.getClass().getName(),
                   "org.powertac.common.TariffSpecification",
                   "spec correct class");
      TariffSpecification spec = (TariffSpecification)result;
      assertEquals(Instant.class, spec.getExpiration().getClass(), "created an Instant");
      
      assertEquals(1, spec.getRates().size(), "one rate");
      assertEquals(501428077, spec.getRates().get(0).getId(), "correct rate ID");
      assertEquals(1, spec.getRegulationRates().size(), "one regRate");
      assertEquals(501428076, spec.getRegulationRates().get(0).getId(), "correct regRate ID");
    }
    catch (MissingDomainObject mdo) {
      fail("bad exception " + mdo.toString());
    }
  }

//  @Test
//  public void readList ()
//  {
//    //String slot1 = "13680:org.powertac.common.Timeslot::597::new::380::2009-01-03T20:00:00.000Z::null";
//    String fp1 = "176271:org.powertac.common.WeatherForecastPrediction::1203::new::23::-6.447321391818082::2.657257536071654::121.71284773822428::0.375";
//    String fp2 = "176272:org.powertac.common.WeatherForecastPrediction::1204::new::24::-7.327664553479619::1.8344251307130162::114.31156703428204::0.375";
//    String forecast = "176272:org.powertac.common.WeatherForecast::1205::new::31::(1203,1204)";
//    try {
//      //Timeslot ts1 = (Timeslot)dor.readObject(slot1);
//      WeatherForecastPrediction wfp1 = (WeatherForecastPrediction)dor.readObject(fp1);
//      WeatherForecastPrediction wfp2 = (WeatherForecastPrediction)dor.readObject(fp2);
//      Object result = dor.readObject(forecast);
//      assertNotNull(result, "read a forecast");
//      assertEquals(result.getClass().getName(), "org.powertac.common.WeatherForecast", "correct class");
//      WeatherForecast wf = (WeatherForecast)result;
//      List<WeatherForecastPrediction> predictions = wf.getPredictions();
//      assertEquals(2, predictions.size(), "correct number of predictions");
//      assertEquals(wfp1, predictions.get(0), "correct first prediction");
//      assertEquals(wfp2, predictions.get(1), "correct second prediction");
//    }
//    catch (MissingDomainObject mdo) {
//      fail("bad exception " + mdo.toString());
//    }
//  }
  
  @Test
  public void readSubstituteClass ()
  {
    String du = "225:org.powertac.du.DefaultBrokerService$LocalBroker::1::new::default broker";
    try {
      Object result = dor.readObject(du);
      assertNotNull(result, "non-null result");
      assertEquals(result.getClass().getName(), "org.powertac.du.DefaultBroker", "correct class");
      Broker broker = (Broker) result;
      assertEquals(broker.getUsername(), "default broker", "correct name");
      Object retrieved = dor.getById(1);
      assertEquals(result, retrieved, "retrieved successfully");
    }
    catch (MissingDomainObject mdo) {
      fail("bad exception " + mdo.toString());
    }
  }
  
  @Test
  public void readOutOfOrder ()
  {
    // entries appear out of order in log
    String soton = "169937:org.powertac.common.Broker::605::new::SotonPower";
    String hc1 = "177116:org.powertac.common.HourlyCharge::600000390::new::600000389::-0.0010::2009-01-03T02:00:00.000Z";
    String hc2 = "177116:org.powertac.common.HourlyCharge::600000392::new::600000389::-0.0010::2009-01-03T03:00:00.000Z";
    String r1 = "177118:org.powertac.common.Rate::600000389::new::600000388::-1::-1::-1::-1::0.0::false::0.0::0.0::1::0.0::0.0";
    String ts1 = "177120:org.powertac.common.TariffSpecification::600000388::new::605::CONSUMPTION::0::0::0.0::0.0::0.0::null";
    Object result = null;
    try {
      result = dor.readObject(soton);
      Broker broker = (Broker) result;
      result = dor.readObject(hc1);
      assertNotNull(result, "succeeded hc1");
      assertEquals(HourlyCharge.class, result.getClass(), "correct class");
      HourlyCharge charge1 = (HourlyCharge)result;
      assertEquals(600000389, charge1.getRateId(), "correct Rate hc1");
      result = dor.readObject(hc2);
      assertNotNull(result, "succeeded hc2");
      assertEquals(HourlyCharge.class, result.getClass(), "correct class");
      HourlyCharge charge2 = (HourlyCharge)result;
      assertEquals(600000389, charge2.getRateId(), "correct Rate hc2");

      result = dor.readObject(r1);
      Rate rate = (Rate)result;
      assertEquals(600000389, rate.getId(), "correct Id");
      assertEquals(600000388, rate.getTariffId(), "correct spec Id");

      result = dor.readObject(ts1);
      TariffSpecification spec = (TariffSpecification)result;
      assertEquals(600000388, spec.getId(), "correct Id");
      assertEquals(broker, spec.getBroker(), "correct broker");
    }
    catch (MissingDomainObject mdo) {
      fail("should succeed: " + mdo.toString());
    }
  }
  
  @Test
  public void readBalancingOrder ()
  {
    String ca = "125552:org.powertac.common.Broker::601::new::CrocodileAgent";
    String r = "177360:org.powertac.common.Rate::200076920::new::200076919::-1::-1::-1::-1::0.0::true::-0.045598969348039364::0.0::0::0.0::0.1";
    String ts = "177360:org.powertac.common.TariffSpecification::200076919::new::601::INTERRUPTIBLE_CONSUMPTION::0::0::0.0::0.0::-0.6::null";
    String bo1 = "237911:org.powertac.common.msg.BalancingOrder::200077175::new::0.5::-0.04103907241323543::200076919::601";
    try {
      Broker crocodile = (Broker)dor.readObject(ca);
      Rate rate = (Rate)dor.readObject(r);
      TariffSpecification spec = (TariffSpecification)dor.readObject(ts);
      spec.addRate(rate);
      Object result = dor.readObject(bo1);
      assertNotNull(result, "should read correctly");
      BalancingOrder order = (BalancingOrder)result;
      assertEquals(0.5, order.getExerciseRatio(), 1e-6, "correct ratio");
      assertEquals(-0.04103907241323543, order.getPrice(), 1e-6, "correct price");
      assertEquals(crocodile, order.getBroker(), "correct broker");
      assertEquals(200076919, order.getTariffId(), "correct tariff");
    }
    catch (MissingDomainObject mdo) {
      fail("should not fail: " + mdo.toString());
    }
  }
  
  @Test
  public void simpleMethodCall ()
  {
    String nc = "140:org.powertac.common.Competition::0::new::game-9";
    String m1 = "221:org.powertac.common.Competition::0::withSimulationBaseTime::1229644800000";
    String m2 = "222:org.powertac.common.Competition::0::withMinimumTimeslotCount::1380";
    String m3 = "222:org.powertac.common.Competition::0::withExpectedTimeslotCount::1440";
    try {
      Competition comp = (Competition)dor.readObject(nc);
      assertNotNull(comp, "valid Competition");
      dor.readObject(m1);
      assertEquals(1229644800000l, comp.getSimulationBaseTime().toEpochMilli(), "correct base time");
      dor.readObject(m2);
      assertEquals(1380, comp.getMinimumTimeslotCount(), "correct min ts count");
      dor.readObject(m3);
      assertEquals(1440, comp.getExpectedTimeslotCount(), "correct exp ts count");
    }
    catch (MissingDomainObject mdo) {
      fail("should not happen: " + mdo.toString());
    }
  }
  
  @Test
  public void readSubscribe ()
  {
    String cu = "812:org.powertac.common.CustomerInfo::1895::new::Village::10";
    String ca = "1255:org.powertac.common.Broker::601::new::CrocodileAgent";
    String r = "1773:org.powertac.common.Rate::200076920::new::1878::-1::-1::-1::-1::0.0::true::-0.045598969348039364::0.0::0::0.0::0.1";
    String ts = "1774:org.powertac.common.TariffSpecification::1878::new::601::INTERRUPTIBLE_CONSUMPTION::0::0::0.0::0.0::-0.6::null";
    String sub = "9204:org.powertac.common.TariffSubscription::2588::new::1895::1878";
    String add = "9204:org.powertac.common.TariffSubscription::2588::subscribe::10";
    try {
      CustomerInfo customer = (CustomerInfo) dor.readObject(cu);
      assertNotNull(customer, "valid customer");
      Broker crocodile = (Broker) dor.readObject(ca);
      assertNotNull(crocodile, "valid broker");
      Rate rate = (Rate) dor.readObject(r);
      assertNotNull(rate, "valid rate");
      TariffSpecification spec = (TariffSpecification) dor.readObject(ts);
      spec.addRate(rate);
      assertNotNull(spec, "valid tariff spec");
      assertEquals(rate, spec.getRates().get(0), "spec contains rate");
      TariffSubscription tsub = (TariffSubscription) dor.readObject(sub);
      assertNotNull(tsub, "valid subscription");
    }
    catch (MissingDomainObject mdo) {
      fail("missing DO " + mdo.toString());
    }
  }

  @Test
  public void readTtxPublish () {
    String ca = "255:org.powertac.common.Broker::601::new::CrocodileAgent";
    String r = "773:org.powertac.common.Rate::300076920::new::300076919::-1::-1::-1::-1::0.0::true::-0.045598969348039364::0.0::0::0.0::0.1";
    String ts = "774:org.powertac.common.TariffSpecification::300076919::new::601::INTERRUPTIBLE_CONSUMPTION::0::0::0.0::0.0::-0.6::null";
    String tx = "906:org.powertac.common.TariffTransaction::7471::new::601::360::PUBLISH::300076919::null::0::0.0::-2679.645482627618::false";
    try {
      Broker crocodile = (Broker) dor.readObject(ca);
      Rate r1 = (Rate) dor.readObject(r);
      TariffSpecification ts1 =
              (TariffSpecification) dor.readObject(ts);
      assertNotNull(crocodile, "valid broker");
      assertNotNull(ts1, "created a tariff spec");
      assertNull(ts1.getExpiration(), "no expiration time");
      TariffTransaction ttx = (TariffTransaction) dor.readObject(tx);
      assertNotNull(ttx, "created a transaction");
      assertEquals(crocodile, ttx.getBroker());
      assertEquals(ts1, ttx.getTariffSpec());
    }
    catch (MissingDomainObject mdo) {
      fail("missing DO "+ mdo.toString());
    }
  }
}
