package org.powertac.visualizer.interfaces;

import org.joda.time.Instant;
import org.primefaces.json.JSONException;

/**
 * Should be implemented by spring's singletons that need activation upon timeslot completion.
 * @author Jurica Babic
 *
 */
public interface TimeslotCompleteActivation {
	
	/**
	 * @param timeslotIndex index of completed timeslot.
	 * @throws JSONException 
	 */
	public void activate(int timeslotIndex, Instant postedTime);
}
