package org.powertac.visualizer.services.handlers;

import org.apache.log4j.Logger;
import org.powertac.common.*;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.TariffUpdate;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.CustomerModel;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.CustomerService;
import org.powertac.visualizer.statistical.BalancingCategory;
import org.powertac.visualizer.statistical.DistributionCategory;
import org.powertac.visualizer.statistical.DynamicData;
import org.powertac.visualizer.statistical.FinanceCategory;
import org.powertac.visualizer.statistical.FinanceDynamicData;
import org.powertac.visualizer.statistical.Grade;
import org.powertac.visualizer.statistical.GradingSystem;
import org.powertac.visualizer.statistical.TariffCategory;
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
			brokerModel.getTariffCategory().processTariffSpecification(
					tariffSpecification);
		}

	}

	public void handleMessage(CashPosition msg) {

		// update balance, if such broker exists
		BrokerModel broker = brokerService.findBrokerByName(msg.getBroker()
				.getUsername());
		if (broker != null) {
			FinanceCategory fc = broker.getFinanceCategory();

			fc.getFinanceDynamicDataMap().putIfAbsent(
					msg.getPostedTimeslotIndex(),
					new FinanceDynamicData(fc.getProfit(), msg
							.getPostedTimeslotIndex()));
			fc.getFinanceDynamicDataMap().get(msg.getPostedTimeslotIndex())
					.updateBalance(msg.getBalance());
			fc.setProfit(msg.getBalance());
		}

	}

	public void handleMessage(TariffTransaction msg) {
		// broker, not genco:
		BrokerModel broker = brokerService.findBrokerByName(msg.getBroker()
				.getUsername());
		if (broker != null) {
			TariffCategory dc = broker.getTariffCategory();

			dc.getTariffDynamicDataMap()
					.putIfAbsent(
							msg.getPostedTimeslotIndex(),
							new TariffDynamicData(msg.getPostedTimeslotIndex(),
									dc.getProfit(), dc.getEnergy(), dc
											.getCustomerCount()));
			dc.getTariffDynamicDataMap()
					.get(msg.getPostedTimeslotIndex())
					.update(msg.getKWh(), msg.getCharge(),
							Helper.getCustomerCount(msg));
			dc.update(msg.getKWh(), msg.getCharge());

		}

	}

	public void handleMessage(DistributionTransaction msg) {
		BrokerModel broker = brokerService.findBrokerByName(msg.getBroker()
				.getUsername());
		if (broker != null) {
			DistributionCategory dc = broker.getDistributionCategory();

			dc.getDynamicDataMap().putIfAbsent(
					msg.getPostedTimeslotIndex(),
					new DynamicData(msg.getPostedTimeslotIndex(), dc
							.getEnergy(), dc.getProfit()));
			dc.getDynamicDataMap().get(msg.getPostedTimeslotIndex())
					.update(msg.getKWh(), msg.getCharge());
			dc.update(msg.getKWh(), msg.getCharge());
		}

	}

	public void handleMessage(BalancingTransaction bt) {
		BrokerModel broker = brokerService.findBrokerByName(bt.getBroker()
				.getUsername());
		if (broker != null) {
			BalancingCategory bc = broker.getBalancingCategory();

			bc.getDynamicDataMap().putIfAbsent(
					bt.getPostedTimeslotIndex(),
					new DynamicData(bt.getPostedTimeslotIndex(),
							bc.getEnergy(), bc.getProfit()));
			bc.getDynamicDataMap().get(bt.getPostedTimeslotIndex())
					.update(bt.getKWh(), bt.getCharge());
			bc.update(bt.getKWh(), bt.getCharge());
		}
	}
}
