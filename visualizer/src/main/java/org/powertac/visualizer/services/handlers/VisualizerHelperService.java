package org.powertac.visualizer.services.handlers;

import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.services.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VisualizerHelperService {

	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private BrokerService brokerService;

	public long getMillisForIndex(int index) {
		Competition comp = visualizerBean.getCompetition();
		if (comp != null) {
			return new Instant(comp.getSimulationBaseTime().getMillis() + index
					* comp.getTimeslotDuration()).getMillis();
		} else {
			return -1;
		}
	}

	public int getTimeslotIndex(Instant time) {
		Competition comp = visualizerBean.getCompetition();
		if (comp != null) {
			long offset = time.getMillis()
					- comp.getSimulationBaseTime().getMillis();
			long duration = comp.getTimeslotDuration();
			// truncate to timeslot boundary
			return (int) (offset / duration);
		} else {
			return -1;
		}
	}

	/**
	 * @return returns a safe timeslot index for wholesale visualization
	 *         purposes. It will return a timeslot index for which all wholesale
	 *         trades have been made and thus will not be mutable.
	 */
	public int getSafetyWholesaleTimeslotIndex() {
		if (visualizerBean.getTimeslotComplete() != null) {
			int lastCompletedTimeslot = visualizerBean.getTimeslotComplete()
					.getTimeslotIndex();
			// NOTE: timeslot is finished, but the info about the final clearing
			// will be
			// in the next timeslot.
			return lastCompletedTimeslot - 1;
		} else
			return -1;

	}
	public int getSafetyTimeslotIndex() {
		if (visualizerBean.getTimeslotComplete() != null) {
			int lastCompletedTimeslot = visualizerBean.getTimeslotComplete()
					.getTimeslotIndex();
			return lastCompletedTimeslot;
		} else
			return -1;

	}

	
}
