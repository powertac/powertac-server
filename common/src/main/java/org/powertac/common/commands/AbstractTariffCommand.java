/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.common.commands;

import org.joda.time.LocalDateTime;

import java.math.BigDecimal;

/**
 * Command object that represents tariff data
 * to be exchanged between Power TAC brokers and
 * customers. Once created the properties of this
 * class are immutable.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 */
public abstract class AbstractTariffCommand {

    private static int DECIMALS = 2;

    private Long tariffId;

    /*
    *  Basic contract properties
    */
    private BigDecimal signupFee;       //one-time fee (>0) / reward (<0) charged / paid for contract signup
    private BigDecimal baseFee;         //daily base Fee (>0) / reward (<0) charged at the end of each day

    /*
    * Distinct powerConsumptionPrices
    */
    BigDecimal[] powerConsumptionPriceList = null;
    BigDecimal[] powerProductionPriceList = null;

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

    protected AbstractTariffCommand(Long tariffId, BigDecimal signupFee, BigDecimal baseFee, BigDecimal[] powerConsumptionPriceList, BigDecimal[] powerProductionPriceList, LocalDateTime contractStartDate, LocalDateTime contractEndDate, Integer minimumContractRuntime, Integer maximumContractRuntime, BigDecimal powerConsumptionThreshold, BigDecimal powerConsumptionSurcharge, BigDecimal powerProductionThreshold, BigDecimal powerProductionSurcharge) {
        this.tariffId = tariffId;
        this.signupFee = signupFee;
        this.baseFee = baseFee;
        this.powerConsumptionPriceList = powerConsumptionPriceList;
        this.powerProductionPriceList = powerProductionPriceList;
        this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate;
        this.minimumContractRuntime = minimumContractRuntime;
        this.maximumContractRuntime = maximumContractRuntime;
        this.powerConsumptionThreshold = powerConsumptionThreshold;
        this.powerConsumptionSurcharge = powerConsumptionSurcharge;
        this.powerProductionThreshold = powerProductionThreshold;
        this.powerProductionSurcharge = powerProductionSurcharge;
    }

    protected AbstractTariffCommand(Long tariffId, Double signupFee, Double baseFee, Double[] powerConsumptionPriceList, Double[] powerProductionPriceList, LocalDateTime contractStartDate, LocalDateTime contractEndDate, Integer minimumContractRuntime, Integer maximumContractRuntime, Double powerConsumptionThreshold, Double powerConsumptionSurcharge, Double powerProductionThreshold, Double powerProductionSurcharge) {

        this.tariffId = tariffId;

        this.signupFee = new BigDecimal(signupFee.toString()); //Note: constructing BigDecimal from the double's String representation: see http://download.oracle.com/javase/1.5.0/docs/api/java/math/BigDecimal.html
        this.signupFee = this.signupFee.setScale(DECIMALS);

        this.baseFee = new BigDecimal(baseFee.toString());
        this.baseFee = this.baseFee.setScale(DECIMALS);

        for (int i = 0; i < powerConsumptionPriceList.length; i++) {
            BigDecimal value = new BigDecimal(powerConsumptionPriceList[i].toString());
            value = value.setScale(DECIMALS);
            this.powerConsumptionPriceList[i] = value;
        }
        for (int i = 0; i < powerProductionPriceList.length; i++) {
            BigDecimal value = new BigDecimal(powerProductionPriceList[i].toString());
            value.setScale(DECIMALS);
            this.powerProductionPriceList[i] = value;
        }

        this.contractStartDate = contractStartDate;
        this.contractEndDate = contractEndDate;

        this.minimumContractRuntime = minimumContractRuntime;
        this.maximumContractRuntime = maximumContractRuntime;

        this.powerConsumptionThreshold = new BigDecimal(powerConsumptionThreshold.toString());
        this.powerConsumptionThreshold = this.powerConsumptionThreshold.setScale(DECIMALS);

        this.powerConsumptionSurcharge = new BigDecimal(powerConsumptionSurcharge.toString());
        this.powerConsumptionSurcharge = this.powerConsumptionSurcharge.setScale(DECIMALS);

        this.powerProductionThreshold = new BigDecimal(powerProductionThreshold.toString());
        this.powerProductionThreshold = this.powerProductionThreshold.setScale(DECIMALS);

        this.powerProductionSurcharge = new BigDecimal(powerProductionSurcharge.toString());
        this.powerProductionSurcharge = this.powerProductionSurcharge.setScale(DECIMALS);
    }

    /*
    * ----------------------------------------------------------------------------------------------
    *   Public accessors; note: this class is immutable once created, thus no mutators are provided
    * ----------------------------------------------------------------------------------------------
    */

    public Long getTariffId() {
        return tariffId;
    }

    public BigDecimal getSignupFee() {
        return signupFee;
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public LocalDateTime getContractStartDate() {
        return contractStartDate;
    }

    public LocalDateTime getContractEndDate() {
        return contractEndDate;
    }

    public Integer getMinimumContractRuntime() {
        return minimumContractRuntime;
    }

    public Integer getMaximumContractRuntime() {
        return maximumContractRuntime;
    }

    public BigDecimal getPowerConsumptionThreshold() {
        return powerConsumptionThreshold;
    }

    public BigDecimal getPowerConsumptionSurcharge() {
        return powerConsumptionSurcharge;
    }

    public BigDecimal getPowerProductionThreshold() {
        return powerProductionThreshold;
    }

    public BigDecimal getPowerProductionSurcharge() {
        return powerProductionSurcharge;
    }

    public BigDecimal[] getPowerProductionPriceArray() {
        return powerProductionPriceList;
    }

    public BigDecimal[] getPowerConsumptionPriceArray() {
        return powerConsumptionPriceList;
    }
}
