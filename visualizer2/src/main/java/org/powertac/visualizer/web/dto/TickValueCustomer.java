package org.powertac.visualizer.web.dto;

import org.powertac.visualizer.domain.RetailKPIHolder;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
public class TickValueCustomer {

    private long id;
    private RetailKPIHolder retail;

    protected TickValueCustomer() {

    }

    public TickValueCustomer(long id, RetailKPIHolder retailKPIHolderCopy) {
        this.id = id;
        this.retail = retailKPIHolderCopy;
    }

    public RetailKPIHolder getRetail() {
        return retail;
    }

    public long getId() {
        return id;
    }
}
