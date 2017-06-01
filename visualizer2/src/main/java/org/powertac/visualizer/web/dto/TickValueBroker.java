package org.powertac.visualizer.web.dto;

import org.powertac.visualizer.domain.Broker;
import org.powertac.visualizer.domain.RetailKPIHolder;
import org.powertac.visualizer.domain.WholesaleKPIHolder;

/**
 * This object holds broker values obtained in a time slot.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
public class TickValueBroker {

    private long id;
    private double cash;
    private RetailKPIHolder retail;
    private WholesaleKPIHolder wholesale;

    protected TickValueBroker() {
        super();
    }

    public TickValueBroker(Broker broker, RetailKPIHolder retailKPIHolder,
                           WholesaleKPIHolder wholesaleKPIHolder) {
        this.id = broker.getId();
        this.cash = broker.getCash();
        this.retail = retailKPIHolder;
        this.wholesale = wholesaleKPIHolder;
    }

    public long getId() {
        return id;
    }

    public RetailKPIHolder getRetail() {
        return retail;
    }

    public WholesaleKPIHolder getWholesale() {
      return wholesale;
    }

    public double getCash() {
        return cash;
    }
}
