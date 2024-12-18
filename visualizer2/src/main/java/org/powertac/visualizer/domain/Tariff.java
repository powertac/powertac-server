package org.powertac.visualizer.domain;

import org.powertac.common.TariffSpecification;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 *
 * Copy of {@link org.powertac.common.TariffSpecification}}
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 *
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Tariff {

    private static long idCounter = 0;
    private long id = idCounter++;

    private Broker broker;

    private long tariffSpecId;

    private boolean active = true;

    /**
     * Last date new subscriptions will be accepted. Null means never expire.
     */
    private Long expiration = null;

    /** Minimum contract duration (in milliseconds) */
    private long minDuration = 0l;

    /** Type of power covered by this tariff */
    private String powerType;

    /**
     * One-time payment for subscribing to tariff, positive for payment to
     * customer, negative for payment from customer.
     */
    private double signupPayment = 0.0;

    /**
     * Payment from customer to broker for canceling subscription before
     * minDuration has elapsed.
     */
    private double earlyWithdrawPayment = 0.0;

    /** Flat payment per period for two-part tariffs */
    private double periodicPayment = 0.0;

    private RetailKPIHolder retailKPIHolder = new RetailKPIHolder();

    public Tariff() {

    }

    public Tariff(Broker broker, TariffSpecification spec) {
        super();
        this.broker = broker;
        this.tariffSpecId = spec.getId();
        this.expiration = spec.getExpiration() == null ? -1 : spec.getExpiration().toInstant().toEpochMilli();
        this.minDuration = spec.getMinDuration();
        this.powerType = spec.getPowerType().toString();
        this.signupPayment = spec.getSignupPayment();
        this.earlyWithdrawPayment = spec.getEarlyWithdrawPayment();
        this.periodicPayment = spec.getPeriodicPayment();
    }

    public long getTariffSpecId() {
        return tariffSpecId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public Broker getBroker() {
        return broker;
    }

    public Long getExpiration() {
        return expiration;
    }

    public long getMinDuration() {
        return minDuration;
    }

    public String getPowerType() {
        return powerType;
    }

    public double getSignupPayment() {
        return signupPayment;
    }

    public double getEarlyWithdrawPayment() {
        return earlyWithdrawPayment;
    }

    public double getPeriodicPayment() {
        return periodicPayment;
    }

    public RetailKPIHolder getRetail() {
        return retailKPIHolder;
    }

    public void setRetail(RetailKPIHolder retailKPIHolder) {
        this.retailKPIHolder = retailKPIHolder;
    }

    public static void recycle() {
        idCounter = 0;
    }
}
