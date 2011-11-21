package org.powertac.server;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BootstrapDataCollector;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/test/resources/cc-config.xml"})
@DirtiesContext
public class CompetitionControlServiceTests
{

  @Autowired
  private CompetitionControlService ccs;
  
  @Autowired
  private PluginConfigRepo pluginConfigRepo;
  
  @Autowired
  private BootstrapDataCollector collector;
  
  private CustomerInfo customer1;
  private CustomerInfo customer2;
  
  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/logger.config");
    Logger.getRootLogger().setLevel(Level.DEBUG);
  }

  @Before
  public void setUp () throws Exception
  {
    reset(collector);
    customer1 = new CustomerInfo("Jack", 3);
    customer2 = new CustomerInfo("Jill", 7);
  }

  @Test
  public void testPreGame ()
  {
    //fail("Not yet implemented");
  }

  @Test
  public void testPreGameFileReader ()
  {
    //fail("Not yet implemented");
  }

  @Test
  public void testRunOnceWriter ()
  {
    // create the PluginConfig instances
    ccs.preGame();
    
    ArrayList<Object> data = new ArrayList<Object>();
    double[] usage1 = new double[] {3.1,3.2,3.3,3.4};
    data.add(new CustomerBootstrapData(customer1, PowerType.CONSUMPTION, usage1));
    double[] usage2 = new double[] {7.2,7.3,7.4,7.5};
    data.add(new CustomerBootstrapData(customer2, PowerType.CONSUMPTION, usage2));
    
    when(collector.collectBootstrapData(anyInt())).thenReturn(data);
    
    CharArrayWriter writer = new CharArrayWriter();
    ccs.saveBootstrapData(writer);
    //System.out.println(writer.toString());
    
    // transfer the data to a reader and check content with XPath
    CharArrayReader reader = new CharArrayReader(writer.toCharArray());
    InputSource source = new InputSource(reader);
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      XPathExpression exp = 
        xPath.compile("/powertac-bootstrap-data/config/plugin-config[@roleName='weatherService']/configuration/entry/string");
      NodeList nodes = (NodeList)exp.evaluate(source, XPathConstants.NODESET);
      assertEquals("two entries", 8, nodes.getLength());
      assertEquals("node 2", "forecastHorizon", nodes.item(2).getTextContent());
      String num = nodes.item(3).getTextContent();
      int hours= Integer.parseInt(num);
      assertEquals("24 hour", 24, hours);
    }
    catch (XPathExpressionException xee) {
      fail("XPath trouble: " + xee.toString());
    }
    //System.out.println(writer.toString());
  }

  @Test
  public void testRegisterTimeslotPhase ()
  {
    //fail("Not yet implemented");
  }

}
