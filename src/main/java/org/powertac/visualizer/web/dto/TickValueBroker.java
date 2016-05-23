package org.powertac.visualizer.web.dto;

import org.powertac.visualizer.domain.Broker;
import org.powertac.visualizer.domain.RetailKPIHolder;

/**
 * This object holds broker values obtained in a time slot.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
public class TickValueBroker {

    private long id;
    private double cash;
    private RetailKPIHolder retail;

    protected TickValueBroker() {
        super();
    }

    public TickValueBroker(Broker broker, RetailKPIHolder retailKPIHolderCopy) {
        this.id = broker.getId();
        this.cash = broker.getCash();
        this.retail = retailKPIHolderCopy;
    }

    public long getId() {
        return id;
    }

    public RetailKPIHolder getRetail() {
        return retail;
    }

    public double getCash() {
        return cash;
    }
}
