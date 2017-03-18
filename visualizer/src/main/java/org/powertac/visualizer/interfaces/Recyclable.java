package org.powertac.visualizer.interfaces;

/**
 * Implementations of this interface will want to recycle their states with a call to recycle().
 * @author Jurica Babic
 *
 */
public interface Recyclable {
	
	public void recycle();
}
