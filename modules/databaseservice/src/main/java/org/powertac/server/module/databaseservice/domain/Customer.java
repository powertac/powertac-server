package org.powertac.server.module.databaseservice.domain;

import org.powertac.common.enumerations.CustomerType;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@RooJavaBean
@RooToString
@RooEntity
public class Customer {

    @ManyToOne
    @JoinColumn
    private Competition competition;

    @NotNull
    private String name;

    @NotNull
    @Enumerated
    private CustomerType customerType;

    @NotNull
    private Boolean smartMetering;

    @NotNull
    private Boolean multiContracting;

    @NotNull
    private Boolean canNegotiate;

    private BigDecimal upperPowerCap;

    private BigDecimal lowerPowerCap;

    private BigDecimal carbonEmissionRate;

    private BigDecimal annualPowerAvg;

    private BigDecimal minResponsiveness;

    private BigDecimal maxResponsiveness;

    private BigDecimal windToPowerRating;

    private BigDecimal tempToPowerRating;

    private BigDecimal sunToPowerRating;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
    private Set<MeterReading> meterReadings = new HashSet<MeterReading>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
    private Set<Tariff> tariffs = new HashSet<Tariff>();
}
