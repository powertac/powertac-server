package org.powertac.server.module.databaseservice.domain;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RooJavaBean
@RooToString
@RooEntity
public class MeterReading {

  @ManyToOne
  @JoinColumn
  private Competition competition;

  @ManyToOne
  @JoinColumn
  private Customer customer;

  @ManyToOne
  @JoinColumn
  private Timeslot timeslot;

  @NotNull
  BigDecimal amount;
}
