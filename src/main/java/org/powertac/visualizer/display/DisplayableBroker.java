package org.powertac.visualizer.display;

import java.io.Serializable;

import org.powertac.visualizer.domain.Appearance;
import org.powertac.visualizer.domain.broker.BrokerModel;

/**
 * The front-end representation of a broker. It is built based upon a BrokerModel instance.
 * @author Jurica Babic
 *
 */
public class DisplayableBroker implements Serializable {
	private String name;
	private Appearance appearance;
	private DisplayableBalancingCategory balancing;
	
	public DisplayableBroker(BrokerModel model) {
		this.balancing = new DisplayableBalancingCategory(model.getBalancingCategory());
		this.name = model.getName();
		this.appearance = model.getAppearance();
	}
	
	public String getName() {
		return name;
	}
	public Appearance getAppearance() {
		return appearance;
	}
	public DisplayableBalancingCategory getBalancing() {
		return balancing;
	}

}
