package org.powertac.server.core.domain;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.entity.RooEntity;
import org.powertac.server.core.domain.Broker;
import javax.persistence.ManyToOne;
import org.powertac.server.core.domain.Competition;
import javax.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;

@RooJavaBean
@RooToString
@RooEntity
public class BrokerCompetition {

    @ManyToOne
    private Broker broker;

    @ManyToOne
    private Competition competition;

    @NotNull
    @Value("false")
    private Boolean ready;
}
