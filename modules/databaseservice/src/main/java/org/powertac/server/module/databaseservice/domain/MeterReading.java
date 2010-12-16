package org.powertac.server.module.databaseservice.domain;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RooJavaBean
@RooToString
@RooEntity
public class MeterReading {

  @ManyToOne
  private Competition competition;

  @ManyToOne
  private Customer customer;

  @ManyToOne
  private Timeslot timeslot;

  @NotNull
  BigDecimal amount;
}
