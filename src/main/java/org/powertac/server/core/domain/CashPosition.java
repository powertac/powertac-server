package org.powertac.server.core.domain;

import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RooJavaBean
@RooToString
@RooEntity
public class CashPosition {

  @ManyToOne(targetEntity = Competition.class)
  @JoinColumn
  Competition competition;

  @ManyToOne(targetEntity = Broker.class)
  @JoinColumn
  Broker broker;

  @NotNull
  BigDecimal amount;

  @NotNull
  BigDecimal balance;

  String description;

  @Value("false")
  Boolean latest;

  @NotNull
  Long transactionId;

  @Temporal(TemporalType.TIMESTAMP)
  @DateTimeFormat(style = "S-")
  private LocalDateTime dateCreated;
}
