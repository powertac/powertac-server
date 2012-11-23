package org.powertac.visualizer.display;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.powertac.visualizer.statistical.BalancingCategory;
import org.powertac.visualizer.statistical.BalancingData;

import com.google.gson.Gson;


/**
 * Displayable balancing category for a broker, used by a front-end.
 * @author Jurica Babic
 *
 */
public class DisplayableBalancingCategory extends AbstractDisplayablePerformanceCategory implements Serializable {
	
	
	private static final long serialVersionUID = 1L;
	private double totalKwh;
	private double totalMoney;
	private String kWhImbalanceJson;
	private String priceImbalanceJson;
	private String unitPriceImbalanceJson;
	
	public DisplayableBalancingCategory(BalancingCategory balancingCategory) {
		super(balancingCategory.getGrade());
		
		Gson gson = new Gson();
		
		this.totalKwh = balancingCategory.getAggregateBalancingData().getTotalKwh();
		this.totalMoney = balancingCategory.getAggregateBalancingData().getTotalMoney();
		
		ArrayList<Object> kWhImbalanceList = new ArrayList<Object>();
		ArrayList<Object> priceImbalanceList = new ArrayList<Object>();
		ArrayList<Object> unitPriceImbalanceList = new ArrayList<Object>();
		
		Set<Entry<Double,BalancingData>> balancingSet = balancingCategory.getBalancingDataMap().entrySet();
		 Iterator<Entry<Double, BalancingData>> iterator = balancingSet.iterator();
		    while(iterator.hasNext()) {
		        Entry<Double, BalancingData> setElement = iterator.next();
		        
		        double[] kWhImbalanceArray = {setElement.getKey(),setElement.getValue().getkWhImbalance()};
		        double[] priceImbalanceArray = {setElement.getKey(),setElement.getValue().getPriceImbalance()};
		        double[] unitPriceImbalanceArray = {setElement.getKey(),setElement.getValue().getUnitPrice()};
		        
		        kWhImbalanceList.add(kWhImbalanceArray);
		        priceImbalanceList.add(priceImbalanceArray);
		        unitPriceImbalanceList.add(unitPriceImbalanceArray);
		    }
		   kWhImbalanceJson = gson.toJson(kWhImbalanceList);
		   priceImbalanceJson = gson.toJson(priceImbalanceList);
		   unitPriceImbalanceJson = gson.toJson(unitPriceImbalanceList);
	}
	
	public double getTotalKwh() {
		return totalKwh;
	}
	public double getTotalMoney() {
		return totalMoney;
	}
	
	public String getkWhImbalanceJson() {
		return kWhImbalanceJson;
	}
	
	public String getPriceImbalanceJson() {
		return priceImbalanceJson;
	}
	
	public String getUnitPriceImbalanceJson() {
		return unitPriceImbalanceJson;
	}
	

}
