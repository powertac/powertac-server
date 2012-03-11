package org.powertac.visualizer.wholesale;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Order;
import org.powertac.common.Timeslot;

public class WholesaleSnapshotTest {
	
	private WholesaleSnapshot wholesaleSnapshot;
	private Timeslot timeslot = new Timeslot(0, null, null);

	@Before
	public void setUp() throws Exception {
		wholesaleSnapshot = new WholesaleSnapshot(timeslot, 0,360 + 0);
		
	
	}

	@Test
	public void test() {
		
		wholesaleSnapshot.addOrder(new Order(null,timeslot, -1.0, null));
		wholesaleSnapshot.addOrder(new Order(null, timeslot, -2.0, 2.0));
		wholesaleSnapshot.addOrder(new Order(null, timeslot, -3.0, 3.0));
		
		wholesaleSnapshot.addOrder(new Order(null, timeslot, 4.0, -4.0));
		wholesaleSnapshot.addOrder(new Order(null, timeslot, 5.0, null));
		wholesaleSnapshot.addOrder(new Order(null, timeslot, 6.0, -6.0));
		
		
		wholesaleSnapshot.close();
		 
		
	}

}
