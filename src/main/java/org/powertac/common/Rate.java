/*
 * Copyright (c) 2011, 2013 by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.common;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.joda.time.*;
import org.joda.time.base.AbstractDateTime;
import org.joda.time.base.AbstractInstant;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.common.state.XStreamStateLoggable;

import com.thoughtworks.xstream.annotations.*;

/**
 * Tariffs are composed of Rates.
 * Rates may be applicable on particular days of the week, particular times
 * of day, or above some usage threshold. Rates may be fixed or variable. 
 * Tariffs and their rates are public information. New tariffs and their Rates
 * are communicated to Customers and to Brokers when tariffs are published.
 * Energy and money quantities in Rates are given from the customer's viewpoint.
 * In other words, a Rate for a consumption tariff will typically specify that
 * the customer pays (negative money value) to recieve energy
 * (positive energy quantity).
 * <p>
 * Each <code>TariffSpecification</code> must include at least one <code>Rate</code>.
 * Rates can be fixed (the default) or variable. A fixed rate has a single
 * <code>value</code> attribute that represents the customer payment for a kWh of energy.
 * This value is typically negative for a consumption tariff (customer pays
 * to receive energy) and positive for a production tariff. A variable rate
 * must specify a <code>minValue</code>, a <code>maxValue</code>, and an 
 * <code>expectedMean</code>. To be valid, a
 * Rate for a consumption tariff must have
 * <code>minValue >= expectedMean >= maxValue</code>.
 * For a production tariff, these relationships are reversed. These ranges
 * constrain the HourlyCharge values that may be applied to the Rate.</p>
 * <p>
 * If a non-zero <code>tierThreshold</code> is given, then the rate applies only after
 * daily consumption/production exceeds the threshold; to achieve a tiered
 * structure, there needs to be at least one <code>Rate</code> with a 
 * <code>tierThreshold</code> of zero, and one
 * for each threshold beyond zero. Tier thresholds must be positive for
 * consumption tariffs, negative for production tariffs. For the purpose of
 * determining tier applicability, production and consumption tracking is
 * reset at midnight every day, in the TariffSubscription.</p>
 * <p>
 * Time-of-use and day-of-week Rates can be specified with
 * <code>dailyBegin</code> / <code>dailyEnd</code> and 
 * <code>weeklyBegin</code> / <code>weeklyEnd</code> specifications. 
 * For <code>dailyBegin</code> / <code>dailyEnd</code>, the values
 * are integer hours in the range 0:23. A <code>Rate</code> that applies from 
 * 22:00 in the evening until 6:00 the next morning would have 
 * <code>dailyBegin=22</code> and <code>dailyEnd=6</code>.
 * Weekly begin/end specifications are integers in the range 1:7, with 1=Monday.</p>
 * <p>
 * It is possible for multiple rates to be applicable at any given combination
 * of time/usage. If this is the case, the most specific rate applies. So if
 * there is a fixed rate that applies all the time, it will be overridden by
 * a time-of-use rate during its period of applibility. Also, if the times for
 * time-of-use rates overlap, they
 * are sorted by start-time, and the applicable rate with the latest start time
 * will apply. This logic is implemented in Tariff.
 * <p>
 * State log fields for readResolve():<br>
 * <code>new(long tariffId, int weeklyBegin, int weeklyEnd,<br>
 * &nbsp;&nbsp;int dailyBegin, int dailyEnd, double tierThreshold,<br>
 * &nbsp;&nbsp;boolean fixed, double minValue, double maxValue,<br>
 * &nbsp;&nbsp;long noticeInterval, double expectedMean, double maxCurtailment)</code>
 * 
 * @author John Collins
 */
@Domain (fields = {"tariffId", "weeklyBegin", "weeklyEnd", "dailyBegin", "dailyEnd",
                   "tierThreshold", "fixed", "minValue", "maxValue",
                   "noticeInterval", "expectedMean", "maxCurtailment"})
@XStreamAlias("rate")
public class Rate extends XStreamStateLoggable
{
  static private Logger log = Logger.getLogger(Rate.class.getName());

  @XStreamAsAttribute
  private long id = IdGenerator.createId();
  @XStreamAsAttribute
  private long tariffId;
  @XStreamAsAttribute
  private int weeklyBegin = -1; // weekly applicability
  @XStreamAsAttribute
  private int weeklyEnd = -1;
  @XStreamAsAttribute
  private int dailyBegin = -1; // daily applicability
  @XStreamAsAttribute
  private int dailyEnd = -1;
  @XStreamAsAttribute
  private double tierThreshold = 0.0; // tier applicability
  @XStreamAsAttribute
  private boolean fixed = true; // if true, minValue is fixed rate
  @XStreamAsAttribute
  private double minValue = 0.0; // min and max rate values
  @XStreamAsAttribute
  private double maxValue = 0.0;
  @XStreamAsAttribute
  private long noticeInterval = 0; // notice interval for variable rate in hours
  @XStreamAsAttribute
  private double expectedMean = 0.0; // expected mean value for variable rate
  @XStreamAsAttribute
  private double maxCurtailment = 0.0; // maximum curtailment for controllable capacity

  private TreeSet<HourlyCharge> rateHistory; // history of values for variable rate

  @XStreamOmitField
  private ProbeCharge probe;

  // depends on TimeService
  @XStreamOmitField
  private TimeService timeService = null;

  /**
   * Default constructor only. You create one of these with the
   * constructor and the fluent-style setter methods.
   */
  public Rate ()
  {
    super();
    rateHistory = new TreeSet<HourlyCharge>();
    probe = new ProbeCharge(new Instant(0l), 0.0);
  }

  /**
   * Returns the id of this Rate
   */
  public long getId ()
  {
    return id;
  }

  /**
   * Returns the id of the TariffSpecification to which this Rate is
   * attached.
   */
  public long getTariffId ()
  {
    return tariffId;
  }
  
  /**
   * Sets the backpointer to the tariff. This is a non-fluent (and non-public)
   * setter, intended to be called by Tariff.
   */
  @StateChange
  void setTariffId (long id)
  {
    tariffId = id;
  }
  
  /**
   * Sets the day of the week on which this Rate comes into effect. The
   * {@code begin} parameter is processed to extract the dayOfWeek field.
   */
  public Rate withWeeklyBegin (AbstractDateTime begin)
  {
    if (begin != null) {
      return withWeeklyBegin(begin.getDayOfWeek());
    }
    return this;
  }

  /**
   * Sets the day of the week on which this Rate comes into effect.
   * Process begin spec to extract dayOfWeek field
   */
  public Rate withWeeklyBegin (ReadablePartial begin)
  {
    if (begin != null) {
      return withWeeklyBegin(begin.get(DateTimeFieldType.dayOfWeek()));
    }
    return this;
  }

  /**
   * Sets the day of the week on which this Rate comes into effect. Note that
   * a value of 1 represents Monday, while 7 represents Sunday.
   */
  @StateChange
  public Rate withWeeklyBegin (int begin)
  {
    weeklyBegin = begin;
    return this;
  }

  public int getWeeklyBegin ()
  {
    return weeklyBegin;
  }

  /**
   * Sets the weekly end of applicability for this Rate,
   * by processing end spec to extract dayOfWeek field.
   */
  public Rate withWeeklyEnd (AbstractDateTime end)
  {
    if (end!= null) {
      return withWeeklyEnd(end.getDayOfWeek());
    }
    return this;
  }

  /**
   * Sets the weekly end of applicability for this Rate,
   * by processing end spec to extract dayOfWeek field.
   */
  public Rate withWeeklyEnd (ReadablePartial end)
  {
    if (end!= null) {
      return withWeeklyEnd(end.get(DateTimeFieldType.dayOfWeek()));
    }
    return this;
  }

  /**
   * Sets the weekly end of applicability for this Rate. A value
   * of 1 represents Monday, and 7 represents Sunday.
   */
  @StateChange
  public Rate withWeeklyEnd (int end)
  {
    weeklyEnd = end;
    return this;
  }

  public int getWeeklyEnd ()
  {
    return weeklyEnd;
  }

  /**
   * Sets the time of day when this Rate comes into effect.
   */
  public Rate withDailyBegin (AbstractDateTime begin)
  {
    if (begin != null) {
      return withDailyBegin(begin.getHourOfDay());
    }
    return this;
  }

  /**
   * Sets the time of day when this Rate comes into effect.
   */
  public Rate withDailyBegin (ReadablePartial begin)
  {
    if (begin != null) {
      return withDailyBegin(begin.get(DateTimeFieldType.hourOfDay()));
    }
    return this;
  }

  /**
   * Sets the time of day when this Rate comes into effect as hours
   * since midnight.
   */
  @StateChange
  public Rate withDailyBegin (int begin)
  {
    dailyBegin = begin;
    return this;
  }

  public int getDailyBegin ()
  {
    return dailyBegin;
  }

  /**
   * Sets the time of day when this Rate is no longer in effect.
   */
  public Rate withDailyEnd (AbstractDateTime end)
  {
    if (end != null) {
      return withDailyEnd(end.getHourOfDay());
    }
    return this;
  }

  /**
   * Sets the time of day when this Rate is no longer in effect.
   */
  public Rate withDailyEnd (ReadablePartial end)
  {
    if (end != null) {
      return withDailyEnd(end.get(DateTimeFieldType.hourOfDay()));
    }
    return this;
  }

  /**
   * Sets the time of day when this Rate is no longer in effect, given
   * as hours since midnight.
   */
  @StateChange
  public Rate withDailyEnd (int end)
  {
    dailyEnd = end;
    return this;
  }

  public int getDailyEnd ()
  {
    return dailyEnd;
  }  

  /**
   * Specifies the minimum interval for rate change notifications for a
   * variable Rate. The value is truncated to integer hours.
   */
  public Rate withNoticeInterval (Duration interval)
  {
    // we assume that integer division will do the Right Thing here
    return withNoticeInterval(interval.getMillis() / TimeService.HOUR);
  }

  /**
   * Specifies the minimum interval in hours for rate change notifications
   * for a variable Rate.
   */
  @StateChange
  public Rate withNoticeInterval (long hours)
  {
    noticeInterval = hours;
    return this;
  }

  public long getNoticeInterval ()
  {
    return noticeInterval;
  }

  /**
   * Adds a new HourlyCharge to a variable rate. If this
   * Rate is not variable, or if the HourlyCharge arrives
   * past its noticeInterval, then we log an error and
   * drop it on the floor. If the update is valid but there's
   * already an HourlyCharge in the specified timeslot, then
   * the update must replace the existing HourlyCharge.
   * Returns true just in case the new charge was added successfully.
   */
  public boolean addHourlyCharge (HourlyCharge newCharge)
  {
    return addHourlyCharge (newCharge, false);
  }
  
  /**
   * Allows initial publication of HourlyCharge instances within the notification interval.
   */
  @StateChange
  public boolean addHourlyCharge (HourlyCharge newCharge, boolean publish)
  {
    boolean result = false;
    if (fixed) {
      // cannot change this rate
      log.error("Cannot change Rate " + this.toString());
    }
    else {
      Instant now = getCurrentTime();
      double sgn = Math.signum(maxValue);
      long warning = newCharge.getAtTime().getMillis() - now.getMillis();
      if (warning < noticeInterval * TimeService.HOUR && !publish) {
        // too late
        log.warn("Too late (" + now.toString() + ") to change rate for " + newCharge.getAtTime().toString());
      }
      else if (sgn * newCharge.getValue() > sgn * maxValue) {
        // charge too high
        log.warn("Excess charge: " + newCharge.getValue() + " > " + maxValue);
      }
      else if (sgn * newCharge.getValue() < sgn * minValue) {
        // charge too low
        log.warn("Charge too low: " + newCharge.getValue() + " < " + minValue);
      }
      else {
        if (probe == null) {
          probe = new ProbeCharge(new Instant(0l), 0.0);
          System.out.println("add: probe was null");
        }
        // first, remove the existing charge for the specified time
        probe.setAtTime(newCharge.getAtTime().plus(1000l));
        //HourlyCharge probe = new HourlyCharge(newCharge.getAtTime().plus(1000l), 0);
        SortedSet<HourlyCharge> head = rateHistory.headSet(probe);
        if (head != null && head.size() > 0) {
          HourlyCharge item = head.last();
          if (item.getAtTime() == newCharge.getAtTime()) {
            log.debug("remove " + item.toString());
            rateHistory.remove(item);
          }
        }
        newCharge.setRateId(id);
        rateHistory.add(newCharge);
        log.info("Adding HourlyCharge " + newCharge.getId() + " at " + newCharge.getAtTime() + " to " + this.toString());
        result = true;
      }
    }
    return result;
  }

  public double getTierThreshold ()
  {
    return tierThreshold;
  }

  /**
   * Sets the usage threshold for applicability of this Rate. The value is
   * interpreted from the Customer's viewpoint, so positive values represent
   * energy consumption in kWh, negative values represent energy production.
   */
  @StateChange
  public Rate withTierThreshold (double tierThreshold)
  {
    this.tierThreshold = tierThreshold;
    return this;
  }

  public double getMinValue ()
  {
    return minValue;
  }

  /**
   * Specifies the minimum charge (closest to zero) for variable Rates.
   * Value should be negative for consumption tariffs, positive for production
   * tariffs.
   */
  @StateChange
  public Rate withMinValue (double minValue)
  {
    this.minValue = minValue;
    return this;
  }

  public double getMaxValue ()
  {
    return maxValue;
  }

  /**
   * Specifies the maximum charge (furthest from zero) for variable Rates.
   * Value should be negative for consumption tariffs, positive for production
   * tariffs.
   */
  @StateChange
  public Rate withMaxValue (double maxValue)
  {
    this.maxValue = maxValue;
    return this;
  }
  
  /**
   * Returns the maximum proportion of offered load or supply that can be 
   * curtailed in a given timeslot.
   */
  public double getMaxCurtailment ()
  {
    return maxCurtailment;
  }
  
  /**
   * Sets the maximum proportion of offered load or supply that can be
   * curtailed. Must be between 0.0 and 1.0. Values > 0.0 are only meaningful
   * for controllable capacities.
   */
  @StateChange
  public Rate withMaxCurtailment (double value)
  {
    maxCurtailment = Math.min(1.0, Math.max(0.0, value));
    return this;
  }
  
  public boolean isFixed ()
  {
    return fixed;
  }

  /**
   * Specifies whether this Rate is fixed (true) or variable (false).
   */
  @StateChange
  public Rate withFixed (boolean fixed)
  {
    this.fixed = fixed;
    return this;
  }
  
  public double getExpectedMean ()
  {
    return expectedMean;
  }

  /**
   * Specifies the expected mean charge/kWh, excluding periodic charges,
   * for this Rate.
   */
  @StateChange
  public Rate withExpectedMean (double value)
  {
    expectedMean = value;
    return this;
  }

  /**
   * Returns the sequence of HourlyCharge instances for this Rate.
   */
  public TreeSet<HourlyCharge> getRateHistory ()
  {
    return rateHistory;
  }

  /**
   * True just in case this Rate applies at this moment, ignoring the
   * tier.
   */
  public boolean applies ()
  {
    return applies(getCurrentTime());
  }

  /**
   * True just in case this Rate applies at the given DateTime, ignoring the
   * tier.
   */
  public boolean applies (AbstractInstant when)
  {
    boolean appliesWeekly = false;
    boolean appliesDaily = false;
    DateTime time = new DateTime(when, DateTimeZone.UTC);

    // check weekly applicability
    int day = time.getDayOfWeek();
    if (weeklyBegin == -1) {
      appliesWeekly = true;
    }
    else if (weeklyEnd == -1) {
      appliesWeekly = (day == weeklyBegin);
    }
    else if (weeklyEnd >= weeklyBegin) {
      appliesWeekly = (day >= weeklyBegin && day <= weeklyEnd);
    }
    else {
      appliesWeekly = (day >= weeklyBegin || day <= weeklyEnd);
    }

    // check daily applicability
    int hour = time.getHourOfDay();
    if (dailyBegin == -1 || dailyEnd == -1) {
      appliesDaily = true;
    }
    else if (dailyEnd > dailyBegin) {
      // Interval does not span midnight
      appliesDaily = ((hour >= dailyBegin) && (hour < dailyEnd));
    }
    else {
      // Interval spans midnight
      appliesDaily = ((hour >= dailyBegin) || (hour < dailyEnd));
    }

    return (appliesWeekly && appliesDaily);
  }

  /**
   * True just in case this Rate applies at this moment, for the
   * indicated usage tier.
   */
  public boolean applies (double usage)
  {
    return applies(usage, getCurrentTime());
  }

  /**
   * True just in case this Rate applies at the specified
   * time, for the indicated usage tier.
   */
  public boolean applies (double usage, AbstractInstant when)
  {
    if (usage >= tierThreshold) {
      return applies(when);
    }
    else {
      return false;
    }
  }

  /**
   * Specifies the charge/kWh for a fixed rate, from the customer's viewpoint.
   * Negative values represent customer debits, while positive values 
   * represent customer credits.
   */
  @StateChange
  public Rate withValue(double value) {
    minValue = value;
    return this;
  }

  /**
   * Returns the rate for the current time. Note that the value is returned
   * even in case the Rate does not apply at the current time or current
   * usage tier. For variable rates, the value returned during periods of
   * inapplicability is meaningless, of course.
   */
  public double getValue ()
  {
    return getValue(getCurrentTime(), null);
  }
  
  /**
   * Shortcut to get value at an instant without a TEH.
   */
  public double getValue (AbstractInstant when)
  {
    return getValue(when, null);
  }

  /**
   * Returns the rate for some time in the past or future, regardless of
   * whether the Rate applies at that time, and regardless of whether
   * the requested time is beyond the notification interval of a
   * variable rate. If helper is given, and this rate is not fixed, and
   * there is not an HourlyCharge for the requested timeslot, then 
   * the helper is used to produce the value. 
   */
  public double getValue (AbstractInstant when,
                          TariffEvaluationHelper helper)
  {
    if (fixed)
      return minValue;
    else if (null != helper) {
      return helper.getWeightedValue(this);
    }
    else if (rateHistory.size() == 0) {
      log.debug("no rate history, return default");
      return expectedMean;
    }
    else {
      if (probe == null) {
        probe = new ProbeCharge(new Instant(0l), 0.0);
      }
      Instant inst = new Instant(when);
      // return the most recent price announcement for the given time
      probe.setAtTime(inst.plus(1000l));
      SortedSet<HourlyCharge> head = rateHistory.headSet(probe);
      if (head == null || head.size() == 0) {
        log.debug("No hourly charge found for " + when.getMillis() + ", returning default");
        return expectedMean; // default
      }
      else {
        HourlyCharge candidate = head.last();
        if (candidate.getAtTime().getMillis() == inst.getMillis()) {
          return candidate.getValue();
        }
        else {
          return expectedMean; // default
        }
      }
    }
  }
  
  /**
   * Returns true just in case this Rate is internally valid, and valid
   * with respect to the given TariffSpecification. 
   * For all Rates, maxCurtailment is between 0.0 and 1.0.
   * For a CONSUMPTION tariff, tierThreshold must be non-negative, while
   * for a PRODUCTION tariff, tierThreshold must be non-positive.
   * For a non-fixed rate, maxValue must be at least as "large"
   * as minValue, where "larger" means more negative for a CONSUMPTION
   * tariff, and more positive for a PRODUCTION tariff. Also, expectedMean
   * must be between minValue and maxValue, and noticeInterval must be
   * non-negative. 
   */
  public boolean isValid(TariffSpecification spec)
  {
    // numeric sanity test
    if (Double.isNaN(minValue) || Double.isNaN(maxValue) || Double.isNaN(expectedMean)) {
      log.warn("numeric insanity: (" + minValue + "," + maxValue + "," + expectedMean);
      return false;
    }
    // curtailment test
    if (maxCurtailment < 0.0 || maxCurtailment > 1.0) {
      log.warn("Curtailment ratio " + maxCurtailment + " out of range");
      return false;
    }
    // tier tests
    if (spec.getPowerType().isConsumption() && tierThreshold < 0.0) {
      log.warn("Negative tier threshold for consumption rate");
      return false;
    }
    if (spec.getPowerType().isProduction() && tierThreshold > 0.0) {
      log.warn("Positive tier threshold for production rate");
      return false;
    }
    // non-fixed rates
    if (isFixed())
      return true;
    double sgn = spec.getPowerType().isConsumption()? -1.0: 1.0;
    // maxValue
    if (sgn * maxValue < sgn * minValue) {
      log.warn("maxValue " + maxValue + " out of range");
      return false;
    }
    // expectedMean
    if (sgn * expectedMean < sgn * minValue || sgn * expectedMean > sgn * maxValue) {
      log.warn("expectedMean " + expectedMean + " out of range");
      return false;
    }
    // noticeInterval
    if (noticeInterval < 0l) {
      log.warn("negative notice interval " + noticeInterval);
      return false;
    }
    return true;
  }

  @Override
  public String toString ()
  {
    String result = "Rate." + IdGenerator.getString(id) + ":";
    if (fixed)
      result += (" Fixed " + getMinValue());
    else
      result += " Variable";
    if (weeklyBegin >= 0) {
      result += (", " + (weeklyEnd >= 0 ? "starts " : "") + "day" + weeklyBegin);
      if (weeklyEnd >= 0) {
        result += (" ends day ${weeklyEnd}");
      }
    }
    if (dailyBegin >= 0) {
      result += (", " + dailyBegin + ":00 -- " + dailyEnd + ":00");
    }
    if (tierThreshold > 0.0) {
      result += (", usage > " + tierThreshold);
    }
    return result;
  }
  
  // retrieves current time
  private Instant getCurrentTime ()
  {
    if (timeService == null)
      timeService = (TimeService)SpringApplicationContext.getBean("timeService");
    return timeService.getCurrentTime();
  }
  
  class ProbeCharge extends HourlyCharge
  {
    public ProbeCharge (Instant when, double charge)
    {
      super(when, charge);
    }
   
    void setAtTime (Instant when)
    {
      atTime = when;
    }
  }
}
