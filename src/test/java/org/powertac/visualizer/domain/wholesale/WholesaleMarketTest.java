package org.powertac.visualizer.domain.wholesale;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Timeslot;


public class WholesaleMarketTest {

	private WholesaleMarket market;
	private Integer timeslotSerialNumber = 0;

	@Before
	public void setUp() throws Exception {
		market = new WholesaleMarket(new Timeslot(timeslotSerialNumber, null, null), timeslotSerialNumber);

		Timeslot timeslot;
		WholesaleSnapshot snapshot;

		for (int i = 0; i < 5; i++) {
			timeslot = new Timeslot(360 + i, null, null);
			snapshot = new WholesaleSnapshot(1258, timeslot, 360 + i);
			snapshot.setClearedTrade(new ClearedTrade(timeslot, 10, i, null));
			snapshot.setOrderbook(new VisualizerOrderbook(timeslot, 0.0, new Instant()));
			snapshot.close();
			market.getSnapshotsMap().put(i, snapshot);
		}

	}

	@Test
	public void test() {

		market.close();

		System.out.println(market.toString());

	}

}
