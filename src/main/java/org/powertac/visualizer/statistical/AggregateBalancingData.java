package org.powertac.visualizer.statistical;

/**
 * It is used to contain the aggregate data of a broker.
 * 
 * @author Jurica Babic
 *
 */
public class AggregateBalancingData {
	private double totalKwh;
	private double totalMoney;
	
	public double getTotalKwh() {
		return totalKwh;
	}
	public void setTotalKwh(double totalKwh) {
		this.totalKwh = totalKwh;
	}
	public double getTotalMoney() {
		return totalMoney;
	}
	public void setTotalMoney(double totalMoney) {
		this.totalMoney = totalMoney;
	}
	public void processBalancingData(BalancingData lastBalancingData) {
		totalKwh+=lastBalancingData.getkWhImbalance();
		totalMoney+=lastBalancingData.getPriceImbalance();		
	}
	
	
}
