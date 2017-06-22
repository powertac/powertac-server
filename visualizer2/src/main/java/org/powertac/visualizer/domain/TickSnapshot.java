package org.powertac.visualizer.domain;

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
public class TickSnapshot {

    private long timeInstance;
    private int timeSlot;
    private List<TickValueBroker> tickValueBrokers;
    private List<TickValueCustomer> tickValueCustomers;

    public TickSnapshot() {

    }

    public TickSnapshot(long timeInstance, int timeSlot, List<TickValueBroker> brokerTicks,
            List<TickValueCustomer> customerTicks) {
        this.timeInstance = timeInstance;
        this.timeSlot = timeSlot;
        this.tickValueBrokers = brokerTicks;
        this.tickValueCustomers = customerTicks;
    }

    public TickSnapshot(long timeInstance, int timeSlot) {
        this.timeInstance = timeInstance;
        this.timeSlot = timeSlot;
        this.tickValueBrokers = new ArrayList<>();
        this.tickValueCustomers = new ArrayList<>();
    }

    public long getTimeInstance() {
        return timeInstance;
    }

    public void setTimeInstance(long timeInstance) {
        this.timeInstance = timeInstance;
    }

    public int getTimeSlot() {
      return timeSlot;
    }

    public void setTimeSlot(int timeSlot) {
      this.timeSlot = timeSlot;
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
