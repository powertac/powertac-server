package org.powertac.visualizer.services.handlers;

import org.apache.log4j.Logger;
import org.powertac.common.*;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.TariffUpdate;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.CustomerModel;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.CustomerService;
import org.powertac.visualizer.statistical.BalancingData;
import org.powertac.visualizer.statistical.Grade;
import org.powertac.visualizer.statistical.GradingSystem;
import org.primefaces.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BrokerMessageHandler implements Initializable {

	private Logger log = Logger.getLogger(BrokerMessageHandler.class);

	@Autowired
	private MessageDispatcher router;
	@Autowired
	private BrokerService brokerService;
	@Autowired
	private AppearanceListBean appearanceListBean;

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(Competition.class,
				TariffSpecification.class, CashPosition.class,
				TariffTransaction.class, DistributionTransaction.class,
				BalancingTransaction.class)) {
			router.registerMessageHandler(this, clazz);
		}
	}

	public void handleMessage(Competition competition) {
		List<String> brokersName = competition.getBrokers();
		
		for (Iterator<String> iterator = brokersName.iterator(); iterator
				.hasNext();) {
			String name = (String) iterator.next();
			BrokerModel brokerModel = new BrokerModel(name,
					appearanceListBean.getAppereance());
			brokerService.addBroker(brokerModel);
		}
		
	}

	public void handleMessage(TariffSpecification tariffSpecification) {
				if (tariffSpecification.getSupersedes() != null) {
			log.debug("NO of tariffspec:"
					+ tariffSpecification.getSupersedes().size());
		}

		// find matching broker and add received tariff spec. to its history.
		BrokerModel brokerModel = brokerService
				.findBrokerByName(tariffSpecification.getBroker().getUsername());
		if (brokerModel != null) {
			brokerModel.getTariffCategory().processTariffSpecification(tariffSpecification);
		}

	}

	public void handleMessage(CashPosition cashPosition) {

		// update balance, if such broker exists
		BrokerModel broker = brokerService.findBrokerByName(cashPosition
				.getBroker().getUsername());

		if (broker != null) {
			//process cashPosition

		}

	}

	public void handleMessage(TariffTransaction tariffTransaction) {
		// broker, not genco:
		BrokerModel brokerModel = brokerService
				.findBrokerByName(tariffTransaction.getBroker().getUsername());
		if (brokerModel != null) {
			brokerModel.getTariffCategory().processTariffTransaction(tariffTransaction);

		}

	}

	public void handleMessage(DistributionTransaction dTx) {
		BrokerModel brokerModel = brokerService.findBrokerByName(dTx.getBroker().getUsername());
		if (brokerModel != null) {
			brokerModel.getAggregateDistributionData().addValues(dTx.getKWh(), dTx.getKWh());
		}
	
	}

	public void handleMessage(BalancingTransaction balancingTransaction) {
		BrokerModel broker = brokerService
				.findBrokerByName(balancingTransaction.getBroker()
						.getUsername());
		if (broker != null) {
			BalancingData data = new BalancingData(
					balancingTransaction.getKWh(),
					balancingTransaction.getCharge(), balancingTransaction
							.getPostedTime().getMillis());
			broker.getBalancingCategory().addBalancingData(data);
		}
	}
}
