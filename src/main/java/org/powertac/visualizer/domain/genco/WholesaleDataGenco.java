package org.powertac.visualizer.domain.genco;

import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.common.Timeslot;

import java.util.ArrayList;

/**
 * Contains data about broker's wholesale performance for one timeslot. Firstly
 * introduced in the Genco model.
 * 
 * @author Jurica Babic
 * 
 */
public class WholesaleDataGenco {
	private Timeslot timeslot;
	private ArrayList<MarketPosition> marketPositions;
	private ArrayList<MarketTransaction> marketTransactions;

	private double totalPrice;
	private double totalMWh;

	public WholesaleDataGenco(Timeslot timeslot) {
		this.timeslot = timeslot;
		marketPositions = new ArrayList<MarketPosition>();
		marketTransactions = new ArrayList<MarketTransaction>();
	}

	public void addMarketPosition(MarketPosition position) {
		marketPositions.add(position);
	}

	public void addMarketTransaction(MarketTransaction transaction) {
		marketTransactions.add(transaction);
		
		totalPrice += transaction.getPrice();
		totalMWh += transaction.getMWh();
	}

	public Timeslot getTimeslot() {
		return timeslot;
	}
	
	public double getTotalMWh() {
		return totalMWh;
	}
	
	public double getTotalPrice() {
		return totalPrice;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<MarketPosition> getMarketPositions() {
		return (ArrayList<MarketPosition>) marketPositions.clone();
	}

	@SuppressWarnings("unchecked")
	public ArrayList<MarketTransaction> getMarketTransactions() {
		return (ArrayList<MarketTransaction>) marketTransactions.clone();
	}

}
