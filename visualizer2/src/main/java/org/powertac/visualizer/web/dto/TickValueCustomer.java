package org.powertac.visualizer.web.dto;

import org.powertac.visualizer.domain.RetailKPIHolder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@JsonInclude(Include.NON_NULL)
public class TickValueCustomer {

    private long id;
    private RetailKPIHolder retail;

    protected TickValueCustomer() {

    }

    public TickValueCustomer(long id, RetailKPIHolder retail) {
        this.id = id;
        this.retail = retail.isEmpty() ? null : retail;
    }

    public RetailKPIHolder getRetail() {
        return retail;
    }

    public long getId() {
        return id;
    }

    @JsonIgnore
    public boolean isEmpty() {
      return retail == null;
    }
}
