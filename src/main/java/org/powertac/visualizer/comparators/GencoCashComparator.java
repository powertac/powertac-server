package org.powertac.visualizer.comparators;

import java.util.Comparator;

import org.powertac.visualizer.domain.GencoModel;

public class GencoCashComparator implements Comparator<GencoModel> {

	public int compare(GencoModel o1, GencoModel o2) {
		if (o1.getCashBalance() > o2.getCashBalance()) {
			return -1;
		} else if (o1.getCashBalance() < o2.getCashBalance()) {
			return 1;
		} else
			return 0;

	}
}
