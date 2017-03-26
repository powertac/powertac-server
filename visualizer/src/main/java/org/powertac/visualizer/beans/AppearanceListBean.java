package org.powertac.visualizer.beans;

import org.powertac.visualizer.domain.Appearance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the list of appearances. Such list should be declared in xml
 * declaration file.
 * 
 * @author Jurica Babic
 * 
 */
public class AppearanceListBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private List<Appearance> appereanceList;
	private List<Boolean> availableList;
	private Appearance defaultAppearance;

	public AppearanceListBean(List<Appearance> appereanceList,
			Appearance defaultAppearance) {

		this.appereanceList = appereanceList;
		this.defaultAppearance = defaultAppearance;
		resetAvailableList();

	}

	public void resetAvailableList() {
		availableList = new ArrayList<Boolean>(appereanceList.size());
		for (int i = 0; i < appereanceList.size(); i++) {
			availableList.add(true);
		}

	}

	public Appearance getAppereance() {

		Appearance appearance;
		int appereanceIndex = availableList.indexOf(true);

		if (appereanceIndex == -1) {
			appearance = defaultAppearance;
		} else {
			appearance = appereanceList.get(appereanceIndex);
			availableList.set(appereanceIndex, false);
		}
		return appearance;
	}

}
