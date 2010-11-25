package org.powertac.server.core.domain;

import org.joda.time.LocalDateTime;

public class Product {

  Competition competition;
  enum ProductType {Future, Option}
  ProductType productType;
  Timeslot timeslot;
}
