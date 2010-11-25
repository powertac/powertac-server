package org.powertac.server.core.domain;

import org.joda.time.LocalDateTime;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import java.math.BigDecimal;

public class DepotPosition {
  Competition competition;
  Broker broker;
  Product product;
  Timeslot timeslot;
  BigDecimal amount;
  BigDecimal balance;
  String description;
  Boolean latest;
  Long transactionId;
  private LocalDateTime dateCreated;
}
