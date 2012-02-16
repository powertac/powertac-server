package org.powertac.visualizer.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.BalancingTransaction;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.visualizer.Helper;
import org.primefaces.json.JSONArray;

/**
 * Represents the day state from a broker.
 * 
 * @author Jurica Babic
 * 
 */
public class DayState {

	Logger log = Logger.getLogger(DayState.class);

	private int day;
	private DisplayableBroker broker;

	private ArrayList<Double> cashBalances = new ArrayList<Double>();
	private double totalCashBalance;
	private double sumCashBalance;
	private double avgCashBalance;

	private ArrayList<Double> energyBalances = new ArrayList<Double>();
	private double avgEnergyBalance;
	private double sumEnergyBalance;

	private JSONArray dayCashBalancesJson = new JSONArray();
	private JSONArray dayEnergyBalancesJson = new JSONArray();

	private List<TariffSpecification> tariffSpecifications = new ArrayList<TariffSpecification>();

	private List<TariffTransaction> tariffTransactions = new ArrayList<TariffTransaction>();
	private int signupCustomersCount;
	private int withdrawCustomersCount;
	
	private List<BalancingTransaction> balancingTransactions = new ArrayList<BalancingTransaction>();
	private double totalBalancingCharge;
	//Same thing as sumEnergyBalance!!!
	private double totalBalancingKWh;

	public DayState(int day, DisplayableBroker displayableBroker) {
		this.day = day;
		broker = displayableBroker;
	}

	/**
	 * Adds values for one timeslot (hour). Should be called sequentially for
	 * each timeslot.
	 * 
	 * @param cashBalance
	 */
	public void addTimeslotValues(int hour, double cashBalance, double energyBalance) {
		cashBalances.add(cashBalance);
		Helper.updateJSON(dayCashBalancesJson, hour, cashBalance);
		sumCashBalance += cashBalance;
		totalCashBalance = cashBalance;
		avgCashBalance = sumCashBalance / cashBalances.size();

		energyBalances.add(energyBalance);
		Helper.updateJSON(dayEnergyBalancesJson, hour, energyBalance);
		sumEnergyBalance += energyBalance;
		avgEnergyBalance = sumEnergyBalance / energyBalances.size();
	}

	public void addTariffSpecification(TariffSpecification tariffSpecification) {
		tariffSpecifications.add(tariffSpecification);
	}

	public void addTariffTransaction(TariffTransaction tariffTransaction) {
		tariffTransactions.add(tariffTransaction);
		
		switch (tariffTransaction.getTxType()) {
		case SIGNUP:
			signupCustomersCount += tariffTransaction.getCustomerCount();	
			break;
		case REVOKE:
		case WITHDRAW:		
			withdrawCustomersCount += tariffTransaction.getCustomerCount();
			break;
		case CONSUME:
		case PERIODIC:
		case PRODUCE:
		case PUBLISH:
		default:
			break;
		}
		
		
	}
	
	public void addBalancingTransaction(BalancingTransaction balancingTransaction){
		balancingTransactions.add(balancingTransaction);
		
		totalBalancingCharge+=new BigDecimal(balancingTransaction.getCharge()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		totalBalancingCharge=new BigDecimal(totalBalancingCharge).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		
		totalBalancingKWh+=new BigDecimal(balancingTransaction.getKWh()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
		totalBalancingKWh=new BigDecimal(totalBalancingKWh).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}

	public int getTariffSpecificationsCount() {
		return tariffSpecifications.size();
	}

	public JSONArray getDayCashBalancesJson() {
		return dayCashBalancesJson;
	}

	public JSONArray getDayEnergyBalancesJson() {
		return dayEnergyBalancesJson;
	}

	public double getAvgCashBalance() {

		return avgCashBalance;
	}

	public double getAvgEnergyBalance() {

		return avgEnergyBalance;
	}

	public int getDay() {
		return day;
	}

	public DisplayableBroker getBroker() {
		return broker;
	}

	public double getSumEnergyBalance() {
		return sumEnergyBalance;
	}

	public double getTotalCashBalance() {
		return totalCashBalance;
	}

	public Logger getLog() {
		return log;
	}

	public ArrayList<Double> getCashBalances() {
		return cashBalances;
	}

	public double getSumCashBalance() {
		return sumCashBalance;
	}

	public ArrayList<Double> getEnergyBalances() {
		return energyBalances;
	}

	public List<TariffSpecification> getTariffSpecifications() {
		return tariffSpecifications;
	}

	public List<TariffTransaction> getTariffTransactions() {
		return tariffTransactions;
	}

	public int getSignupCustomersCount() {
		return signupCustomersCount;
	}

	public int getWithdrawCustomersCount() {
		return withdrawCustomersCount;
	}

	public List<BalancingTransaction> getBalancingTransactions() {
		return balancingTransactions;
	}

	public double getTotalBalancingCharge() {
		return totalBalancingCharge;
	}

	public double getTotalBalancingKWh() {
		return totalBalancingKWh;
	}
	
	

}
