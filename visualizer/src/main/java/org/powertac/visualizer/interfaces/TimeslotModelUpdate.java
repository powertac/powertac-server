package org.powertac.visualizer.interfaces;

import org.joda.time.Instant;

/**
 * For objects that need to (per timeslot) update their states.
 * @author Jurica Babic
 *
 */
public interface TimeslotModelUpdate {
	
	/**
	 * @param timeslotIndex update will be run for this timeslot index.
	 */
	public void update(int timeslotIndex, Instant postedTime);
}
