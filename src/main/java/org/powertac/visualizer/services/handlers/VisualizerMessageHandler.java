package org.powertac.visualizer.services.handlers;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.powertac.common.BankTransaction;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.common.msg.DistributionReport;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.TariffUpdate;
import org.powertac.common.msg.TimeslotComplete;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.VisualizerApplicationContext;
import org.powertac.visualizer.beans.AppearanceListBean;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.powertac.visualizer.push.GlobalPusher;
import org.powertac.visualizer.push.InfoPush;
import org.powertac.visualizer.services.PushService;
import org.primefaces.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VisualizerMessageHandler implements Initializable {

	private Logger log = LogManager
			.getLogger(VisualizerMessageHandler.class);
	@Autowired
	private VisualizerBean visualizerBean;
	@Autowired
	private VisualizerHelperService helper;
	@Autowired
	private AppearanceListBean appearanceListBean;
	@Autowired
	private MessageDispatcher router;
	@Autowired
	private PushService pushService;

	
	public void handleMessage(TimeslotUpdate timeslotUpdate) {

		// if there was the timeslot update before:
		if (visualizerBean.getTimeslotUpdate() != null) {
			visualizerBean.setOldTimeslotUpdate(visualizerBean
					.getTimeslotUpdate());
		}

		visualizerBean.setTimeslotUpdate(timeslotUpdate);

		// millis: should include timezone in the future.
		visualizerBean.setCurrentMillis(timeslotUpdate.getPostedTime()
				.getMillis());

		// // for the first timeslot:
		// if (visualizerBean.getFirstTimeslotInstant() == null)
		// visualizerBean.setFirstTimeslotInstant(timeslotUpdate.getPostedTime());

		StringBuilder builder = new StringBuilder();
		builder.append("Enabled timeslots:");
		for (int i = timeslotUpdate.getFirstEnabled(); i < timeslotUpdate
				.getLastEnabled(); i++) {

			builder.append(" ").append(i);
		}

		log.debug(builder + "\n");

		// int relativeTimeslotIndex =
		// helper.computeRelativeTimeslotIndex(timeslotUpdate.getPostedTime());
		// for all brokers and gencos:
		// helper.updateTimeslotIndex(relativeTimeslotIndex);
		// update for visualizerBean:
		// visualizerBean.setRelativeTimeslotIndex(relativeTimeslotIndex);

		// log.debug("\nTimeslot index: " + relativeTimeslotIndex +
		// "\nPostedtime:" + timeslotUpdate.getPostedTime());

		Competition comp = visualizerBean.getCompetition();
		// timeslot serial number:
		int timeslotSerialNumber = timeslotUpdate.getFirstEnabled()
				- comp.getDeactivateTimeslotsAhead();
		visualizerBean.setCurrentTimeslotSerialNumber(timeslotSerialNumber);

		// visualizerBean.setWeek(timeslotSerialNumber / (24 * 7));
		// visualizerBean.setDay(timeslotSerialNumber / 24);
		// visualizerBean.setHour(timeslotSerialNumber % 24);

	}

	public void handleMessage(TimeslotComplete complete) {
		visualizerBean.setTimeslotComplete(complete);
		List<TimeslotCompleteActivation> activators = VisualizerApplicationContext
				.listBeansOfType(TimeslotCompleteActivation.class);
		for (TimeslotCompleteActivation active : activators) {
			// System.out.println("activating..." +
			// active.getClass().getSimpleName());
			active.activate(complete.getTimeslotIndex(), visualizerBean
					.getTimeslotUpdate().getPostedTime());

		}

		// TODO call pushables:

		// int relativeTimeslotIndex = complete.getTimeslotIndex() -
		// visualizerBean.getFirstTimeslotIndex();
		
		//GLOBAL PUSH:
		pushService.pushGlobal(new GlobalPusher(visualizerBean.getWeatherPusher(), visualizerBean.getNominationPusher()));

	}

	public void handleMessage(SimStart simStart) {

	}

	public void handleMessage(SimEnd simEnd) {
		visualizerBean.setRunning(false);
		visualizerBean.setFinished(true);
		pushService.pushInfoMessage(new InfoPush("finish"));
	}

	public void handleMessage(SimPause simPause) {
		// TODO
	}

	public void handleMessage(BankTransaction bankTransaction) {
		// TODO
	}

	public void handleMessage(SimResume simResume) {
		// TODO

	}

	public void handleMessage(DistributionReport report) {
		log.debug("DIST REPORT: " + "PROD " + report.getTotalProduction()
				+ "CONS " + report.getTotalConsumption());
	}

	public void handleMessage(TariffExpire msg) {
		// TODO
	}

	public void handleMessage(TariffRevoke msg) {
		// TODO
	}

	public void handleMessage(TariffStatus msg) {
		// TODO
	}

	public void handleMessage(TariffUpdate msg) {
		// TODO
	}

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(DistributionReport.class,
				SimResume.class, SimEnd.class, BankTransaction.class,
				SimPause.class, SimStart.class, TimeslotComplete.class,
				TimeslotUpdate.class, TariffExpire.class,
				TariffRevoke.class, TariffStatus.class, TariffUpdate.class)) {
			router.registerMessageHandler(this, clazz);
		}
	}
}
