package org.powertac.visualizer.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    private boolean empty;

    public RetailKPIHolder() {
        super();
        resetCurrentValues();
    }

    public RetailKPIHolder(RetailKPIHolder retailKPIHolder) {
        subscribedPopulation = retailKPIHolder.getSubscribedPopulation();
        kwh = retailKPIHolder.getKwh();
        money = retailKPIHolder.getMoney();
        activeTariffs = retailKPIHolder.getActiveTariffs();
        revokedTariffs = retailKPIHolder.getRevokedTariffs();
        publishedTariffs = retailKPIHolder.getPublishedTariffs();
        empty = retailKPIHolder.empty;
    }

    public void resetCurrentValues() {
        subscribedPopulation = 0;
        kwh = 0.0;
        money = 0.0;
        revokedTariffs = 0;
        publishedTariffs = 0;
        empty = true;
    }

    // Adds new individuals to the count
    public void signup(int population) {
        subscribedPopulation += population;
        empty = empty && subscribedPopulation == 0;
    }

    // Removes individuals from the count
    public void withdraw(int population) {
        subscribedPopulation -= population;
        empty = empty && subscribedPopulation == 0;
    }

    // VizCustomer produces or consumes power. We assume the kwh value is
    // negative
    // for production, positive for consumption
    public void produceConsume(double txKwh, double txMoney) {
        kwh += txKwh;
        money += txMoney;
        empty = empty && kwh == 0.0 && money == 0.0;
    }

    public void incrementRevokedTariffs() {
        revokedTariffs++;
        empty = false;
    }

    public void incrementPublishedTariffs() {
        publishedTariffs++;
        empty = false;
    }

    public int getSubscribedPopulation() {
        return subscribedPopulation;
    }

    public void setSubscribedPopulation(int population) {
        subscribedPopulation = population;
        empty = empty && subscribedPopulation == 0;
    }

    public double getKwh() {
        return kwh;
    }

    public void setKwh(double kwh) {
        this.kwh = kwh;
        empty = empty && kwh == 0.0;
    }

    public Double getMoney() {
        return money;
    }

    public void setMoney(double money) {
        this.money = money;
        empty = empty && money == 0.0;
    }

    public int getActiveTariffs() {
        return activeTariffs;
    }

    public void setActiveTariffs(int activeTariffs) {
        this.activeTariffs = activeTariffs;
        empty = empty && activeTariffs == 0;
    }

    public int getRevokedTariffs() {
        return revokedTariffs;
    }

    public void setRevokedTariffs(int revokedTariffs) {
        this.revokedTariffs = revokedTariffs;
        empty = empty && revokedTariffs == 0;
    }

    public int getPublishedTariffs() {
        return publishedTariffs;
    }

    public void setPublishedTariffs(int publishedTariffs) {
        this.publishedTariffs = publishedTariffs;
        empty = empty && publishedTariffs == 0;
    }

    @Override
    public String toString() {
        return "RetailKPIHolder [subscribedPopulationCum="
                + ", subscribedPopulation=" + subscribedPopulation + ", kwh="
                + kwh + ", money=" + money + "]";
    }

    public boolean isEmpty() {
      return empty;
    }

}
