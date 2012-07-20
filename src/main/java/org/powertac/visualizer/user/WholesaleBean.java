package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.powertac.common.OrderbookOrder;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.wholesale.VisualizerOrderbookOrder;
import org.powertac.visualizer.domain.wholesale.WholesaleMarket;
import org.powertac.visualizer.domain.wholesale.WholesaleSnapshot;
import org.powertac.visualizer.services.WholesaleService;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.paginator.PaginatorElementRenderer;
import org.primefaces.model.TreeNode;
import org.springframework.beans.factory.annotation.Autowired;

public class WholesaleBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private WholesaleSnapshot selectedSnapshot;
	private WholesaleMarket selectedMarket;
	private double totalTradedQuantityMWh;
	private WholesaleService model;
	private Collection<WholesaleMarket> markets;

	private List<VisualizerOrderbookOrder> beforeAsks;
	private List<VisualizerOrderbookOrder> afterAsks;

	@Autowired
	public WholesaleBean(WholesaleService wholesaleService) {
		model = wholesaleService;
		refresh();
	}

	private void refresh() {
		markets = new ArrayList<WholesaleMarket>(model.getWholesaleMarkets().values());
		totalTradedQuantityMWh = model.getTotalTradedQuantityMWh();
			}

	public WholesaleSnapshot getSelectedSnapshot() {
		return selectedSnapshot;
	}

	public void setSelectedSnapshot(WholesaleSnapshot selectedSnapshot) {
		this.selectedSnapshot = selectedSnapshot;
		beforeAsks = selectedSnapshot.getBeforeAsks();
		afterAsks = selectedSnapshot.getAfterAsks();
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

	public WholesaleService getModel() {
		return model;
	}

	public Collection<WholesaleMarket> getMarkets() {
		refresh();
		return markets;
	}
	public List<VisualizerOrderbookOrder> getAfterAsks() {
		return afterAsks;
	}
	public List<VisualizerOrderbookOrder> getBeforeAsks() {
		return beforeAsks;
	}
	
	public void clearSelections(){
		selectedSnapshot = null;
		selectedMarket = null;
	}

}
