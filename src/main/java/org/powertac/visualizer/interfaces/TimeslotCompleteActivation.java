package org.powertac.visualizer.interfaces;

/**
 * Should be implemented by spring's singletons that need activation upon timeslot completion.
 * @author Jurica Babic
 *
 */
public interface TimeslotCompleteActivation {
	
	/**
	 * @param timeslotIndex index of completed timeslot.
	 */
	public void activate(int timeslotIndex);
}
