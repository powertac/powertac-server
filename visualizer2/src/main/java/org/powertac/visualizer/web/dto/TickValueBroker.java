package org.powertac.visualizer.web.dto;

import org.powertac.visualizer.domain.Broker;
import org.powertac.visualizer.domain.RetailKPIHolder;
import org.powertac.visualizer.domain.WholesaleKPIHolder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * This object holds broker values obtained in a time slot.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@JsonInclude(Include.NON_NULL)
public class TickValueBroker {

    private long id;
    private double cash;
    private RetailKPIHolder retail;
    private WholesaleKPIHolder wholesale;

    protected TickValueBroker() {
        super();
    }

    public TickValueBroker(Broker broker, RetailKPIHolder retail,
                           WholesaleKPIHolder wholesale) {
        this.id = broker.getId();
        this.cash = broker.getCash();
        this.retail = retail.isEmpty() ? null : retail;
        this.wholesale = wholesale.isEmpty() ? null : wholesale;
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

    @JsonIgnore
    public boolean isEmpty() {
      return retail == null && wholesale == null && cash == 0.0;
    }

}
