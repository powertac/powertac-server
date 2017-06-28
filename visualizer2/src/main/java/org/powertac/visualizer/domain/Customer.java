package org.powertac.visualizer.domain;

import java.util.List;

import org.powertac.common.CustomerInfo;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

/**
 *
 * This entity represents a certain customer population within Power TAC game.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Customer {

    private static long idCounter = 0;
    private long id = idCounter++;

    private long idCustomerInfo;

    /** Name of the customer model */
    private String name;

    /** population represented by this model */
    private int population;

    private String powerType;

    private double controllableKW;

    private double upRegulationKW;

    private double downRegulationKW;

    private double storageCapacity;

    /**
     * True just in case this customer can engage in multiple contracts at the
     * same time. Defaults to false.
     */
    private boolean multiContracting = false;

    /**
     * True just in case this customer negotiates over contracts. Defaults to
     * false.
     */
    private boolean canNegotiate = false;

    // relevant field from PowerType:
    private String genericPowerType;

    // Don't include this in  the messages
    @JsonIgnore
    private List<Double> bootstrapNetUsage;

    private RetailKPIHolder retail = new RetailKPIHolder();

    private String customerClass;

    public Customer() {

    }

    public Customer(CustomerInfo info) {
        this.idCustomerInfo = info.getId();
        this.name = info.getName();
        this.population = info.getPopulation();
        this.powerType = info.getPowerType().toString();
        this.controllableKW = info.getControllableKW();
        this.upRegulationKW = info.getUpRegulationKW();
        this.downRegulationKW = info.getDownRegulationKW();
        this.storageCapacity = info.getStorageCapacity();
        this.multiContracting = info.isMultiContracting();
        this.canNegotiate = info.isCanNegotiate();
        this.genericPowerType = info.getPowerType().getGenericType().toString();
        if (info.getCustomerClass() != null) {
            this.customerClass = info.getCustomerClass().toString();
        }
    }

    public long getId() {
        return id;
    }

    public long getIdCustomerInfo() {
        return idCustomerInfo;
    }

    public String getCustomerClass() {
        return customerClass;
    }

    public String getName() {
        return name;
    }

    public int getPopulation() {
        return population;
    }

    public String getPowerType() {
        return powerType;
    }

    public double getControllableKW() {
        return controllableKW;
    }

    public double getUpRegulationKW() {
        return upRegulationKW;
    }

    public double getDownRegulationKW() {
        return downRegulationKW;
    }

    public double getStorageCapacity() {
        return storageCapacity;
    }

    public boolean isMultiContracting() {
        return multiContracting;
    }

    public boolean isCanNegotiate() {
        return canNegotiate;
    }

    public String getGenericPowerType() {
        return genericPowerType;
    }

    public List<Double> getBootstrapNetUsage() {
        return bootstrapNetUsage;
    }

    public void setBootstrapNetUsage(List<Double> bootstrapNetUsage) {
        this.bootstrapNetUsage = bootstrapNetUsage;
    }

    public RetailKPIHolder getRetail() {
        return retail;
    }

    public void setRetail(RetailKPIHolder retailKPIHolder) {
        this.retail = retailKPIHolder;
    }

    public static void recycle() {
        idCounter = 0;
    }
}
