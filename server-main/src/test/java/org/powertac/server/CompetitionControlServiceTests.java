package org.powertac.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.CustomerInfo;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.BootstrapDataCollector;
import org.powertac.common.msg.CustomerBootstrapData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@SpringJUnitConfig(locations = {"classpath:cc-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class CompetitionControlServiceTests
{

  @Autowired
  private CompetitionSetupService css;
  
  @Autowired
  private BootstrapDataCollector collector;
  
  private CustomerInfo customer1;
  private CustomerInfo customer2;

  @BeforeEach
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
      assertEquals(2, nodes.getLength(), "two entries");
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
    assertEquals(val, dt.toInstant(), "correct time translation");
  }

  @Test
  public void setAuthorizedBrokers ()
  {
    CompetitionControlService ccs = new CompetitionControlService();
    List<String> aaNames = Arrays.asList("default broker");
    ccs.setAlwaysAuthorizedBrokers(aaNames);
    List<String> usernames = Arrays.asList("Sally", "Jenny");
    ccs.setAuthorizedBrokerList(usernames);
    List<String> names = ccs.getBrokerNames();
    assertEquals(3, names.size(), "3 names");
    assertEquals(0, names.indexOf("default broker"), "defaut first");
    assertEquals(1, names.indexOf("Sally"), "Sally second");
    assertEquals(2, names.indexOf("Jenny"), "Jenny third");
  }

  @Test
  public void setAuthorizedBrokersWithQueues ()
  {
    CompetitionControlService ccs = new CompetitionControlService();
    List<String> aaNames = Arrays.asList("buyer", "genco");
    ccs.setAlwaysAuthorizedBrokers(aaNames);
    List<String> usernames = Arrays.asList("Sally/S1", "Jenny/J1");
    ccs.setAuthorizedBrokerList(usernames);
    List<String> names = ccs.getBrokerNames();
    assertEquals(4, names.size(), "4 names");
    assertEquals(2, names.indexOf("Sally"), "Sally first");
    assertEquals(3, names.indexOf("Jenny"), "Jenny second");
  }
}
