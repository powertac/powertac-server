package org.powertac.common.commands;

import org.joda.time.LocalDateTime;
import org.powertac.common.interfaces.Broker;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 * User: cblock
 * Date: 01.12.10
 * Time: 15:52
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractTariffCommand {

  Broker broker;

  /*
  *  Basic contract properties
  */
  private BigDecimal signupFee;       //one-time fee (>0) / reward (<0) charged / paid for contract signup
  private BigDecimal baseFee;         //daily base Fee (>0) / reward (<0) charged at the end of each day

  /*
   * Distinct powerConsumptionPrices
   */
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
  private BigDecimal powerConsumptionPrice23;    // --- " ---"

  private BigDecimal powerProductionPrice0;     //kWh dependent power consumption fee (>0) / reward (<0) for hour 0
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
  private BigDecimal powerProductionPrice23;      // --- " ---"

  private LocalDateTime contractStartDate;        //defines a specific contract start Date, may be specified by customer or broker
  private LocalDateTime contractEndDate;          //defines a specific contract end Date, may be specified by customer or broker

  /*
  * These attributes allow a broker to specify minimum and maximum contract runtimes, e.g. min one month
  */
  private Integer minimumContractRuntime;         //null or min days; has to be consistent with contractEndTime - contractStartTime
  private Integer maximumContractRuntime;         //null or max days; has to be consistent with contractEndTime - contractStartTime

  /*
  * These attributes allow modeling a two-part tariff
  */
  private BigDecimal powerConsumptionThreshold;   // >0: threshold consumption level; consumption exceeding this threshold leads to a surcharge (see below) being added to the time dependent powerConsumptionPrice
  private BigDecimal powerConsumptionSurcharge;   // % fee added to hourly powerConsumptionPrice if consumption exceeds threshold (see above)

  private BigDecimal powerProductionThreshold;    // >0: threshold production level; production exceeding this threshold leads to a surcharge (see below) being added to the time dependent powerProductionPrice
  private BigDecimal powerProductionSurcharge;    // % fee added to hourly powerProductionPrice if production exceeds threshold (see above)

}
