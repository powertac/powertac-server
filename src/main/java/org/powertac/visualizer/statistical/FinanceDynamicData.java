package org.powertac.visualizer.statistical;

/**
 * Tracks the finance data for one timeslot of a single broker. 
 * @author Jurica Babic
 *
 */
public class FinanceDynamicData {
	private double startingBalance;
	private double balanceDelta;
	private double balance;
	private int tsIndex;
	
	public FinanceDynamicData(double startingBalance, int tsIndex) {
		this.startingBalance = startingBalance;
		this.tsIndex = tsIndex;		
	}
	
	public void updateBalance(double balance){
		balanceDelta = balance - startingBalance;
		this.balance = balance;
	}
	
	public double getBalance() {
		return balance;
	}
	/**
	 * @return balance for one timeslot
	 */
	public double getBalanceDelta() {
		return balanceDelta;
	}
	public int getTsIndex() {
		return tsIndex;
	}
}
