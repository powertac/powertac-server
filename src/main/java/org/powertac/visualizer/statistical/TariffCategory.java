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

	private double profit;
	private double netKWh;
	private int customerCount;

	// key: postedTime
	private ConcurrentHashMap<Long, TariffDynamicData> tariffDynamicData;
	private ConcurrentHashMap<CustomerInfo, CustomerTariffData> customerTariffData;
	private ConcurrentHashMap<TariffSpecification, TariffData> tariffData;

	public TariffCategory(BrokerModel broker) {
		super(broker);
		tariffDynamicData = new ConcurrentHashMap<Long, TariffDynamicData>(100,
				0.75f, 1);
		customerTariffData = new ConcurrentHashMap<CustomerInfo, CustomerTariffData>(
				20, 0.75f, 1);
		tariffData = new ConcurrentHashMap<TariffSpecification, TariffData>(20,
				0.75f, 1);
	}

	public void processTariffSpecification(TariffSpecification ts) {
		tariffData.putIfAbsent(ts, new TariffData(ts, broker));
	}

	public void processTariffTransaction(TariffTransaction tx) {

		// handle SIGNUP&WITHDRAW transactions:
		int deltaCustomers = Helper.getCustomerCount(tx);

		tariffDynamicData.putIfAbsent(tx.getPostedTime().getMillis(),
				new TariffDynamicData(profit, netKWh, customerCount))
				.addAmounts(tx.getCharge(), tx.getKWh(), deltaCustomers);

		customerTariffData.putIfAbsent(tx.getCustomerInfo(),
				new CustomerTariffData(broker, tx.getCustomerInfo()))
				.addAmounts(tx.getCharge(), tx.getKWh());

		TariffData tData = tariffData.get(tx.getTariffSpec());
		tData.processTariffTx(tx);

		profit += tx.getCharge();
		netKWh += tx.getKWh();
		customerCount += deltaCustomers;

	}

	public double getNetKWh() {
		return netKWh;
	}

	public double getProfit() {
		return profit;
	}

	public ConcurrentHashMap<Long, TariffDynamicData> getTariffDynamicData() {
		return tariffDynamicData;
	}

	public int getCustomerCount() {
		return customerCount;
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
