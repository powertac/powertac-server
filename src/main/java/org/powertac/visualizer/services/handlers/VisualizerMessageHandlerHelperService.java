package org.powertac.visualizer.services.handlers;

import java.util.ArrayList;

import org.joda.time.Instant;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.services.BrokerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class VisualizerMessageHandlerHelperService {

	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private BrokerService brokerService;


}
