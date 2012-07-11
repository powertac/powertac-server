package org.powertac.visualizer.interfaces;

import org.joda.time.Instant;

/**
 * Should be implemented by spring's singletons that need activation upon timeslot completion.
 * @author Jurica Babic
 *
 */
public interface TimeslotCompleteActivation {
	
	/**
	 * @param timeslotIndex index of completed timeslot.
	 */
	public void activate(int timeslotIndex, Instant postedTime);
}
