package org.powertac.visualizer.domain.broker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.powertac.common.CustomerInfo;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;

import com.google.gson.Gson;

/**
 * Holds the data for broker's tariff.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffData
{
  private Logger log = Logger.getLogger(TariffData.class);
  private BrokerModel broker;
  private TariffSpecification spec;

  private double profit;
  private double netKWh;
  private long customers;
  private String powerType;
  private String ratesGraph;
  Gson gson = new Gson();
  private ConcurrentHashMap<CustomerInfo, TariffCustomerStats> tariffCustomerStats;

  public TariffData (TariffSpecification spec, BrokerModel broker)
  {
    this.spec = spec;
    this.broker = broker;
    tariffCustomerStats =
      new ConcurrentHashMap<CustomerInfo, TariffCustomerStats>(20, 0.75f, 1);
    powerType = spec.getPowerType().toString();
    createRatesGraph();
  }

  public double getNetKWh ()
  {
    return Math.round(netKWh);
  }

  public long getCustomers ()
  {
    return customers;
  }

  public double getProfit ()
  {
    return Math.round(profit);
  }

  public BigDecimal getProfitInThousandsOfEuro ()
  {
    return new BigDecimal(profit / 1000).setScale(2, RoundingMode.HALF_UP);
  }

  public TariffSpecification getSpec ()
  {
    return spec;
  }

  public ConcurrentHashMap<CustomerInfo, TariffCustomerStats>
    getTariffCustomerStats ()
  {
    return tariffCustomerStats;
  }

  public void processTariffTx (TariffTransaction tx)
  {
    profit += tx.getCharge();
    netKWh += tx.getKWh();
    if (tx.getCustomerInfo() != null) { // otherwise this tx is most likely to
                                        // be PUBLISH
      tariffCustomerStats.putIfAbsent(tx.getCustomerInfo(),
                                      new TariffCustomerStats(tx
                                              .getCustomerInfo(), spec));
      tariffCustomerStats.get(tx.getCustomerInfo()).addAmounts(tx.getCharge(),
                                                               tx.getKWh());
    }

  }

  public void setCustomers (long customers)
  {
    this.customers += customers;
  }

  public String getPowerType ()
  {
    return powerType;
  }

  public BrokerModel getBroker ()
  {
    return broker;
  }

  public BigDecimal getNetMWh ()
  {
    return new BigDecimal(netKWh / 1000).setScale(2, RoundingMode.HALF_UP);
  }

  public String toString ()
  {
    return tariffCustomerStats.toString();
  }

  private void createRatesGraph ()
  {
    ArrayList<Object> series = new ArrayList<Object>();
    Calendar calendar = GregorianCalendar.getInstance();

    // for every rate in TariffSpecification
    for (Rate rate: spec.getRates()) {
      // check if day of the week is specified
      if (rate.getWeeklyBegin() != -1) {
        ArrayList<Object> data = new ArrayList<Object>();
        // For start we only need the day of the week; January 2007 is used
        // since the days for January 2007 correspond to days in Rate class
        // (1 == monday, ...)
        calendar.clear();
        calendar.set(2007, 0, rate.getWeeklyBegin(),
                     rate.getDailyBegin() >= 0? rate.getDailyBegin() + 1: 0, 0,
                     0);
        Object[] start =
          { calendar.getTimeInMillis(), rate.getMinValue() * 100 };

        data.add(start);
        calendar.clear();
        calendar.set(2007, 0, rate.getWeeklyEnd() >= 0? rate.getWeeklyEnd()
                                                      : rate.getWeeklyBegin(),
                     rate.getDailyEnd() >= 0? rate.getDailyEnd() + 1: 23, 0, 0);
        Object[] end = { calendar.getTimeInMillis(), rate.getMinValue() * 100 };
        data.add(end);
        series.add(new RatesGraphTemplate(data));
      }

      else if (rate.getDailyBegin() != -1 && rate.getDailyEnd() != -1) {
        ArrayList<Object> data = new ArrayList<Object>();
        calendar.clear();
        calendar.set(2006, 0, 1, rate.getDailyBegin() + 1, 0, 0);
        Object[] start =
          { calendar.getTimeInMillis(), rate.getMinValue() * 100 };
        data.add(start);
        calendar.clear();
        calendar.set(2006, 0, 1, rate.getDailyEnd() + 1, 0, 0);
        Object[] end = { calendar.getTimeInMillis(), rate.getMinValue() * 100 };
        data.add(end);
        series.add(new RatesGraphTemplate(data));
      }

      else if (rate.getDailyBegin() == -1 && rate.getDailyEnd() == -1
               && rate.getWeeklyBegin() == -1 && rate.getWeeklyEnd() == -1) {
        ArrayList<Object> data = new ArrayList<Object>();
        calendar.clear();
        calendar.set(2007, 0, 1, 1, 0, 0);
        Object[] start =
          { calendar.getTimeInMillis(), rate.getMinValue() * 100 };
        data.add(start);
        calendar.clear();
        calendar.set(2007, 0, 7, 1, 0, 0);
        Object[] end = { calendar.getTimeInMillis(), rate.getMinValue() * 100 };
        data.add(end);
        series.add(new RatesGraphTemplate(data));
      }

      else {
        log.info("NO template in TariffData!");
      }
    }
    ratesGraph = gson.toJson(series);
  }

  private class RatesGraphTemplate
  {
    ArrayList<Object> data;

    public RatesGraphTemplate (ArrayList<Object> data)
    {
      this.data = data;
    }
  }

  public String getRatesGraph ()
  {
    return ratesGraph;
  }

}
