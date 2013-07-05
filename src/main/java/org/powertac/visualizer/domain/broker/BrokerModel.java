package org.powertac.visualizer.domain.broker;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.powertac.visualizer.domain.Appearance;
import org.powertac.visualizer.statistical.BalancingCategory;
import org.powertac.visualizer.statistical.DistributionCategory;
import org.powertac.visualizer.statistical.FinanceCategory;
import org.powertac.visualizer.statistical.TariffCategory;
import org.powertac.visualizer.statistical.WholesaleCategory;

public class BrokerModel {

	Logger log = Logger.getLogger(BrokerModel.class);
	// basic
	private String name;
	private Appearance appearance;
	private String id;
	
	private FinanceCategory financeCategory;
	private BalancingCategory balancingCategory;
	private WholesaleCategory wholesaleCategory; 
	private TariffCategory tariffCategory;
	private DistributionCategory distributionCategory;
	
	public BrokerModel(String name, Appearance appearance) {
		this.name = name;
		this.appearance = appearance;
		id = RandomStringUtils.random(7,
				"abcdefghijklomnopqrstuvxy".toCharArray());

		balancingCategory = new BalancingCategory(this);
		wholesaleCategory = new WholesaleCategory(this);
		tariffCategory = new TariffCategory(this);
		financeCategory = new FinanceCategory(this);
		distributionCategory = new DistributionCategory(this);

	}

	public BalancingCategory getBalancingCategory() {
		return balancingCategory;
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

	public String getId() {
		return id;
	}

	public WholesaleCategory getWholesaleCategory() {
		return wholesaleCategory;
	}


	public void grade() {
//		balancingCategory.setGrade(GradingSystem.getBalancingGrade(
//				balancingCategory.getKwh(),
//				aggregateDistributionData.getNetKWh()));
//		wholesaleCategory.setGrade(GradingSystem.getWholesaleMarketGrade(
//				wholesaleCategory.getNoOrders(),
//				wholesaleCategory.getNoMarketTransactions()));

	}
	
	public DistributionCategory getDistributionCategory() {
		return distributionCategory;
	}
	
	public TariffCategory getTariffCategory() {
		return tariffCategory;
	}
	
	public FinanceCategory getFinanceCategory() {
		return financeCategory;
	}

}
