package org.powertac.visualizer.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Keeps track of customer status and usage. Usage is stored per-customer-unit,
 * but reported as the product of the per-customer quantity and the subscribed
 * population. This allows the broker to use historical usage data as the
 * subscribed population shifts.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@JsonInclude(Include.NON_DEFAULT)
public class RetailKPIHolder {

    @JsonProperty("sub")
    private int subscribedPopulation;
    private double kwh;
    @JsonProperty("m")
    private double money;
    @JsonProperty("actTx")
    private int activeTariffs;
    @JsonProperty("rvkTx")
    private int revokedTariffs;
    @JsonProperty("pubTx")
    private int publishedTariffs;

    public RetailKPIHolder() {
        super();
    }

    public RetailKPIHolder(RetailKPIHolder retailKPIHolder) {
        subscribedPopulation = retailKPIHolder.getSubscribedPopulation();
        kwh = retailKPIHolder.getKwh();
        money = retailKPIHolder.getMoney();
        activeTariffs = retailKPIHolder.getActiveTariffs();
        revokedTariffs = retailKPIHolder.getRevokedTariffs();
        publishedTariffs = retailKPIHolder.getPublishedTariffs();
    }

    // Adds new individuals to the count
    public void signup(int population) {
        subscribedPopulation += population;
    }

    // Removes individuals from the count
    public void withdraw(int population) {
        subscribedPopulation -= population;
    }

    // VizCustomer produces or consumes power. We assume the kwh value is
    // negative
    // for production, positive for consumption
    public void produceConsume(double txKwh, double txMoney) {
        kwh += txKwh;
        money += txMoney;
    }

    public void resetCurrentValues() {
        subscribedPopulation = 0;
        kwh = 0.0;
        money = 0.0;
        revokedTariffs = 0;
        publishedTariffs = 0;
    }

    public void incrementRevokedTariffs() {
        revokedTariffs++;
    }

    public void incrementPublishedTariffs() {
        publishedTariffs++;
    }

    public int getSubscribedPopulation() {
        return subscribedPopulation;
    }

    public void setSubscribedPopulation(int subscribedPopulation) {
        this.subscribedPopulation = subscribedPopulation;
    }

    public double getKwh() {
        return kwh;
    }

    public void setKwh(Double kwh) {
        this.kwh = kwh;
    }

    public Double getMoney() {
        return money;
    }

    public void setMoney(Double money) {
        this.money = money;
    }

    public int getActiveTariffs() {
        return activeTariffs;
    }

    public void setActiveTariffs(int activeTariffs) {
        this.activeTariffs = activeTariffs;
    }

    public int getRevokedTariffs() {
        return revokedTariffs;
    }

    public void setRevokedTariffs(int revokedTariffs) {
        this.revokedTariffs = revokedTariffs;
    }

    public int getPublishedTariffs() {
        return publishedTariffs;
    }

    public void setPublishedTariffs(int publishedTariffs) {
        this.publishedTariffs = publishedTariffs;
    }

    @Override
    public String toString() {
        return "RetailKPIHolder [subscribedPopulationCum="
                + ", subscribedPopulation=" + subscribedPopulation + ", kwh="
                + kwh + ", money=" + money + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RetailKPIHolder other = (RetailKPIHolder) obj;
        if (activeTariffs != other.activeTariffs) {
            return false;
        }
        if (Double.doubleToLongBits(kwh) != Double.doubleToLongBits(other.kwh)) {
            return false;
        }
        if (Double.doubleToLongBits(money) != Double.doubleToLongBits(other.money)) {
            return false;
        }
        if (publishedTariffs != other.publishedTariffs) {
            return false;
        }
        if (revokedTariffs != other.revokedTariffs) {
            return false;
        }
        if (subscribedPopulation != other.subscribedPopulation) {
            return false;
        }
        return true;
    }
}
