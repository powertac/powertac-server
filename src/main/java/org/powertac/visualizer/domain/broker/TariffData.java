package org.powertac.visualizer.domain.broker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;

/**
 * Holds the data for broker's tariff.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffData {
	private BrokerModel broker;
	private TariffSpecification spec;

	private double profit;
	private double netKWh;
	private long customers;
	private String powerType;
	private ConcurrentHashMap<CustomerInfo, TariffCustomerStats> tariffCustomerStats;

	public TariffData(TariffSpecification spec, BrokerModel broker) {
		this.spec = spec;
		this.broker = broker;
		tariffCustomerStats = new ConcurrentHashMap<CustomerInfo, TariffCustomerStats>(
				20, 0.75f, 1);
		powerType = spec.getPowerType().toString();
	}

	public double getNetKWh() {
		return Math.round(netKWh);
	}

	public long getCustomers() {
		return customers;
	}

	public double getProfit() {
		return Math.round(profit);
	}

	public BigDecimal getProfitInThousandsOfEuro() {
		return new BigDecimal(profit / 1000).setScale(2, RoundingMode.HALF_UP);
	}

	public TariffSpecification getSpec() {
		return spec;
	}

	public ConcurrentHashMap<CustomerInfo, TariffCustomerStats> getTariffCustomerStats() {
		return tariffCustomerStats;
	}

	public void processTariffTx(TariffTransaction tx) {
		profit += tx.getCharge();
		netKWh += tx.getKWh();
		if (tx.getCustomerInfo() != null) { // otherwise this tx is most likely to be PUBLISH
			tariffCustomerStats.putIfAbsent(tx.getCustomerInfo(),
					new TariffCustomerStats(tx.getCustomerInfo(), spec));
			tariffCustomerStats.get(tx.getCustomerInfo()).addAmounts(
					tx.getCharge(), tx.getKWh());
		}

	}

	public void setCustomers(long customers) {
		this.customers += customers;
	}

	public String getPowerType() {
		return powerType;
	}

	public BrokerModel getBroker() {
		return broker;
	}

	public BigDecimal getNetMWh() {
		return new BigDecimal(netKWh / 1000).setScale(2, RoundingMode.HALF_UP);
	}

	public String toString() {
		return tariffCustomerStats.toString();
	}

}
