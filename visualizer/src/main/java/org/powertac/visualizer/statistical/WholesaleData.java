package org.powertac.visualizer.statistical;

import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.visualizer.domain.broker.BrokerModel;

/**
 * Represent one broker's action on the wholesale market.
 * 
 * @author Jurica Babic
 * 
 */
public class WholesaleData {

	private BrokerModel broker;
	private double priceOrder;
	private double energyOrder;
	private double priceTx;
	private double energyTx;

	public WholesaleData(BrokerModel broker, Order order) {
		this.broker = broker;
		priceOrder = order.getLimitPrice();
		energyOrder = order.getMWh();
	}

	public void setBroker(BrokerModel broker) {
		this.broker = broker;
	}

	public void processMarketTransaction(MarketTransaction tx) {
		energyTx = tx.getMWh();
		priceTx = tx.getPrice();

	}

	public BrokerModel getBroker() {
		return broker;
	}

	public double getEnergyOrder() {
		return energyOrder;
	}

	public double getEnergyTx() {
		return energyTx;
	}

	public double getPriceOrder() {
		return priceOrder;
	}

	public double getPriceTx() {
		return priceTx;
	}

}
