package org.powertac.common.msg;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import com.thoughtworks.xstream.XStream;
import org.powertac.common.XMLMessageConverter;

public class MarketBootstrapDataTests
{

  private double[] mwhs;
  private double[] prices;
  
  @BeforeEach
  public void setUp () throws Exception
  {
    mwhs = new double[] {0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0,1.1};
    prices = new double[] {2.0,2.2,2.1,2.4,2.3,2.6,2.5,2.8,2.7,3.0,2.9};
  }

  @Test
  public void testMarketBootstrapData ()
  {
    MarketBootstrapData mbd = new MarketBootstrapData(mwhs, prices);
    assertNotNull(mbd, "object created");
    assertEquals(11, mbd.getMwh().length, "correct mwh array size");
    assertEquals(11, mbd.getMarketPrice().length, "correct price array size");
    assertEquals(0.3, mbd.getMwh()[2], 1e-6, "correct 3rd mwh");
    assertEquals(2.3, mbd.getMarketPrice()[4], 1e-6, "correct 5th price");
    assertEquals(2.659091, mbd.getMeanMarketPrice(), 1e-6, "correct mean price");
  }
  
  @Test
  public void xmlSerializationTest ()
  {
    MarketBootstrapData mbd = new MarketBootstrapData(mwhs, prices);
    XStream xstream = XMLMessageConverter.getXStream();
    xstream.processAnnotations(MarketBootstrapData.class);
    StringWriter serialized = new StringWriter();
    serialized.write(xstream.toXML(mbd));
    //System.out.println(serialized.toString());
    MarketBootstrapData xmbd = 
      (MarketBootstrapData)xstream.fromXML(serialized.toString());
    assertNotNull(xmbd, "deserialized something");
    assertEquals(mbd.getId(), xmbd.getId(), "correct id");
    assertEquals(0.5, xmbd.getMwh()[4], 1e-6, "correct 5th mwh");
    assertEquals(2.1, xmbd.getMarketPrice()[2], 1e-6, "correct 3rd price");
  }
}
