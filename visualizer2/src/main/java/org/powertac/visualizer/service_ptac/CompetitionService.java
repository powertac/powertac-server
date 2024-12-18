package org.powertac.visualizer.service_ptac;

import java.time.Instant;
import org.powertac.common.TimeService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class CompetitionService {

    private static long idCounter = 0;
    private long id = idCounter++;

    /**
     * Original ID from Power TAC.
     */
    private long idPowerTacCompetition;

    /** The competition's name */
    private String name;

    /** Optional text that further describes the competition */
    private String description;

    /** length of a timeslot in simulation minutes */
    private int timeslotLength;

    /** Number of timeslots in initialization data dump */
    private int bootstrapTimeslotCount; // 14 days

    /**
     * Number of extra timeslots at start of bootstrap before data collection
     * starts
     */
    private int bootstrapDiscardedTimeslots;

    /** Minimum number of timeslots, aka competition length */
    private int minimumTimeslotCount;

    private int expectedTimeslotCount;

    /**
     * concurrently open timeslots, i.e. time window in which broker actions
     * like trading are allowed
     */
    private int timeslotsOpen = 24;

    /**
     * # timeslots a timeslot gets deactivated ahead of the now timeslot
     * (default: 1 timeslot, which (given default length of 60 min) means that
     * e.g. trading is disabled 60 minutes ahead of time
     */
    private int deactivateTimeslotsAhead = 1;

    /** Minimum order quantity */
    private double minimumOrderQuantity; // MWh

    /** the start time of the simulation scenario, in sim time. */
    private Instant simulationBaseTime;

    /** timezone offset for scenario locale */
    private int timezoneOffset;

    /** approximate latitude in degrees north for scenario locale */
    private int latitude;

    /**
     * the time-compression ratio for the simulation. So if we are running
     * one-hour timeslots every 5 seconds, the rate would be 720 (=default).
     */
    private long simulationRate;

    /**
     * controls the values of simulation time values reported. If we are running
     * one-hour timeslots, then the modulo should be one hour, expressed in
     * milliseconds. If we are running one-hour timeslots but want to update
     * time every 30 minutes of simulated time, then the modulo would be
     * 30*60*1000. Note that this will not work correctly unless the calls to
     * updateTime() are made at modulo/rate intervals. Also note that the
     * reported time is computed as rawTime - rawTime % modulo, which means it
     * will never be ahead of the raw simulation time. Note that values other
     * than the length of a timeslot have not been tested.
     */
    private long simulationModulo;

    protected CompetitionService() {
    }

    public void setCurrent(org.powertac.common.Competition comp) {
        this.idPowerTacCompetition = comp.getId();
        this.name = comp.getName();
        this.description = comp.getDescription();
        this.timeslotLength = comp.getTimeslotLength();
        this.bootstrapTimeslotCount = comp.getBootstrapTimeslotCount();
        this.bootstrapDiscardedTimeslots = comp.getBootstrapDiscardedTimeslots();
        this.minimumTimeslotCount = comp.getMinimumTimeslotCount();
        this.expectedTimeslotCount = comp.getExpectedTimeslotCount();
        this.timeslotsOpen = comp.getTimeslotsOpen();
        this.deactivateTimeslotsAhead = comp.getDeactivateTimeslotsAhead();
        this.minimumOrderQuantity = comp.getMinimumOrderQuantity();
        this.simulationBaseTime = comp.getSimulationBaseTime();
        this.timezoneOffset = comp.getTimezoneOffset();
        this.latitude = comp.getLatitude();
        this.simulationRate = comp.getSimulationRate();
        this.simulationModulo = comp.getSimulationModulo();
    }

    public long getId() {
        return id;
    }

    public long getIdPowerTacCompetition() {
        return idPowerTacCompetition;
    }

    public long getTimeslotDuration() {
        return timeslotLength * TimeService.MINUTE;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getTimeslotLength() {
        return timeslotLength;
    }

    public int getBootstrapTimeslotCount() {
        return bootstrapTimeslotCount;
    }

    public int getBootstrapDiscardedTimeslots() {
        return bootstrapDiscardedTimeslots;
    }

    public int getMinimumTimeslotCount() {
        return minimumTimeslotCount;
    }

    public int getExpectedTimeslotCount() {
        return expectedTimeslotCount;
    }

    public int getTimeslotsOpen() {
        return timeslotsOpen;
    }

    public int getDeactivateTimeslotsAhead() {
        return deactivateTimeslotsAhead;
    }

    public double getMinimumOrderQuantity() {
        return minimumOrderQuantity;
    }

    public Instant getSimulationBaseTime() {
        return simulationBaseTime;
    }

    public int getTimezoneOffset() {
        return timezoneOffset;
    }

    public int getLatitude() {
        return latitude;
    }

    public long getSimulationRate() {
        return simulationRate;
    }

    public long getSimulationModulo() {
        return simulationModulo;
    }

}
