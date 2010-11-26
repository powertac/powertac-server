package org.powertac.server.core.domain;

import org.powertac.server.core.enumeration.CompetitionStatus;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import javax.persistence.CascadeType;
import javax.persistence.ManyToMany;
import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import javax.persistence.Enumerated;
import org.joda.time.LocalDateTime;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.springframework.format.annotation.DateTimeFormat;

@RooJavaBean
@RooToString
@RooEntity
public class Competition {

    String name;

    @ManyToMany(cascade = CascadeType.ALL, mappedBy = "competition")
    private Set<Customer> customers = new HashSet<Customer>();

    @NotNull
    @Value("false")
    private Boolean enabled;

    @NotNull
    @Value("false")
    private Boolean currentCompetition;

    @Enumerated
    private CompetitionStatus competitionStatus;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "S-")
    private LocalDateTime dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "S-")
    private LocalDateTime lastUpdated;
}
