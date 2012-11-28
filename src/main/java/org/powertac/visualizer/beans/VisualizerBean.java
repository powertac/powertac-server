package org.powertac.visualizer.beans;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Competition;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.VisualizerApplicationContext;
import org.powertac.visualizer.domain.DayOverview;
import org.powertac.visualizer.interfaces.Recyclable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * Holds properties of the Visualizer such as number of received messages from
 * the simulator, number of Visualizer runs etc.
 * 
 * @author Jurica Babic
 */
@Service
public class VisualizerBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private Logger log = Logger.getLogger(VisualizerBean.class);

	private int messageCounter;
	private int visualizerRunCount;

	private DayOverview dayOverview;

	private Competition competition;
	private Instant firstTimeslotInstant;
	private TimeslotUpdate timeslotUpdate;
	private String simulationStatus;
	private int currentTimeslotSerialNumber;
	private int relativeTimeslotIndex;
	private int week;
	private int day;
	private int hour;
	private boolean finished;
	private boolean running;
	private int firstTimeslotIndex;
	private long currentMillis;

	@Autowired
	private AppearanceListBean appearanceList;

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
		List<Recyclable> recyclables = VisualizerApplicationContext.listBeansOfType(Recyclable.class);
		for (Recyclable rec : recyclables) {
			log.info("recycling..." + rec.getClass().getName());
			rec.recycle();
		}
	}

	public void init() {
		messageCounter = 0;
		visualizerRunCount++;

		dayOverview = null;

		competition = null;

		firstTimeslotInstant = null;

		timeslotUpdate = null;
		simulationStatus = null;
		currentTimeslotSerialNumber = -1;
		relativeTimeslotIndex = -1;
		week = -1;
		day = -1;
		hour = -1;
		finished = false;
		running = false;
		firstTimeslotIndex = -1;
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

	public int getCurrentFirstEnabledTimeslotSerialNumber() {
		if (timeslotUpdate != null)
			return timeslotUpdate.getFirstEnabled();
		else {
			return -1;
		}
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

	/**
	 * 
	 * 
	 * @param relativeTimeslotIndex
	 */
	public void setRelativeTimeslotIndex(int relativeTimeslotIndex) {
		this.relativeTimeslotIndex = relativeTimeslotIndex;

	}

	public int getRelativeTimeslotIndex() {
		return relativeTimeslotIndex;
	}

	public void setFirstTimeslotInstant(Instant firstTimeslot) {
		this.firstTimeslotInstant = firstTimeslot;
	}

	public Instant getFirstTimeslotInstant() {
		return firstTimeslotInstant;
	}

	public DayOverview getDayOverview() {
		return dayOverview;
	}

	public void setDayOverview(DayOverview dayOverview) {
		this.dayOverview = dayOverview;
	}

	public int getWeek() {
		return week;
	}

	public void setWeek(int week) {
		this.week = week;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public int getHour() {
		return hour;
	}

	public void setHour(int hour) {
		this.hour = hour;
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

	public int getFirstTimeslotIndex() {
		return firstTimeslotIndex;
	}

	public void setFirstTimeslotIndex(int firstTimeslotIndex) {
		this.firstTimeslotIndex = firstTimeslotIndex;
	}
	
	public long getCurrentMillis() {
		return currentMillis;
	}
	public void setCurrentMillis(long currentMillis) {
		this.currentMillis = currentMillis;
	}

}
