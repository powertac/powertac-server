package org.powertac.visualizer.web.dto;

import org.powertac.visualizer.domain.Broker;
import org.powertac.visualizer.domain.Customer;
import org.powertac.visualizer.domain.TickSnapshot;
import org.powertac.visualizer.service_ptac.CompetitionService;
import org.powertac.visualizer.service_ptac.VisualizerService.VisualizerState;

import java.util.List;

/**
 * An initial context message will be sent to clients who subscribe on a
 * Visualizer web channel. This happens when a user opens the Visualizer via
 * browser. The initial message contains data about the game context:
 * competition, brokers and customers along with corresponding IDs for each of
 * entities. The front-end is then able to associate future data based on those
 * IDs (and there is no need to transmit meta data about competition, brokers
 * and customers each time slot).
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
public class InitMessage {

    private VisualizerState state;
    private CompetitionService competition;
    private List<Broker> brokers;
    private List<Customer> customers;
    private List<TickSnapshot> snapshots;

    public InitMessage() {
        super();
    }

    public InitMessage(VisualizerState state, CompetitionService competition,
            List<Broker> brokers, List<Customer> customer,
            List<TickSnapshot> snaps) {
        super();
        this.state = state;
        this.competition = competition;
        this.brokers = brokers;
        this.customers = customer;
        this.snapshots = snaps;
    }

    public VisualizerState getState() {
        return state;
    }

    public void setState(VisualizerState state) {
        this.state = state;
    }

    public List<TickSnapshot> getSnapshots() {
        return snapshots;
    }

    public void setSnapshots(List<TickSnapshot> snapshots) {
        this.snapshots = snapshots;
    }

    public CompetitionService getCompetition() {
        return competition;
    }

    public void setCompetition(CompetitionService competition) {
        this.competition = competition;
    }

    public List<Broker> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<Broker> brokers) {
        this.brokers = brokers;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(List<Customer> customers) {
        this.customers = customers;
    }
}
