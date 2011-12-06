package org.powertac.visualizer.services.handler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.Broker;
import org.powertac.common.Competition;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.BrokerModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VisualizerMessageHandlerHelperService {

	@Autowired
	private AppearanceListBean appearanceListBean;
	@Autowired
	private VisualizerBean visualizerBean;
	private Logger log = Logger.getLogger(VisualizerMessageHandlerHelperService.class);

	/**
	 * Builds a broker list. Each broker will also get an appearance.
	 * 
	 * @param competition
	 *            Competition that is used for retrieving brokers.
	 * @return list of brokers
	 */
	public List<BrokerModel> buildBrokerList(Competition competition) {
		List<String> brokersName = competition.getBrokers();
		List<BrokerModel> brokers = new ArrayList<BrokerModel>(
				brokersName.size());

		for (Iterator<String> iterator = brokersName.iterator(); iterator
				.hasNext();) {
			String name = (String) iterator.next();
			BrokerModel brokerModel = new BrokerModel(name,
					appearanceListBean.getAppereance());
			brokers.add(brokerModel);
		}

		return brokers;
	}

	/**
	 * @param template
	 * @return BrokerModel object that matches Broker username. Returns Null if not exists.
	 */
	public BrokerModel findBroker(Broker template) {
		
		List<BrokerModel> brokerModels = visualizerBean.getBrokers();
		for (Iterator iterator = brokerModels.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			if (brokerModel.getName().equals(template.getUsername())) {

				return brokerModel;
			}
		}
		log.debug("Could not find broker");
		return null;
	}

}
