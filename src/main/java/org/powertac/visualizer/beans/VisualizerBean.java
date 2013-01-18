package org.powertac.visualizer.beans;

import java.io.Serializable;
import java.util.List;

import javax.faces.bean.ManagedBean;

import org.apache.log4j.Logger;
import org.apache.tools.ant.types.selectors.DateSelector.TimeComparisons;
import org.powertac.common.Competition;
import org.powertac.common.msg.TimeslotComplete;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.VisualizerApplicationContext;
import org.powertac.visualizer.interfaces.Recyclable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Holds properties of the Visualizer such as number of received messages from
 * the simulator, number of Visualizer runs etc.
 * 
 * @author Jurica Babic
 */
@ManagedBean
@Service
public class VisualizerBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private Logger log = Logger.getLogger(VisualizerBean.class);

	private int messageCounter;
	private int visualizerRunCount;

	private Competition competition;
	private TimeslotUpdate oldTimeslotUpdate;
	private TimeslotUpdate timeslotUpdate;
	private String simulationStatus;
	private int currentTimeslotSerialNumber;
	private boolean finished;
	private boolean running;
	private long currentMillis;
	private TimeslotComplete timeslotComplete;

	@Autowired
	private AppearanceListBean appearanceList;

	private Boolean tournamentMode = false;

	public VisualizerBean() {
		init();
	}

	public void incrementMessageCounter() {
		messageCounter++;
	}

	public int getMessageCount() {
		return messageCounter;
	}

	/**
	 * Configures visualizer bean for the new competition instance. Resets its
	 * properties and increments Visualizer counter.
	 */
	public void newRun() {
		init();
		appearanceList.resetAvailableList(); // so broker appearances will be
		// free for the next competition
		// Recycle:
		List<Recyclable> recyclables = VisualizerApplicationContext
				.listBeansOfType(Recyclable.class);
		for (Recyclable rec : recyclables) {
			log.info("recycling..." + rec.getClass().getName());
			rec.recycle();
		}
	}

	public void init() {
		messageCounter = 0;
		visualizerRunCount++;
		competition = null;
		oldTimeslotUpdate = null;
		timeslotUpdate = null;
		simulationStatus = null;
		currentTimeslotSerialNumber = -1;
		finished = false;
		running = false;

		currentMillis = 0;

	}

	public int getVisualizerRunCount() {
		return visualizerRunCount;
	}

	public Competition getCompetition() {
		return competition;
	}

	public void setCompetition(Competition competition) {
		this.competition = competition;
	}

	public TimeslotUpdate getTimeslotUpdate() {
		return timeslotUpdate;

	}

	public void setTimeslotUpdate(TimeslotUpdate timeslotUpdate) {
		this.timeslotUpdate = timeslotUpdate;
	}

	public String getSimulationStatus() {
		return simulationStatus;
	}

	public void setSimulationStatus(String simulationStatus) {
		this.simulationStatus = simulationStatus;
	}

	public int getCurrentTimeslotSerialNumber() {
		return currentTimeslotSerialNumber;
	}

	public void setCurrentTimeslotSerialNumber(int currentTimeslotSerialNumber) {
		this.currentTimeslotSerialNumber = currentTimeslotSerialNumber;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public long getCurrentMillis() {
		return currentMillis;
	}

	public void setCurrentMillis(long currentMillis) {
		this.currentMillis = currentMillis;
	}

	public TimeslotUpdate getOldTimeslotUpdate() {
		return oldTimeslotUpdate;
	}

	public void setOldTimeslotUpdate(TimeslotUpdate oldTimeslotUpdate) {
		this.oldTimeslotUpdate = oldTimeslotUpdate;
	}

	public Boolean getTournamentMode() {
		return tournamentMode;
	}

	public void setTournamentMode(Boolean tournamentMode) {
		this.tournamentMode = tournamentMode;
	}
	
	public TimeslotComplete getTimeslotComplete() {
		return timeslotComplete;
	}
	public void setTimeslotComplete(TimeslotComplete timeslotComplete) {
		this.timeslotComplete = timeslotComplete;
	}
}
