package org.powertac.visualizer.services.handlers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.DayOverview;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.CustomerModel;
import org.powertac.visualizer.domain.broker.DayState;
import org.powertac.visualizer.interfaces.VisualBroker;
import org.powertac.visualizer.services.BrokerService;
import org.primefaces.json.JSONArray;
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


	public int computeRelativeTimeslotIndex(Instant timeslot) {

		long millisDifference = timeslot.getMillis()-visualizerBean.getFirstTimeslotInstant().getMillis();
		long numberOfHours = millisDifference / (1000*60*60); 
		return (int) numberOfHours; //<- will be a relative number of timeslots.
	}

	/**
	 * Builds day overview object for VisualizerBean. Should be called after visualizerBean and brokers have been informed about the new timeslot index.
	 */
	public void buildDayOverview() {
		//build displayable dayStates list:
		ArrayList<DayState> dayStates = new ArrayList<DayState>();
		for (Iterator<BrokerModel> iterator = brokerService.getBrokerList().iterator(); iterator.hasNext();) {
			BrokerModel broker = (BrokerModel) iterator.next();
			dayStates.add(broker.getDisplayableDayState());
		}
		int day = visualizerBean.getCurrentTimeslotSerialNumber()/24;
		
		visualizerBean.setDayOverview(new DayOverview(dayStates,day));	
		
	}



}
