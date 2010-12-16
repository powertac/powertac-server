package org.powertac.server.module.databaseservice.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

@RooJavaBean
@RooToString
@RooEntity
public class BrokerCompetition {

  @ManyToOne
  @JoinColumn
  private Broker broker;

  @ManyToOne
  @JoinColumn
  private Competition competition;

  @NotNull
  @Value("false")
  private Boolean ready;
}
