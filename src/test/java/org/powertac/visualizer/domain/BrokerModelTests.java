package org.powertac.visualizer.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = { "classpath:test-config.xml" })
//@DirtiesContext
public class BrokerModelTests {
	
	 BrokerModel brokerModel;

	 @Before
	  public void setUp () throws Exception
	  {
		 brokerModel=new BrokerModel("testBroker", new Appearance("163eef", "/app/resources/images/brokers/163eef.png"));
		 
	  }
	 
	 @Test
	  public void testJSON () throws JSONException
	  {
		 double balance=10.5;
		 int currentTimeslotIndex = 2;
		 
		 assertEquals("Empty JSON array","[]",brokerModel.getCashBalanceJson().toString());
		 
		 JSONArray entry = new JSONArray();
		 entry.put(currentTimeslotIndex).put(balance);
		 assertEquals("[2,10.5]", entry.toString());
		 
		 brokerModel.getCashBalanceJson().put(currentTimeslotIndex,entry);
		 brokerModel.getCashBalanceJson().put(currentTimeslotIndex,entry);
		 
		 assertEquals("One array","[null,null,[2,10.5]]",brokerModel.getCashBalanceJson().toString());
		 
		 
	  }
	
}
