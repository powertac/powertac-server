package org.powertac.visualizer.interfaces;

/**
 * For objects that need to (per timeslot) update their Json objects.
 * @author Jurica Babic
 *
 */
public interface JSONUpdate {
	
	/**
	 * @param timeslotIndex update will be run for this timeslot index.
	 */
	public void updateJson(int timeslotIndex);
}
