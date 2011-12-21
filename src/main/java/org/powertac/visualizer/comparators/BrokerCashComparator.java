package org.powertac.visualizer.comparators;

import java.util.Comparator;

import org.powertac.visualizer.domain.BrokerModel;

public class BrokerCashComparator implements Comparator<BrokerModel> {

	public int compare(BrokerModel o1, BrokerModel o2) {

		if (o1.getCashBalance() > o2.getCashBalance()) {
			return -1;
		} else if (o1.getCashBalance() < o2.getCashBalance()) {
			return 1;
		} else
			return 0;

	}

}
