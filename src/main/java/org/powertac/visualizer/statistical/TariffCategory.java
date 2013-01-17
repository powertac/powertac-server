package org.powertac.visualizer.statistical;

import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.CustomerInfo;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.CustomerTariffData;
import org.powertac.visualizer.domain.broker.TariffData;
import org.powertac.visualizer.domain.broker.TariffDynamicData;

/**
 * This performance category holds tariff related info for one broker.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffCategory extends AbstractPerformanceCategory implements
		PerformanceCategory {

	private int customerCount;

	// key: postedTime
	private ConcurrentHashMap<Integer, TariffDynamicData> tariffDynamicDataMap;
	private ConcurrentHashMap<CustomerInfo, CustomerTariffData> customerTariffData;
	private ConcurrentHashMap<TariffSpecification, TariffData> tariffData;

	public TariffCategory(BrokerModel broker) {
		super(broker);
		tariffDynamicDataMap = new ConcurrentHashMap<Integer, TariffDynamicData>(100,
				0.75f, 1);
		customerTariffData = new ConcurrentHashMap<CustomerInfo, CustomerTariffData>(
				20, 0.75f, 1);
		tariffData = new ConcurrentHashMap<TariffSpecification, TariffData>(20,
				0.75f, 1);
	}

	public void processTariffSpecification(TariffSpecification ts) {
		tariffData.putIfAbsent(ts, new TariffData(ts, broker));
	}

	public int getCustomerCount() {
		return customerCount;
	}
	
	public ConcurrentHashMap<Integer, TariffDynamicData> getTariffDynamicDataMap() {
		return tariffDynamicDataMap;
	}

	/**
	 * @return Info about broker's transactions related to one customer model.
	 */
	public ConcurrentHashMap<CustomerInfo, CustomerTariffData> getCustomerTariffData() {
		return customerTariffData;
	}

	/**
	 * @return Info about broker's tariff related to one customer model.
	 */
	public ConcurrentHashMap<TariffSpecification, TariffData> getTariffData() {
		return tariffData;
	}

}
