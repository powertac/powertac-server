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
import org.powertac.common.enumerations.TariffState;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Command object that represents a tariff reply, i.e.
 * a subscription to a <code>TariffPublishedCommand</code>
 * or a counter offer in a negotiation process.
 *
 * Once created this command object is immutable.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 * @see TariffReplyCommand, AbstractTariffCommand
 */
public class TariffReplyCommand extends AbstractTariffCommand {

    private static final long serialVersionUID = -2880976902173580921L;

    private TariffState tariffState;

    public TariffReplyCommand(TariffState tariffState, Long tariffId, BigDecimal signupFee, BigDecimal baseFee, BigDecimal[] powerConsumptionPriceList, BigDecimal[] powerProductionPriceList, LocalDateTime contractStartDate, LocalDateTime contractEndDate, Integer minimumContractRuntime, Integer maximumContractRuntime, BigDecimal powerConsumptionThreshold, BigDecimal powerConsumptionSurcharge, BigDecimal powerProductionThreshold, BigDecimal powerProductionSurcharge) {
        super(tariffId, signupFee, baseFee, powerConsumptionPriceList, powerProductionPriceList, contractStartDate, contractEndDate, minimumContractRuntime, maximumContractRuntime, powerConsumptionThreshold, powerConsumptionSurcharge, powerProductionThreshold, powerProductionSurcharge);
        this.tariffState = tariffState;
    }

    public TariffReplyCommand(TariffState tariffState, Long tariffId, Double signupFee, Double baseFee, Double[] powerConsumptionPriceList, Double[] powerProductionPriceList, LocalDateTime contractStartDate, LocalDateTime contractEndDate, Integer minimumContractRuntime, Integer maximumContractRuntime, Double powerConsumptionThreshold, Double powerConsumptionSurcharge, Double powerProductionThreshold, Double powerProductionSurcharge) {
        super(tariffId, signupFee, baseFee, powerConsumptionPriceList, powerProductionPriceList, contractStartDate, contractEndDate, minimumContractRuntime, maximumContractRuntime, powerConsumptionThreshold, powerConsumptionSurcharge, powerProductionThreshold, powerProductionSurcharge);
        this.tariffState = tariffState;
    }

    public TariffState getTariffState() {
        return tariffState;
    }
}
