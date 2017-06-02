package org.powertac.visualizer.domain;

import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Map;

import org.powertac.common.MarketTransaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Keeps track of wholesale market, 24 timelots at a time.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@JsonInclude(Include.NON_DEFAULT)
public class WholesaleKPIHolder {

    @JsonIgnore
    private TreeMap<Integer, LinkedList<MarketTransaction>> mtxs = null;

    @JsonIgnore
    private LinkedList<Integer> remove = null;

    @JsonProperty("m")
    private double money = 0; // money delta for current timeslot

    @JsonProperty("mwh")
    private double mwh = 0; // energy delta for current timeslot

    @JsonProperty("p")
    private double price = Double.NaN; // mean price per mwh for current timeslot

    @JsonProperty("pb")
    private double priceBuy = Double.NaN; // mean price per sold mwh for current timeslot

    @JsonProperty("ps")
    private double priceSell = Double.NaN; // mean price per bought mwh for current timeslot

    @JsonIgnore
    private boolean empty;

    public WholesaleKPIHolder() {
        super();
        mtxs = new TreeMap<>();
        remove = new LinkedList<>();
    }

    // Note: this creates a different kind of instance: as opposed to the
    // persistent one with a map, this creates derivative ones with info for
    // a particular timeslot, to be sent to the frontend.
    public WholesaleKPIHolder(WholesaleKPIHolder persist, int timeslot) {
        super();
        double total = 0;
        double totalBuy = 0;
        double totalSell = 0;
        mwh = 0;
        money = 0;
        price = 0;
        priceBuy = 0;
        priceSell = 0;
        empty = true;
        for (Map.Entry<Integer, LinkedList<MarketTransaction>> e : persist.mtxs.entrySet()) {
            Integer slot = e.getKey();
            if (slot.intValue() < timeslot) {
                persist.remove.add(slot);
            } else if (slot.intValue() == timeslot) {
                persist.remove.add(slot);
                for (MarketTransaction mtx : e.getValue()) {
                    double m = mtx.getMWh();
                    double p = mtx.getPrice();
                    if (m < 0) {
                        // Sold energy
                        total -= m;
                        totalSell -= m;
                        priceSell -= m * p;
                        money -= m * p;
                    } else if (m > 0) {
                        // Bought energy
                        total += m;
                        totalBuy += m;
                        priceBuy -= m * p;
                        money += m * p;
                    }
                    mwh += m;
                    price -= m * p;
                }
                if (total > 0) {
                    price /= total;
                    empty = false;
                    if (totalBuy > 0) {
                        priceBuy /= totalBuy;
                    } else {
                        priceBuy = Double.NaN;
                    }
                    if (totalSell > 0) {
                        priceSell /= totalSell;
                    } else {
                        priceSell = Double.NaN;
                    }
                } else {
                    price = Double.NaN;
                }
                /* if (price < 0) {
                    System.out.println("Negative price: " + price);
                    for (MarketTransaction mtx : e.getValue()) {
                      System.out.println("  #" + mtx.getTimeslotIndex() + "\t " + mtx.getBroker().getUsername() + "\t " + mtx.getMWh() + " MWh @ " + mtx.getPrice() );
                    }
                } */
            } else {
                break;
            }
        }
    }

    public void resetCurrentValues() {
        for (Integer slot : remove) {
          mtxs.remove(slot);
        }
        remove.clear();
    }

    public void addTransaction(MarketTransaction mtx) {
        Integer timeslot = mtx.getTimeslotIndex();
        LinkedList<MarketTransaction> list = mtxs.get(timeslot);
        if (list == null) {
            list = new LinkedList<>();
            mtxs.put(timeslot, list);
        }
        list.add(mtx);
    }

    public double getMwh() {
        return mwh;
    }

    public double getPrice() {
        return price;
    }

    public double getCash() {
        return money;
    }

    public boolean isEmpty() {
        return empty;
    }
}

