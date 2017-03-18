package org.powertac.visualizer.push;


public class NominationCategoryPusher {

	private String name;
	private long amount;

	public NominationCategoryPusher(String name, long amount) {
		this.name=name;
		this.amount=amount;
	}

	public long getAmount() {
		return amount;
	}
	public String getName() {
		return name;
	}
	
	

}
