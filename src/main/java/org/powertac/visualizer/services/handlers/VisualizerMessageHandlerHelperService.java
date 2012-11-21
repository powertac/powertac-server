package org.powertac.visualizer.services.handlers;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.DayOverview;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.DayState;
import org.powertac.visualizer.services.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VisualizerMessageHandlerHelperService {

	@Autowired
	private AppearanceListBean appearanceListBean;
	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private BrokerService brokerService;
	private Logger log = Logger.getLogger(VisualizerMessageHandlerHelperService.class);


//	public int computeRelativeTimeslotIndex(Instant timeslot) {
//
//		long millisDifference = timeslot.getMillis()-visualizerBean.getFirstTimeslotInstant().getMillis();
//		long numberOfHours = millisDifference / (1000*60*60); 
//		return (int) numberOfHours; //<- will be a relative number of timeslots.
//	}

}
