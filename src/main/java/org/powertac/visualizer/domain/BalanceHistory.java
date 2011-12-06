package org.powertac.visualizer.domain;

import org.powertac.common.CashPosition;
import org.powertac.common.Timeslot;
import org.powertac.common.msg.TimeslotUpdate;

public class BalanceHistory {

	private TimeslotUpdate timeslotUpdate;
	private double balance;
	private int timeslotNumber;

	public BalanceHistory(double balance, int timeslotNumber,
			TimeslotUpdate timeslotUpdate) {
		this.balance = balance;
		this.timeslotNumber = timeslotNumber;
		this.timeslotUpdate = timeslotUpdate;
	}

	public TimeslotUpdate getTimeslotUpdate() {
		return timeslotUpdate;
	}

	public void setTimeslotUpdate(TimeslotUpdate timeslotUpdate) {
		this.timeslotUpdate = timeslotUpdate;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	public int getTimeslotNumber() {
		return timeslotNumber;
	}

	public void setTimeslotNumber(int timeslotNumber) {
		this.timeslotNumber = timeslotNumber;
	}
	
	

}
