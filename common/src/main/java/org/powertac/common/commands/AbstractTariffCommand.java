package org.powertac.common.commands;

import org.joda.time.LocalDateTime;
import org.powertac.common.interfaces.Broker;

import java.math.BigDecimal;
import java.util.ArrayList;

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

    public BigDecimal getSignupFee() {
        return signupFee;
    }

    public void setSignupFee(BigDecimal signupFee) {
        this.signupFee = signupFee;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    public LocalDateTime getContractStartDate() {
        return contractStartDate;
    }

    public void setContractStartDate(LocalDateTime contractStartDate) {
        this.contractStartDate = contractStartDate;
    }

    public LocalDateTime getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(LocalDateTime contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public Integer getMinimumContractRuntime() {
        return minimumContractRuntime;
    }

    public void setMinimumContractRuntime(Integer minimumContractRuntime) {
        this.minimumContractRuntime = minimumContractRuntime;
    }

    public Integer getMaximumContractRuntime() {
        return maximumContractRuntime;
    }

    public void setMaximumContractRuntime(Integer maximumContractRuntime) {
        this.maximumContractRuntime = maximumContractRuntime;
    }

    public BigDecimal getPowerConsumptionThreshold() {
        return powerConsumptionThreshold;
    }

    public void setPowerConsumptionThreshold(BigDecimal powerConsumptionThreshold) {
        this.powerConsumptionThreshold = powerConsumptionThreshold;
    }

    public BigDecimal getPowerConsumptionSurcharge() {
        return powerConsumptionSurcharge;
    }

    public void setPowerConsumptionSurcharge(BigDecimal powerConsumptionSurcharge) {
        this.powerConsumptionSurcharge = powerConsumptionSurcharge;
    }

    public BigDecimal getPowerProductionThreshold() {
        return powerProductionThreshold;
    }

    public void setPowerProductionThreshold(BigDecimal powerProductionThreshold) {
        this.powerProductionThreshold = powerProductionThreshold;
    }

    public BigDecimal getPowerProductionSurcharge() {
        return powerProductionSurcharge;
    }

    public void setPowerProductionSurcharge(BigDecimal powerProductionSurcharge) {
        this.powerProductionSurcharge = powerProductionSurcharge;
    }

    private BigDecimal powerProductionSurcharge;    // % fee added to hourly powerProductionPrice if production exceeds threshold (see above)
    
    public ArrayList<BigDecimal> getPowerProductionPriceList()
    {
        ArrayList<BigDecimal> powerProductionPriceList = new ArrayList<BigDecimal>();
        powerProductionPriceList.add(powerProductionPrice0);
        powerProductionPriceList.add(powerProductionPrice1);
        powerProductionPriceList.add(powerProductionPrice2);
        powerProductionPriceList.add(powerProductionPrice3);
        powerProductionPriceList.add(powerProductionPrice4);
        powerProductionPriceList.add(powerProductionPrice5);
        powerProductionPriceList.add(powerProductionPrice6);
        powerProductionPriceList.add(powerProductionPrice7);
        powerProductionPriceList.add(powerProductionPrice8);
        powerProductionPriceList.add(powerProductionPrice9);
        powerProductionPriceList.add(powerProductionPrice10);
        powerProductionPriceList.add(powerProductionPrice11);
        powerProductionPriceList.add(powerProductionPrice12);
        powerProductionPriceList.add(powerProductionPrice13);
        powerProductionPriceList.add(powerProductionPrice14);
        powerProductionPriceList.add(powerProductionPrice15);
        powerProductionPriceList.add(powerProductionPrice16);
        powerProductionPriceList.add(powerProductionPrice17);
        powerProductionPriceList.add(powerProductionPrice18);
        powerProductionPriceList.add(powerProductionPrice19);
        powerProductionPriceList.add(powerProductionPrice20);
        powerProductionPriceList.add(powerProductionPrice21);
        powerProductionPriceList.add(powerProductionPrice22);
        powerProductionPriceList.add(powerProductionPrice23);
        return powerProductionPriceList;
    }
    
        public ArrayList<BigDecimal> getPowerConsumptionPriceList()
    {
        ArrayList<BigDecimal> powerConsumptionPriceList = new ArrayList<BigDecimal>();
        powerConsumptionPriceList.add(powerConsumptionPrice0);
        powerConsumptionPriceList.add(powerConsumptionPrice1);
        powerConsumptionPriceList.add(powerConsumptionPrice2);
        powerConsumptionPriceList.add(powerConsumptionPrice3);
        powerConsumptionPriceList.add(powerConsumptionPrice4);
        powerConsumptionPriceList.add(powerConsumptionPrice5);
        powerConsumptionPriceList.add(powerConsumptionPrice6);
        powerConsumptionPriceList.add(powerConsumptionPrice7);
        powerConsumptionPriceList.add(powerConsumptionPrice8);
        powerConsumptionPriceList.add(powerConsumptionPrice9);
        powerConsumptionPriceList.add(powerConsumptionPrice10);
        powerConsumptionPriceList.add(powerConsumptionPrice11);
        powerConsumptionPriceList.add(powerConsumptionPrice12);
        powerConsumptionPriceList.add(powerConsumptionPrice13);
        powerConsumptionPriceList.add(powerConsumptionPrice14);
        powerConsumptionPriceList.add(powerConsumptionPrice15);
        powerConsumptionPriceList.add(powerConsumptionPrice16);
        powerConsumptionPriceList.add(powerConsumptionPrice17);
        powerConsumptionPriceList.add(powerConsumptionPrice18);
        powerConsumptionPriceList.add(powerConsumptionPrice19);
        powerConsumptionPriceList.add(powerConsumptionPrice20);
        powerConsumptionPriceList.add(powerConsumptionPrice21);
        powerConsumptionPriceList.add(powerConsumptionPrice22);
        powerConsumptionPriceList.add(powerConsumptionPrice23);
        return powerConsumptionPriceList;
    }

}
