package org.powertac.visualizer.domain.broker;

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
	private ConcurrentHashMap<CustomerInfo, TariffCustomerStats> tariffCustomerStats;

	public TariffData(TariffSpecification spec, BrokerModel broker) {
		this.spec = spec;
		this.broker = broker;
		tariffCustomerStats = new ConcurrentHashMap<CustomerInfo, TariffCustomerStats>(
				20, 0.75f, 1);
	}
	
	public double getNetKWh() {
		return netKWh;
	}
	public double getProfit() {
		return profit;
	}
	public TariffSpecification getSpec() {
		return spec;
	}
	public ConcurrentHashMap<CustomerInfo, TariffCustomerStats> getTariffCustomerStats() {
		return tariffCustomerStats;
	}

	public void processTariffTx(TariffTransaction tx) {
		profit+=tx.getCharge();
		netKWh+=tx.getKWh();
		tariffCustomerStats.putIfAbsent(tx.getCustomerInfo(), new TariffCustomerStats(tx.getCustomerInfo(),spec));
		tariffCustomerStats.get(tx.getCustomerInfo()).addAmounts(tx.getCharge(),tx.getKWh());
		
	}
	
	public BrokerModel getBroker() {
		return broker;
	}

}
