package org.powertac.visualizer.domain.wholesale;

import static org.junit.Assert.*;

import java.sql.Time;
import java.util.Iterator;
import java.util.List;

import org.joda.time.Instant;
import org.joda.time.field.OffsetDateTimeField;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Orderbook;
import org.powertac.common.Timeslot;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.primefaces.model.TreeNode;

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
