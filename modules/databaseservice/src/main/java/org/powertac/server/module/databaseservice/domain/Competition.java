package org.powertac.server.module.databaseservice.domain;

import org.joda.time.LocalDateTime;
import org.powertac.common.enumerations.CompetitionStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import javax.persistence.Enumerated;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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
}
