package org.powertac.server.core.domain;

import java.math.BigDecimal;

public class Customer {

  enum CustomerType {ConsumerHousehold, ConsumerOffice, ConsumerElectricVehicle, ProducerSolar, ProducerWind, ProducerRunOfRiver, ProducerFossil}

  Competition competition;
  private String name;
  private CustomerType customerType;
  private Boolean smartMetering; // true=customer has a smart meter -> detailed consumption data available, false=customer has no smart meter -> no detailed consumption data
  private Boolean multiContracting; // describes whether or not this customer engages in multiple contracts at the same time
  private Boolean canNegotiate; // describes whether or not this customer negotiates over contracts

  private BigDecimal upperPowerCap;   // >0: max power consumption (think consumer with fuse limit); <0: min power production (think nuclear power plant with min output)
  private BigDecimal lowerPowerCap;   // >0: min power consumption (think refrigerator); <0: max power production (think power plant with max capacity)
  //private BigDecimal upperSheddedPowerRating;
  //private BigDecimal lowerSheededPowerRating;

  private BigDecimal generatorCarbonEmissionRate; // >=0 - gram CO2 per kW/h

  /*
  * The following give a rough description of the consumer's load profile
  */
  private BigDecimal annualPowerAvg; // >0: customer is on average a consumer; <0 customer is on average a producer
  private BigDecimal minResponsiveness; // TODO define factor characterizing minimal responsiveness to price signals, i.e. "elasticity"
  private BigDecimal maxResponsiveness; // TODO define factor characterizing max responsiveness to price signals, i.e. "elasticity"

  /*
  * The following parameters describe the weather response of the consumer's load
  */
  private BigDecimal windToPowerRating; // measures how wind changes translate into load changes of the customer
  private BigDecimal temperatureToPowerRating; // measures how temperature changes translate into load changes of the customer
  private BigDecimal sunToPowerRating; // measures how sun intensity changes translate into load changes of the customer

}
