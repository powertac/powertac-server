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
import org.powertac.common.enumerations.CustomerType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;


/**
 * Command object that represents a published tariff
 * which is sent out to all customers and all brokers
 * in the competition. Customers may respond send their
 * response as a <code>TariffReplyCommand</code> in order
 * to either subscribe to this tariff instance or to
 * start negotiatiating the conditions.
 *
 * Once created this command object is immutable.
 *
 * @author Carsten Block
 * @version 1.0, Date: 01.12.10
 * @see TariffReplyCommand, AbstractTariffCommand
 */
public class TariffPublishCommand extends AbstractTariffCommand implements Serializable {

    private static final long serialVersionUID = 1900809883839146303L;

    private Set<CustomerType> permittedCustomerTypes;
    private String authToken;

    public TariffPublishCommand(Set<CustomerType> permitCustomerTypes, String authToken, Long tariffId, BigDecimal signupFee, BigDecimal baseFee, BigDecimal[] powerConsumptionPriceList, BigDecimal[] powerProductionPriceList, LocalDateTime contractStartDate, LocalDateTime contractEndDate, Integer minimumContractRuntime, Integer maximumContractRuntime, BigDecimal powerConsumptionThreshold, BigDecimal powerConsumptionSurcharge, BigDecimal powerProductionThreshold, BigDecimal powerProductionSurcharge) {
        super(tariffId, signupFee, baseFee, powerConsumptionPriceList, powerProductionPriceList, contractStartDate, contractEndDate, minimumContractRuntime, maximumContractRuntime, powerConsumptionThreshold, powerConsumptionSurcharge, powerProductionThreshold, powerProductionSurcharge);
        this.permittedCustomerTypes = permittedCustomerTypes;
        this.authToken = authToken;
    }

    public TariffPublishCommand(Set<CustomerType> permitCustomerTypes, String authToken, Long tariffId, Double signupFee, Double baseFee, Double[] powerConsumptionPriceList, Double[] powerProductionPriceList, LocalDateTime contractStartDate, LocalDateTime contractEndDate, Integer minimumContractRuntime, Integer maximumContractRuntime, Double powerConsumptionThreshold, Double powerConsumptionSurcharge, Double powerProductionThreshold, Double powerProductionSurcharge) {
        super(tariffId, signupFee, baseFee, powerConsumptionPriceList, powerProductionPriceList, contractStartDate, contractEndDate, minimumContractRuntime, maximumContractRuntime, powerConsumptionThreshold, powerConsumptionSurcharge, powerProductionThreshold, powerProductionSurcharge);
        this.permittedCustomerTypes = permittedCustomerTypes;
        this.authToken = authToken;
    }

    public Set<CustomerType> getPermittedCustomerTypes() {
        return permittedCustomerTypes;
    }

    public String getAuthToken() {
        return authToken;
    }
}
