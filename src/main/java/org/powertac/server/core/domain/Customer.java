package org.powertac.server.core.domain;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import org.powertac.server.core.enumeration.CustomerType;
import javax.persistence.Enumerated;
import java.math.BigDecimal;

@RooJavaBean
@RooToString
@RooEntity
public class Customer {

    @NotNull
    @ManyToOne
    private Competition competition;

    @NotNull
    private String name;

    @NotNull
    @Enumerated
    private CustomerType customerType;      //gives a "rough" classification what type of customer to expect based on an enumeration, i.e. a fixed set of customer types

    @NotNull
    private Boolean smartMetering;          // true=customer has a smart meter -> detailed consumption data available, false=customer has no smart meter -> no detailed consumption data

    @NotNull
    private Boolean multiContracting;       // describes whether or not this customer engages in multiple contracts at the same time

    @NotNull
    private Boolean canNegotiate;           // describes whether or not this customer negotiates over contracts

    private BigDecimal upperPowerCap;       // >0: max power consumption (think consumer with fuse limit); <0: min power production (think nuclear power plant with min output)

    private BigDecimal lowerPowerCap;       // >0: min power consumption (think refrigerator); <0: max power production (think power plant with max capacity)

    private BigDecimal carbonEmissionRate;  // >=0 - gram CO2 per kW/h

    private BigDecimal annualPowerAvg;      // >0: customer is on average a consumer; <0 customer is on average a producer

    private BigDecimal minResponsiveness;   // TODO: define factor characterizing minimal responsiveness to price signals, i.e. "elasticity"

    private BigDecimal maxResponsiveness;   // TODO: define factor characterizing max responsiveness to price signals, i.e. "elasticity"

    private BigDecimal windToPowerRating;   // measures how wind changes translate into load changes of the customer

    private BigDecimal tempToPowerRating;   // measures how temperature changes translate into load changes of the customer

    private BigDecimal sunToPowerRating;    // measures how sun intensity changes translate into load changes of the customer
}
