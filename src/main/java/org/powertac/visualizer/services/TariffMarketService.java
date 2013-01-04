package org.powertac.visualizer.services;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.springframework.stereotype.Service;

@Service
public class TariffMarketService implements TimeslotCompleteActivation,Recyclable{

	private static Logger log = Logger.getLogger(TariffMarketService.class);
	
	
	
	@Override
	public void recycle() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void activate(int timeslotIndex, Instant postedTime) {
		// TODO Auto-generated method stub
		
	}

}
