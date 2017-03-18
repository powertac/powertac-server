package org.powertac.visualizer.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import org.powertac.visualizer.web.dto.TickValueBroker;
import org.powertac.visualizer.web.dto.TickValueCustomer;

/**
 * A model object used for storing per-tick (i.e., time slot) values. The object
 * is pushed to the front-end where the processing (e.g., assembly of graphs)
 * takes place.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TickSnapshot {

    private long timeInstance;
    private List<TickValueBroker> tickValueBrokers;
    private List<TickValueCustomer> tickValueCustomers;

    protected TickSnapshot() {

    }

    public TickSnapshot(long timeInstance, List<TickValueBroker> brokerTicks,
            List<TickValueCustomer> customerTicks) {
        this.timeInstance = timeInstance;
        this.tickValueBrokers = brokerTicks;
        this.tickValueCustomers = customerTicks;
    }

    public TickSnapshot(long timeInstance) {
        this.timeInstance = timeInstance;
        this.tickValueBrokers = new ArrayList<>();
        this.tickValueCustomers = new ArrayList<>();
    }

    public long getTimeInstance() {
        return timeInstance;
    }

    public void setTimeInstance(long timeInstance) {
        this.timeInstance = timeInstance;
    }

    public List<TickValueBroker> getTickValueBrokers() {
        return tickValueBrokers;
    }

    public void setTickValueBrokers(List<TickValueBroker> tickValueBrokers) {
        this.tickValueBrokers = tickValueBrokers;
    }

    public List<TickValueCustomer> getTickValueCustomers() {
        return tickValueCustomers;
    }

    public void setTickValueCustomers(List<TickValueCustomer> tickValueCustomers) {
        this.tickValueCustomers = tickValueCustomers;
    }
}
