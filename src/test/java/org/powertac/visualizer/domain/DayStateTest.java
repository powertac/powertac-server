package org.powertac.visualizer.domain;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class DayStateTest {

	private int dayId = 5;
	private DayState dayState;

	@Before
	public void setUp() {
		dayState = new DayState(dayId,null);

		for (int i = 0; i < 24; i++) {
			dayState.addTimeslotValues(i, i, i);
		}

	}

	@Test
	public void testAvgCashBalance() {
		assertEquals("AVG cash balance:", "" + 11.5, "" + dayState.getAvgCashBalance());

	}
}
