package org.powertac.logtool.common;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.DistributionTransaction;
import org.powertac.common.HourlyCharge;
import org.powertac.common.Order;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.msg.BalancingOrder;

public class DomainObjectReaderTest
{
  private DomainObjectReader dor;
  
  @Before
  public void setUp () throws Exception
  {
    dor = new DomainObjectReader();
  }

  @Test
  public void testReadSingleObject ()
  {
    String aston = "144669:org.powertac.common.Broker::603::new::AstonTAC";
    try {
      Object result = dor.readObject(aston);
      assertNotNull("created an instance", result);
      assertEquals("correct class", "org.powertac.common.Broker", result.getClass().getName());
      assertEquals("correct id", 603, ((Broker)result).getId());
      assertEquals("object stored in map", result, dor.getById(603));
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
      assertEquals("correct class", "org.powertac.common.DistributionTransaction", result.getClass().getName());
      DistributionTransaction dtx = (DistributionTransaction)result;
      assertEquals("correct id", 3459, dtx.getId());
      assertEquals("object stored in map", result, dor.getById(3459));
      assertEquals("broker stored", broker, dtx.getBroker());
      assertEquals("broker stored in map", broker, dor.getById(603));
      assertEquals("tx stored in map", dtx, dor.getById(3459));
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
      //assertNotNull("timeslot created", timeslot);
      Object result = dor.readObject(order);
      assertNotNull("order created", result);
      assertEquals("correct class", "org.powertac.common.Order", result.getClass().getName());
      Order o = (Order)result;
      assertEquals("correct id", 400000393, o.getId());
      assertEquals("order in map", o, dor.getById(400000393));
      assertEquals("correct broker", broker, o.getBroker());
      assertEquals("correct mwh", 2.109375, o.getMWh(), 1e-6);
      assertEquals("correct price", -31.835472671068615, o.getLimitPrice(), 1e-6);
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
//      assertNotNull("read a forecast", result);
//      assertEquals("correct class", "org.powertac.common.WeatherForecast", result.getClass().getName());
//      WeatherForecast wf = (WeatherForecast)result;
//      List<WeatherForecastPrediction> predictions = wf.getPredictions();
//      assertEquals("correct number of predictions", 2, predictions.size());
//      assertEquals("correct first prediction", wfp1, predictions.get(0));
//      assertEquals("correct second prediction", wfp2, predictions.get(1));
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
      assertNotNull("non-null result", result);
      assertEquals("corret class", "org.powertac.du.DefaultBroker", result.getClass().getName());
      Broker broker = (Broker) result;
      assertEquals("correct name", "default broker", broker.getUsername());
      Object retrieved = dor.getById(1);
      assertEquals("retrieved successfully", result, retrieved);
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
    String ts1 = "177120:org.powertac.common.TariffSpecification::600000388::new::605::CONSUMPTION::0::0.0::0.0::0.0";
    Object result = null;
    try {
      result = dor.readObject(soton);
      Broker broker = (Broker) result;
      result = dor.readObject(hc1);
      assertNotNull("succeeded hc1", result);
      assertEquals("correct class", HourlyCharge.class, result.getClass());
      HourlyCharge charge1 = (HourlyCharge)result;
      assertEquals("correct Rate hc1", 600000389, charge1.getRateId());
      result = dor.readObject(hc2);
      assertNotNull("succeeded hc2", result);
      assertEquals("correct class", HourlyCharge.class, result.getClass());
      HourlyCharge charge2 = (HourlyCharge)result;
      assertEquals("correct Rate hc2", 600000389, charge2.getRateId());

      result = dor.readObject(r1);
      Rate rate = (Rate)result;
      assertEquals("correct Id", 600000389, rate.getId());
      assertEquals("correct spec Id", 600000388, rate.getTariffId());

      result = dor.readObject(ts1);
      TariffSpecification spec = (TariffSpecification)result;
      assertEquals("correct Id", 600000388, spec.getId());
      assertEquals("correct broker", broker, spec.getBroker());
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
    String ts = "177360:org.powertac.common.TariffSpecification::200076919::new::601::INTERRUPTIBLE_CONSUMPTION::0::0.0::0.0::-0.6";
    String bo1 = "237911:org.powertac.common.msg.BalancingOrder::200077175::new::0.5::-0.04103907241323543::200076919::601";
    try {
      Broker crocodile = (Broker)dor.readObject(ca);
      Rate rate = (Rate)dor.readObject(r);
      TariffSpecification spec = (TariffSpecification)dor.readObject(ts);
      spec.addRate(rate);
      Object result = dor.readObject(bo1);
      assertNotNull("should read correctly", result);
      BalancingOrder order = (BalancingOrder)result;
      assertEquals("correct ratio", 0.5, order.getExerciseRatio(), 1e-6);
      assertEquals("correct price", -0.04103907241323543, order.getPrice(), 1e-6);
      assertEquals("correct broker", crocodile, order.getBroker());
      assertEquals("correct tariff", 200076919, order.getTariffId());
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
      assertNotNull("valid Competition", comp);
      dor.readObject(m1);
      assertEquals("correct base time", 1229644800000l, comp.getSimulationBaseTime().getMillis());
      dor.readObject(m2);
      assertEquals("correct min ts count", 1380, comp.getMinimumTimeslotCount());
      dor.readObject(m3);
      assertEquals("correct exp ts count", 1440, comp.getExpectedTimeslotCount());
    }
    catch (MissingDomainObject mdo) {
      fail("should not happen: " + mdo.toString());
    }
  }
}
