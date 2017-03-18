package org.powertac.visualizer.interfaces;

import java.util.HashMap;

import org.powertac.visualizer.statistical.SingleTimeslotWholesaleData;

/**
 * It is used to wrap the Wholesale category object. The WholesaleBean object will thus have a limited visibility of the Wholesale Category object and will avoid possible misuse. 
 * 
 * @author Jurica Babic
 *
 */
public interface ClosedWholesaleWrapper {

	public HashMap<Integer, SingleTimeslotWholesaleData> getClosedSingleTimeslotWholesaleMap();
	
}
