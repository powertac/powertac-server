package org.powertac.commands;

import java.math.BigDecimal;


public class PublishTariffCommand {

  private String authToken;
  private Long tariffId;

  private Boolean isDynamic;
  private Boolean isNegotiable;
  private BigDecimal signupFee;       //one-time fee (>0) / reward (<0) charged / paid for contract signup
  private BigDecimal baseFee;
  private Integer minimumContractRuntime;         //null or min days; has to be consistent with contractEndTime - contractStartTime
  private Integer maximumContractRuntime;

  private BigDecimal powerConsumptionPrice0;     //kWh dependent power consumption fee (>0) / reward (<0) for hour 0
  private BigDecimal powerConsumptionPrice1;
  private BigDecimal powerConsumptionPrice2;
  private BigDecimal powerConsumptionPrice3;
  private BigDecimal powerConsumptionPrice4;
  private BigDecimal powerConsumptionPrice5;
  private BigDecimal powerConsumptionPrice6;
  private BigDecimal powerConsumptionPrice7;
  private BigDecimal powerConsumptionPrice8;
  private BigDecimal powerConsumptionPrice9;
  private BigDecimal powerConsumptionPrice10;
  private BigDecimal powerConsumptionPrice11;
  private BigDecimal powerConsumptionPrice12;
  private BigDecimal powerConsumptionPrice13;
  private BigDecimal powerConsumptionPrice14;
  private BigDecimal powerConsumptionPrice15;
  private BigDecimal powerConsumptionPrice16;
  private BigDecimal powerConsumptionPrice17;
  private BigDecimal powerConsumptionPrice18;
  private BigDecimal powerConsumptionPrice19;
  private BigDecimal powerConsumptionPrice20;
  private BigDecimal powerConsumptionPrice21;
  private BigDecimal powerConsumptionPrice22;
  private BigDecimal powerConsumptionPrice23;

  private BigDecimal powerProductionPrice0;
  private BigDecimal powerProductionPrice1;
  private BigDecimal powerProductionPrice2;
  private BigDecimal powerProductionPrice3;
  private BigDecimal powerProductionPrice4;
  private BigDecimal powerProductionPrice5;
  private BigDecimal powerProductionPrice6;
  private BigDecimal powerProductionPrice7;
  private BigDecimal powerProductionPrice8;
  private BigDecimal powerProductionPrice9;
  private BigDecimal powerProductionPrice10;
  private BigDecimal powerProductionPrice11;
  private BigDecimal powerProductionPrice12;
  private BigDecimal powerProductionPrice13;
  private BigDecimal powerProductionPrice14;
  private BigDecimal powerProductionPrice15;
  private BigDecimal powerProductionPrice16;
  private BigDecimal powerProductionPrice17;
  private BigDecimal powerProductionPrice18;
  private BigDecimal powerProductionPrice19;
  private BigDecimal powerProductionPrice20;
  private BigDecimal powerProductionPrice21;
  private BigDecimal powerProductionPrice22;
  private BigDecimal powerProductionPrice23;
}
