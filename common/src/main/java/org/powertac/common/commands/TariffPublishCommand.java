package org.powertac.common.commands;

import org.joda.time.LocalDateTime;

import java.io.Serializable;
import java.math.BigDecimal;


public class TariffPublishCommand extends AbstractTariffCommand implements Serializable {

    private static final long serialVersionUID = 1900809883839146303L;

    private String authToken;
    private Long tariffId;

    protected TariffPublishCommand(String authToken, Long tariffId, BigDecimal signupFee, BigDecimal baseFee, BigDecimal[] powerConsumptionPriceList, BigDecimal[] powerProductionPriceList, LocalDateTime contractStartDate, LocalDateTime contractEndDate, Integer minimumContractRuntime, Integer maximumContractRuntime, BigDecimal powerConsumptionThreshold, BigDecimal powerConsumptionSurcharge, BigDecimal powerProductionThreshold, BigDecimal powerProductionSurcharge) {
        super(signupFee, baseFee, powerConsumptionPriceList, powerProductionPriceList, contractStartDate, contractEndDate, minimumContractRuntime, maximumContractRuntime, powerConsumptionThreshold, powerConsumptionSurcharge, powerProductionThreshold, powerProductionSurcharge);
        this.authToken = authToken;
        this.tariffId = tariffId;
    }

    protected TariffPublishCommand(String authToken, Long tariffId, Double signupFee, Double baseFee, Double[] powerConsumptionPriceList, Double[] powerProductionPriceList, LocalDateTime contractStartDate, LocalDateTime contractEndDate, Integer minimumContractRuntime, Integer maximumContractRuntime, Double powerConsumptionThreshold, Double powerConsumptionSurcharge, Double powerProductionThreshold, Double powerProductionSurcharge) {
        super(signupFee, baseFee, powerConsumptionPriceList, powerProductionPriceList, contractStartDate, contractEndDate, minimumContractRuntime, maximumContractRuntime, powerConsumptionThreshold, powerConsumptionSurcharge, powerProductionThreshold, powerProductionSurcharge);
        this.authToken = authToken;
        this.tariffId = tariffId;
    }
}
