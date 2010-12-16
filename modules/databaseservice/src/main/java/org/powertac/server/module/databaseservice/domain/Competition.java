package org.powertac.server.module.databaseservice.domain;

import org.joda.time.LocalDateTime;
import org.powertac.common.enumerations.CompetitionStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@RooJavaBean
@RooToString
@RooEntity
public class Competition {

    private String name;

    private Boolean enabled;

    private Boolean currentCompetition;

    @Enumerated
    private CompetitionStatus competitionStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "S-")
    private LocalDateTime dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "S-")
    private LocalDateTime lastUpdated;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "competition")
    private Set<BrokerCompetition> brokerCompetitions = new HashSet<BrokerCompetition>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "competition")
    private Set<CashRecord> cashRecords = new HashSet<CashRecord>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "competition")
    private Set<Customer> customers = new HashSet<Customer>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "competition")
    private Set<MeterReading> meterReadings = new HashSet<MeterReading>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "competition")
    private Set<Orderbook> orderbooks = new HashSet<Orderbook>();
}
