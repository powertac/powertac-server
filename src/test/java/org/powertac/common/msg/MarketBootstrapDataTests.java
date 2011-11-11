package org.powertac.common.msg;

import static org.junit.Assert.*;

import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

public class MarketBootstrapDataTests
{

  private double[] mwhs;
  private double[] prices;
  
  @Before
  public void setUp () throws Exception
  {
    mwhs = new double[] {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,1.1};
    prices = new double[] {2.0,2.2,2.1,2.4,2.3,2.6,2.5,2.8,2.7,3.0,2.9};
  }

  @Test
  public void testMarketBootstrapData ()
  {
    MarketBootstrapData mbd = new MarketBootstrapData(mwhs, prices);
    assertNotNull("object created", mbd);
    assertEquals("correct mwh array size", 11, mbd.getMwh().length);
    assertEquals("correct price array size", 11, mbd.getMarketPrice().length);
    assertEquals("correct 3rd mwh", 0.3, mbd.getMwh()[2], 1e-6);
    assertEquals("correct 5th price", 2.3, mbd.getMarketPrice()[4], 1e-6);
  }
  
  @Test
  public void xmlSerializationTest ()
  {
    MarketBootstrapData mbd = new MarketBootstrapData(mwhs, prices);
    XStream xstream = new XStream();
    xstream.processAnnotations(MarketBootstrapData.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mbd));
    //System.out.println(serialized.toString());
    MarketBootstrapData xmbd = 
      (MarketBootstrapData)xstream.fromXML(serialized.toString());
    assertNotNull("deserialized something", xmbd);
    assertEquals("correct id", mbd.getId(), xmbd.getId());
    assertEquals("correct 5th mwh", 0.5, xmbd.getMwh()[4], 1e-6);
    assertEquals("correct 3rd price", 2.1, xmbd.getMarketPrice()[2], 1e-6);
  }
}
