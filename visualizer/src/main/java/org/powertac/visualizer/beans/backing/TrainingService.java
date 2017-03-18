package org.powertac.visualizer.beans.backing;

import org.powertac.common.MarketPosition;
import org.powertac.common.TariffTransaction;
import org.springframework.stereotype.Service;

@Service
public class TrainingService {

	private int counter=0;
	
	public int getCounter() {
		return counter;
	}
	public void increment() {
		counter++;
		
	}
}
