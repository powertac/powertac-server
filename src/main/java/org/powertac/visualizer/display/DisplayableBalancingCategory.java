package org.powertac.visualizer.display;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.powertac.visualizer.statistical.BalancingCategory;
import org.powertac.visualizer.statistical.BalancingData;
import org.primefaces.json.JSONArray;

/**
 * Displayable balancing category for a broker, used by a front-end.
 * @author Jurica Babic
 *
 */
public class DisplayableBalancingCategory extends AbstractDisplayablePerformanceCategory implements Serializable {
	
	
	private static final long serialVersionUID = 1L;
	private double totalKwh;
	private double totalMoney;
	private JSONArray kWhImbalanceJson = new JSONArray();
	private JSONArray priceImbalanceJson = new JSONArray();
	private JSONArray unitPriceImbalanceJson = new JSONArray();
	
	public DisplayableBalancingCategory(BalancingCategory balancingCategory) {
		super(balancingCategory.getGrade());
		
		this.totalKwh = balancingCategory.getAggregateBalancingData().getTotalKwh();
		this.totalMoney = balancingCategory.getAggregateBalancingData().getTotalMoney();
		
		Set<Entry<Double,BalancingData>> balancingSet = balancingCategory.getBalancingDataMap().entrySet();
		 Iterator<Entry<Double, BalancingData>> iterator = balancingSet.iterator();
		    while(iterator.hasNext()) {
		        Entry<Double, BalancingData> setElement = iterator.next();
		        kWhImbalanceJson.put("["+setElement.getKey()+","+setElement.getValue().getkWhImbalance()+"]");
		        priceImbalanceJson.put("["+setElement.getKey()+","+setElement.getValue().getPriceImbalance()+"]");
		        unitPriceImbalanceJson.put("["+setElement.getKey()+","+setElement.getValue().getUnitPrice()+"]");
		    }
	}
	
	public double getTotalKwh() {
		return totalKwh;
	}
	public double getTotalMoney() {
		return totalMoney;
	}
	
	public JSONArray getkWhImbalanceJson() {
		return kWhImbalanceJson;
	}
	
	public JSONArray getPriceImbalanceJson() {
		return priceImbalanceJson;
	}
	
	public JSONArray getUnitPriceImbalanceJson() {
		return unitPriceImbalanceJson;
	}
	

}
