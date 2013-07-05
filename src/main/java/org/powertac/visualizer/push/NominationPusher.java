package org.powertac.visualizer.push;

public class NominationPusher {

	private NominationCategoryPusher profit;
	private NominationCategoryPusher balance;
	private NominationCategoryPusher customerNumber;

	
	public NominationPusher() {
		// TODO Auto-generated constructor stub
	}

	public NominationPusher(NominationCategoryPusher profit,
			NominationCategoryPusher balance,
			NominationCategoryPusher customerNumber) {
		this.profit=profit;
		this.balance=balance;
		this.customerNumber=customerNumber;

	}
	public NominationCategoryPusher getBalance() {
		return balance;
	}
	public NominationCategoryPusher getCustomerNumber() {
		return customerNumber;
	}
	public NominationCategoryPusher getProfit() {
		return profit;
	}
	public void setBalance(NominationCategoryPusher balance) {
		this.balance = balance;
	}
	public void setCustomerNumber(NominationCategoryPusher customerNumber) {
		this.customerNumber = customerNumber;
	}
	public void setProfit(NominationCategoryPusher profit) {
		this.profit = profit;
	}

}
