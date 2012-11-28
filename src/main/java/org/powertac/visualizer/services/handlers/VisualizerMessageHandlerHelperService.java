package org.powertac.visualizer.services.handlers;

import org.joda.time.Instant;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.DayOverview;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.DayState;
import org.powertac.visualizer.services.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;


@Service
public class VisualizerMessageHandlerHelperService {

	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private BrokerService brokerService;

	public int computeRelativeTimeslotIndex(Instant timeslot) {

		long millisDifference = timeslot.getMillis()-visualizerBean.getFirstTimeslotInstant().getMillis();
		long numberOfHours = millisDifference / (1000*60*60); 
		return (int) numberOfHours; //<- will be a relative number of timeslots.
	}

	/**
	 * Builds day overview object for VisualizerBean. Should be called after
   * visualizerBean and brokers have been informed about the new timeslot index.
	 */
	public void buildDayOverview() {
		//build displayable dayStates list:
		ArrayList<DayState> dayStates = new ArrayList<DayState>();
    for (BrokerModel broker: brokerService.getBrokerList()) {
      dayStates.add(broker.getDisplayableDayState());
    }
		int day = visualizerBean.getCurrentTimeslotSerialNumber()/24;
		
		visualizerBean.setDayOverview(new DayOverview(dayStates,day));	
	}
}
