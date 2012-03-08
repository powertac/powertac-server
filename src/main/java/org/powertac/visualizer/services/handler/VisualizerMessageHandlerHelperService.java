package org.powertac.visualizer.services.handler;

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
import org.powertac.visualizer.domain.BrokerModel;
import org.powertac.visualizer.domain.CustomerModel;
import org.powertac.visualizer.domain.DayOverview;
import org.powertac.visualizer.domain.DayState;
import org.powertac.visualizer.domain.GencoModel;
import org.powertac.visualizer.domain.VisualBroker;
import org.primefaces.json.JSONArray;
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
		List<BrokerModel> brokers = new ArrayList<BrokerModel>(brokersName.size());

		for (Iterator<String> iterator = brokersName.iterator(); iterator.hasNext();) {
			String name = (String) iterator.next();
			BrokerModel brokerModel = new BrokerModel(name, appearanceListBean.getAppereance());
			brokers.add(brokerModel);
		}

		return brokers;
	}

	/**
	 * @param template
	 * @return VisualBroker object that matches Broker username. Returns Null if
	 *         such does not exists.
	 */
	public VisualBroker findVisualBroker(Broker template) {

		VisualBroker visualBroker = null;

		// first try to find matching competition broker:
		visualBroker = (VisualBroker) findBrokerModel(template);
		if (visualBroker!=null) {
			return visualBroker;
		}
		// then try to find matching genco:
		List<GencoModel> gencoModels = visualizerBean.getGencos();
		for (Iterator iterator = gencoModels.iterator(); iterator.hasNext();) {
			GencoModel gencoModel = (GencoModel) iterator.next();
			if (gencoModel.getName().equals(template.getUsername())) {

				return gencoModel;
			}
		}

		log.debug("Could not find broker");
		return visualBroker;
	}

	public BrokerModel findBrokerModel(Broker template) {
		List<BrokerModel> brokerModels = visualizerBean.getBrokers();
		for (Iterator iterator = brokerModels.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			if (brokerModel.getName().equals(template.getUsername())) {

				return brokerModel;
			}
		}
		return null;

	}

	/**
	 * For each broker builds collection of customer models.
	 * 
	 * @param brokers
	 * @param customers
	 */
	public void buildCustomerModels(List<BrokerModel> brokers, List<CustomerInfo> customers) {

		for (Iterator<BrokerModel> iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			Set<CustomerModel> customerModels = new HashSet<CustomerModel>();
			for (Iterator<CustomerInfo> iterator2 = customers.iterator(); iterator2.hasNext();) {
				CustomerInfo customerInfo = (CustomerInfo) iterator2.next();
				customerModels.add(new CustomerModel(customerInfo));
				
			}
			
			brokerModel.setCustomerModels(customerModels);
		}

	}

	public int computeRelativeTimeslotIndex(Instant timeslot) {
//		int timeslotIndex = visualizerBean.getCompetition().computeTimeslotIndex(timeslot);
//		return timeslotIndex-visualizerBean.getFirstTimeslotIndex();
//	}
		long millisDifference = timeslot.getMillis()-visualizerBean.getFirstTimeslotInstant().getMillis();
		long numberOfHours = millisDifference / (1000*60*60); 
		return (int) numberOfHours; //<- will be a relative number of timeslots.
	}

	public void updateTimeslotIndex(int relativeTimeslotIndex) {
		List<BrokerModel> brokers = visualizerBean.getBrokers();
		for (Iterator iterator = brokers.iterator(); iterator.hasNext();) {
			BrokerModel brokerModel = (BrokerModel) iterator.next();
			brokerModel.setCurrentTimeslotIndex(relativeTimeslotIndex);
		}
		
		List<GencoModel> gencos = visualizerBean.getGencos();
		for (Iterator iterator = gencos.iterator(); iterator.hasNext();) {
			GencoModel gencoModel = (GencoModel) iterator.next();
			gencoModel.setCurrentTimeslotIndex(relativeTimeslotIndex);
		}
	}

	public void updateGlobalCharts() {

		//cash lineChart:
		JSONArray cashChartArray = new JSONArray();
		//subscription pieChart:
		JSONArray customerCountArray = new JSONArray();
		
		
		if (visualizerBean.getBrokers() != null) {
			for (Iterator iterator = visualizerBean.getBrokers().iterator(); iterator.hasNext();) {
				BrokerModel broker = (BrokerModel) iterator.next();
				customerCountArray.put(broker.getCustomerCount());
				cashChartArray.put(broker.getCashBalanceJson());
			}
			visualizerBean.setSubscriptionsPieChartJSON(customerCountArray);
			visualizerBean.setBrokerCashBalancesJSON(cashChartArray);
		} 

		
	}

	/**
	 * Builds day overview object for VisualizerBean. Should be called after visualizerBean and brokers have been informed about the new timeslot index.
	 */
	public void buildDayOverview() {
		//build displayable dayStates list:
		ArrayList<DayState> dayStates = new ArrayList<DayState>();
		for (Iterator<BrokerModel> iterator = visualizerBean.getBrokers().iterator(); iterator.hasNext();) {
			BrokerModel broker = (BrokerModel) iterator.next();
			dayStates.add(broker.getDisplayableDayState());
		}
		int day = visualizerBean.getRelativeTimeslotIndex()/24;
		
		visualizerBean.setDayOverview(new DayOverview(dayStates,day));	
		
	}
	
	

}
