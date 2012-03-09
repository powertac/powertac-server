package org.powertac.visualizer.wholesale;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.ClearedTrade;
import org.powertac.common.Timeslot;
import org.primefaces.model.TreeNode;

public class WholesaleMarketTest {

	private WholesaleMarket market;
	private Integer timeslotSerialNumber = 0;

	@Before
	public void setUp() throws Exception {
		market = new WholesaleMarket(timeslotSerialNumber);
		
		Timeslot timeslot;
		WholesaleSnapshot snapshot;
		
		
		for(int i=0; i<5; i++){
			timeslot = new Timeslot(360+i, null, null);
			snapshot = new WholesaleSnapshot(timeslot, 0);
			snapshot.setClearedTrade(new ClearedTrade(timeslot,10,i,null));
			snapshot.close();
			market.getSnapshots().put(i, snapshot);
		}
		
	}
	@Test
	public void test() {
		
		market.close();
		
		
		List<TreeNode> nodes = market.getMarketNode().getChildren();
		
		for (Iterator iterator = nodes.iterator(); iterator.hasNext();) {
			TreeNode treeNode = (TreeNode) iterator.next();
			System.out.println("Treenode: "+treeNode.getData());
		}
		
		System.out.println(market.toString());
		
	}

}
