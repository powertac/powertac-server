package org.powertac.visualizer.domain;

import java.util.List;
import java.util.Set;

import org.joda.time.Instant;
import org.powertac.common.BalancingTransaction;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;

/**
 * Represents abstract broker model for Visualizer. There can be various
 * implementations of this interface, such as: BrokerModel and GencoModel.
 * 
 * @author Jurica Babic
 * 
 */
public interface VisualBroker {
	public String getName();

	public Appearance getAppearance();

	public void addTariffSpecification(TariffSpecification tariffSpecification);

	public void addTariffTransaction(TariffTransaction tariffTransaction);

	public void setCustomerModels(Set<CustomerModel> customerModels);

	public List<TariffSpecification> getTariffSpecifications();

	public long getOfferedTarrifsCount();

	public double getCashBalance();

	public double getEnergyBalance();

	public void updateCashBalance(double balance);

	public void addBalancingTransaction(BalancingTransaction balancingTransaction);

	public int getCurrentTimeslotIndex();

	public void setCurrentTimeslotIndex(int currentTimeslotIndex);

	
}
