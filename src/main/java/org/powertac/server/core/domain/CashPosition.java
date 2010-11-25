package org.powertac.server.core.domain;

import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

public class CashPosition {
  Competition competition;
  Broker broker;
  BigDecimal amount;
  BigDecimal balance;
  String description;
  Boolean latest;
  Long transactionId;
  private LocalDateTime dateCreated;
}
