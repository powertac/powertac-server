package org.powertac.visualizer.services.competitioncontrol;

/**
 * This class allows us to modify broker's name from the web-interface (because a list of Strings won't work). 
 * @author Jurica Babic
 *
 */
public class FakeBroker {

	private String name;

	public FakeBroker(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
