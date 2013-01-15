package org.powertac.visualizer.statistical;


/**
 * Balancing data for one timeslot from broker's perspective.
 * @author Jurica Babic
 *
 */
public class BalancingData {
	
	private double kWhImbalance;
	private double priceImbalance;
	private double unitPrice;
	private double timestamp;
	
	public BalancingData(double kWhImbalance, double priceImbalance, double timestamp) {
		this.kWhImbalance = kWhImbalance;
		this.priceImbalance = priceImbalance;
		this.timestamp = timestamp;
		calculateUnitPrice();
	}
	
	public double getkWhImbalance() {
		return kWhImbalance;
	}
	public double getPriceImbalance() {
		return priceImbalance;
	}
	public double getUnitPrice() {
		return unitPrice;
	}
	
	public double getTimestamp() {
		return timestamp;
	}
	
	private void calculateUnitPrice(){
		if(kWhImbalance!=0){
			unitPrice = priceImbalance/kWhImbalance;
		}
	}
	
	
}
