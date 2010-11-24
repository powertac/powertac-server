package org.powertac.domain;

import java.math.BigDecimal;

public class Customer {

  enum CustomerType {ConsumerHousehold, ConsumerOffice, ProducerSolar, ProducerWind, ProducerRunOfRiver, ProducerFossil };
  private String name;

  private BigDecimal upperPowerCap;   // >0: max power consumption (think consumer with fuse limit); <0: min power production (think nuclear power plant with min output)
  private BigDecimal lowerPowerCap;   // >0: min power consumption (think refrigerator); <0: max power production (think power plant with max capacity)

  private CustomerType customerType;

}
