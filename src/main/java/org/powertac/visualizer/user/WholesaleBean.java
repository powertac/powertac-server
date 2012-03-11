package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.wholesale.WholesaleMarket;
import org.powertac.visualizer.wholesale.WholesaleModel;
import org.powertac.visualizer.wholesale.WholesaleSnapshot;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;

public class WholesaleBean implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private WholesaleSnapshot selectedSnapshot;
	private WholesaleMarket selectedMarket;
	private double totalTradedQuantityMWh;
	private WholesaleModel model;
	private Collection<WholesaleMarket> markets;
	
	public WholesaleBean(VisualizerBean visualizerBean) {
		model = visualizerBean.getWholesaleModel();
		markets  = new ArrayList<WholesaleMarket>(model.getWholesaleMarkets().values());
		totalTradedQuantityMWh = model.getTotalTradedQuantityMWh();
	}

	public WholesaleSnapshot getSelectedSnapshot() {
		return selectedSnapshot;
	}

	public void setSelectedSnapshot(WholesaleSnapshot selectedSnapshot) {
		this.selectedSnapshot = selectedSnapshot;
	}

	public WholesaleMarket getSelectedMarket() {
		return selectedMarket;
	}

	public void setSelectedMarket(WholesaleMarket selectedMarket) {
		this.selectedMarket = selectedMarket;
	}

	public double getTotalTradedQuantityMWh() {
		return totalTradedQuantityMWh;
	}

	public WholesaleModel getModel() {
		return model;
	}

	public Collection<WholesaleMarket> getMarkets() {
		return markets;
	}
	
	
	
}
