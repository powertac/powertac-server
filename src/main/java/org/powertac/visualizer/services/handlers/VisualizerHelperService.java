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
}
