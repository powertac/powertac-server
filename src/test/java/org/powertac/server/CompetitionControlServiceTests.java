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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BootstrapDataCollector;
import org.powertac.common.msg.CustomerBootstrapData;
//import org.powertac.common.repo.PluginConfigRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:cc-config.xml"})
@DirtiesContext
public class CompetitionControlServiceTests
{

  @Autowired
  private CompetitionControlService ccs;
  
  @Autowired
  private CompetitionSetupService css;
  
  @Autowired
  private BootstrapDataCollector collector;
  
  private CustomerInfo customer1;
  private CustomerInfo customer2;
  
  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
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
    css.preGame();
    
    ArrayList<Object> data = new ArrayList<Object>();
    double[] usage1 = new double[] {3.1,3.2,3.3,3.4};
    data.add(new CustomerBootstrapData(customer1, PowerType.CONSUMPTION, usage1));
    double[] usage2 = new double[] {7.2,7.3,7.4,7.5};
    data.add(new CustomerBootstrapData(customer2, PowerType.CONSUMPTION, usage2));
    
    when(collector.collectBootstrapData(anyInt())).thenReturn(data);
    
    CharArrayWriter writer = new CharArrayWriter();
    css.saveBootstrapData(writer);
    //System.out.println(writer.toString());
    
    // transfer the data to a reader and check content with XPath
    CharArrayReader reader = new CharArrayReader(writer.toCharArray());
    InputSource source = new InputSource(reader);
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      XPathExpression exp = 
        xPath.compile("/powertac-bootstrap-data/bootstrap/customer-bootstrap-data/netUsage");
      NodeList nodes = (NodeList)exp.evaluate(source, XPathConstants.NODESET);
      assertEquals("two entries", 2, nodes.getLength());
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

  @Test
  public void testConfig ()
  {
    DateTimeZone.setDefault(DateTimeZone.UTC);
    Instant val = new DateTime(2011, 1, 26, 0, 0, 0, 0).toInstant();
    String dateString = "2011-1-26";
    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd");
    DateTime dt = fmt.parseDateTime(dateString);
    assertEquals("correct time translation", val, dt.toInstant());
  }
}
