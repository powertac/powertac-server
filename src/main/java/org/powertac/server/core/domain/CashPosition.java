package org.powertac.server.core.domain;

import org.joda.time.LocalDateTime;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.math.BigDecimal;

@RooJavaBean
@RooToString
@RooEntity
public class CashPosition {

  @ManyToOne(targetEntity = Competition.class) @JoinColumn Competition competition;
  @ManyToOne(targetEntity = Broker.class) @JoinColumn Broker broker;
  BigDecimal amount;
  BigDecimal balance;
  String description;
  Boolean latest;
  Long transactionId;
  private LocalDateTime dateCreated;
}
