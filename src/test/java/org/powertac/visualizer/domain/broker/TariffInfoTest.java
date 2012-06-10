package org.powertac.visualizer.domain.broker;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Broker;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TariffTransaction.Type;
import org.powertac.common.enumerations.PowerType;

public class TariffInfoTest {

	private TariffInfo tariffInfo;
	private TariffSpecification tariffSpecification;
	private Broker broker = new Broker("Djuro");

	@Before
	public void setUp() throws Exception {
		tariffSpecification = new TariffSpecification(broker, PowerType.CONSUMPTION);
	}

	@Test
	public void testSpecWithThreeRates() {

		tariffSpecification.addRate(new Rate().withValue(1).withMaxValue(1.5));
		tariffSpecification.addRate(new Rate().withValue(2).withMaxValue(2.5).withWeeklyBegin(6).withWeeklyEnd(7));
		tariffSpecification.addRate(new Rate().withValue(3).withMaxValue(3.5).withWeeklyBegin(5).withWeeklyEnd(4));
		tariffInfo = new TariffInfo(tariffSpecification);

		System.out.println("\nTest spec with three rates, max value chart:");
		System.out.println(tariffInfo.getJson().getRatesLineChartMaxValue());
		assertEquals(3, tariffInfo.getJson().getRatesLineChartMaxValue().length());
		System.out.println("\nTest spec with three rates, min value chart:");
		System.out.println(tariffInfo.getJson().getRatesLineChartMinValue());

	}

	@Test
	public void testGraphs() {
		tariffInfo = new TariffInfo(tariffSpecification);

		TariffTransaction transaction = new TariffTransaction(broker, null, Type.SIGNUP, tariffSpecification,
				new CustomerInfo("Purgeri", 200), 35, 0, 100);
		tariffInfo.addTariffTransaction(transaction);
		tariffInfo.update(0);
		System.out.println(tariffInfo.getJson());

		transaction = new TariffTransaction(broker, null, Type.CONSUME, tariffSpecification, new CustomerInfo(
				"Srakari", 200), 10, -100, 15);
		tariffInfo.addTariffTransaction(transaction);
		tariffInfo.update(1);
		System.out.println(tariffInfo.getJson());

		transaction = new TariffTransaction(broker, null, Type.WITHDRAW, tariffSpecification, new CustomerInfo(
				"Purgeri", 200), 35, 0, 100);
		tariffInfo.addTariffTransaction(transaction);
		tariffInfo.update(2);
		System.out.println(tariffInfo.getJson());

		System.out.println();
	}

	@Test
	public void testTariffLifeCycle() {
		tariffInfo = new TariffInfo(tariffSpecification);

		TariffTransaction transaction = new TariffTransaction(broker, null, Type.SIGNUP, tariffSpecification,
				new CustomerInfo("Purgeri", 200), 35, 0, 100);
		tariffInfo.addTariffTransaction(transaction);
		tariffInfo.addTariffMessage("Tariff updated");
		
		ArrayList<String> messages = tariffInfo.getTariffLifecycle();
		
		for (Iterator iterator = messages.iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			System.out.println(string);
		}
	}

}
