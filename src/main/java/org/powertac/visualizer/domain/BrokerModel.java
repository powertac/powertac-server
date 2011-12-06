package org.powertac.visualizer.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang.RandomStringUtils;
import org.powertac.common.CashPosition;
import org.powertac.common.TariffSpecification;
import org.powertac.common.msg.TimeslotUpdate;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.LineChartSeries;

public class BrokerModel {

	private String name;
	private Appearance appearance;
	private List<TariffSpecification> tariffSpecifications;
	private long offeredTarrifsCount;
	private double balance;
	
	private String id;
	
	private CartesianChartModel balanceChart;
	private LineChartSeries balanceChartSeries;
	
	
	

	public BrokerModel(String name, Appearance appearance) {
		this.name = name;
		this.appearance = appearance;
		tariffSpecifications = new ArrayList<TariffSpecification>();
		balance = 0;
		balanceChart = new CartesianChartModel();
		balanceChartSeries = new LineChartSeries(name);
		balanceChartSeries.set(0, 0);
		
		balanceChart.addSeries(balanceChartSeries);
		id= RandomStringUtils.random(10, "abcdefghijklmn");
		
	}

	public String getName() {
		return name;
	}
	

	public void setName(String name) {
		this.name = name;
	}

	public Appearance getAppearance() {
		return appearance;
	}

	public void setAppereance(Appearance appearance) {
		this.appearance = appearance;
	}
	public void addTariffSpecification(TariffSpecification tariffSpecification) {
		offeredTarrifsCount++;
		tariffSpecifications.add(tariffSpecification);
	}
	public List<TariffSpecification> getTariffSpecifications() {
		return tariffSpecifications;
	}
	public long getOfferedTarrifsCount() {
		return offeredTarrifsCount;
	}
	
	public double getBalance() {
		return balance;
	}
	
		
	
	/**
	 * Sets new balance and adds BalanceHistory to collection.
	 * @param timeslotUpdate
	 * @param balance
	 * @param timeslotNumber
	 */
	public void updateBalance(TimeslotUpdate timeslotUpdate, double balance, int timeslotNumber){
		this.balance=balance;
		//primefaces chart
		
		balanceChartSeries.set(timeslotNumber, balance);
		
	}

	public CartesianChartModel getBalanceChart() {
		return balanceChart;
	}

	public void setBalanceChart(CartesianChartModel balanceChart) {
		this.balanceChart = balanceChart;
	}

	public LineChartSeries getBalanceChartSeries() {
		return balanceChartSeries;
	}

	public void setBalanceChartSeries(LineChartSeries balanceChartSeries) {
		this.balanceChartSeries = balanceChartSeries;
	}
	
	public String getId() {
		return id;
	}
	
}
