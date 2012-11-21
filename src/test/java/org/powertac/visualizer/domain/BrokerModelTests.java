package org.powertac.visualizer.domain;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.CustomerInfo;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.CustomerModel;
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
	public void setUp() throws Exception {
		brokerModel = new BrokerModel("testBroker",
				new Appearance("163eef", "/app/resources/images/brokers/163eef.png"));

	}

	@Test
	public void testJSON() throws JSONException {
		double balance = 10.5;
		int currentTimeslotIndex = 2;

		assertEquals("Empty JSON array", "[]", brokerModel.getJson().getCashBalanceJson().toString());

		JSONArray entry = new JSONArray();
		entry.put(currentTimeslotIndex).put(balance);
		assertEquals("[2,10.5]", entry.toString());

		brokerModel.getJson().getCashBalanceJson().put(currentTimeslotIndex, entry);
		brokerModel.getJson().getCashBalanceJson().put(currentTimeslotIndex, entry);

		assertEquals("One array", "[null,null,[2,10.5]]", brokerModel.getJson().getCashBalanceJson().toString());

		JSONArray arrayOfArrays = new JSONArray();

		JSONArray firstArray = new JSONArray();
		firstArray.put(0, 1);
		firstArray.put(1, 2);

		JSONArray secondArray = new JSONArray();
		secondArray.put(0, 3);
		secondArray.put(1, 4);

		arrayOfArrays.put(0, firstArray);
		arrayOfArrays.put(1, secondArray);

		assertEquals("Arrays wtihin an array", "[[1,2],[3,4]]", arrayOfArrays.toString());

		ArrayList<Double> cashBalances = new ArrayList<Double>(24);
		cashBalances.add((double) 46);

		assertEquals("Size of an Array<Double> with one element:", 1, cashBalances.size());
	}


}
