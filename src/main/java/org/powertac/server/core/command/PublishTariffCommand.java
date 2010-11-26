package org.powertac.server.core.command;

import org.joda.time.LocalDateTime;
import org.powertac.server.core.domain.Broker;
import org.powertac.server.core.domain.Customer;
import org.powertac.server.core.domain.Tariff;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;


@XmlRootElement(name = "publishTariffCommand")
public class PublishTariffCommand {

    @XmlElement
    private String authToken;

    public String getAuthToken() {
        return authToken;
    }

    public Long getTariffId() {
        return tariffId;
    }

    @XmlElement
    private Long tariffId;
    @XmlElement
    private Boolean isDynamic;
    @XmlElement
    private Boolean isNegotiable;

    /*
    * Attributes of Tariff
    */

    @XmlElement
    private BigDecimal powerConsumptionPrice0;     //kWh dependent power consumption fee (>0) / reward (<0) for hour 0
    @XmlElement
    private BigDecimal powerConsumptionPrice1;
    @XmlElement
    private BigDecimal powerConsumptionPrice2;
    @XmlElement
    private BigDecimal powerConsumptionPrice3;
    @XmlElement
    private BigDecimal powerConsumptionPrice4;
    //Todo: 24timeslots

    @XmlElement
    private BigDecimal powerProductionPrice0;
    @XmlElement
    private BigDecimal powerProductionPrice1;
    @XmlElement
    private BigDecimal powerProductionPrice2;
    @XmlElement
    private BigDecimal powerProductionPrice3;
    @XmlElement
    private BigDecimal powerProductionPrice4;
    //Todo: 24timeslots

    @XmlElement
    private BigDecimal signupFee;       //one-time fee (>0) / reward (<0) charged / paid for contract signup
    @XmlElement
    private BigDecimal baseFee;
    @XmlElement
    private Integer minimumContractRuntime;         //null or min days; has to be consistent with contractEndTime - contractStartTime
    @XmlElement
    private Integer maximumContractRuntime;

}
