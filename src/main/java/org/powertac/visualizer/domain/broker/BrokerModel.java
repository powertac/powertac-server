package org.powertac.visualizer.domain.broker;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.domain.Appearance;
import org.powertac.visualizer.statistical.AggregateDistributionData;
import org.powertac.visualizer.statistical.BalancingCategory;
import org.powertac.visualizer.statistical.GradingSystem;
import org.powertac.visualizer.statistical.TariffCategory;
import org.powertac.visualizer.statistical.WholesaleCategory;

public class BrokerModel {// implements TimeslotModelUpdate {

	Logger log = Logger.getLogger(BrokerModel.class);
	// basic
	private String name;
	private Appearance appearance;
	private String id;
	// customers
	private int customerCount;
	// balance
	private double cashBalance;

	private BalancingCategory balancingCategory;
	private WholesaleCategory wholesaleCategory;
	private TariffCategory tariffCategory;
	private AggregateDistributionData aggregateDistributionData = new AggregateDistributionData();

	public BrokerModel(String name, Appearance appearance) {
		this.name = name;
		this.appearance = appearance;
		id = RandomStringUtils.random(7,
				"abcdefghijklomnopqrstuvxy".toCharArray());

		balancingCategory = new BalancingCategory(this);
		wholesaleCategory = new WholesaleCategory(this);
		tariffCategory = new TariffCategory(this);

	}

	public BalancingCategory getBalancingCategory() {
		return balancingCategory;
	}

	public void updateCashBalance(double balance) {
		this.cashBalance = Helper.roundNumberTwoDecimal(balance);
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

	public double getCashBalance() {
		return cashBalance;
	}

	public void setCashBalance(double cashBalance) {
		this.cashBalance = cashBalance;
	}

	public String getId() {
		return id;
	}

	public int getCustomerCount() {
		return customerCount;
	}

	public WholesaleCategory getWholesaleCategory() {
		return wholesaleCategory;
	}

	public AggregateDistributionData getAggregateDistributionData() {
		return aggregateDistributionData;
	}

	public void grade() {
		balancingCategory.setGrade(GradingSystem.getBalancingGrade(
				balancingCategory.getAggregateBalancingData().getTotalKwh(),
				aggregateDistributionData.getNetKWh()));
		wholesaleCategory.setGrade(GradingSystem.getWholesaleMarketGrade(
				wholesaleCategory.getNoOrders(),
				wholesaleCategory.getNoMarketTransactions()));

	}
	
	public TariffCategory getTariffCategory() {
		return tariffCategory;
	}

}
