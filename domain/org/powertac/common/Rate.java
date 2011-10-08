/*
 * Copyright (c) 2011 by the original author or authors.
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

import com.thoughtworks.xstream.annotations.*;

/**
 * Tariffs are composed of Rates.
 * Rates may be applicable on particular days of the week, particular times
 * of day, or above some usage threshold. Rates may be fixed or variable. 
 * Tariffs and their rates are public information. New tariffs and their Rates
 * are communicated to Customers and to Brokers when tariffs are published.
 * @author jcollins
 */
@Domain
@XStreamAlias("rate")
public class Rate //implements Serializable
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
  private boolean isFixed = true; // if true, minValue is fixed rate
  @XStreamAsAttribute
  private double minValue = 0.0; // min and max rate values
  @XStreamAsAttribute
  private double maxValue = 0.0;
  @XStreamAsAttribute
  private long noticeInterval = 0; // notice interval for variable rate in hours
  @XStreamAsAttribute
  private double expectedMean = 0.0; // expected mean value for variable rate
  private TreeSet<HourlyCharge> rateHistory; // history of values for variable rate

  // depends on TimeService
  @XStreamOmitField
  private TimeService timeService;

  /**
   * Default constructor only. You create one of these with the
   * constructor and the builder-style setter methods.
   */
  public Rate ()
  {
    super();
    timeService = (TimeService)SpringApplicationContext.getBean("timeService");
    rateHistory = new TreeSet<HourlyCharge>();
  }
    
//    m?.each { k,v ->
//      if (k == "weeklyBegin")
//        setWeeklyBegin(v) // extract day-of-week
//      else if (k == "weeklyEnd")
//        setWeeklyEnd(v) // extract day-of-week
//      else if (k == "dailyBegin")
//        setDailyBegin(v) // extract hour-of-day
//      else if (k == "dailyEnd")
//        setDailyEnd(v) // extract hour-of-day
//      else if (k == "noticeInterval")
//        setNoticeInterval(v) // truncate to integer hours
//      else if (k == "value")
//        setValue(v)
//      else
//        this."$k" = v }
//    if (weeklyBegin >= 0 && weeklyEnd == -1) {
//      weeklyEnd = weeklyBegin
//    }
//  }

  public long getId ()
  {
    return id;
  }

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
   * Process weeklyBegin spec to extract dayOfWeek field
   */
  public Rate withWeeklyBegin (AbstractDateTime begin)
  {
    if (begin != null) {
      return withWeeklyBegin(begin.getDayOfWeek());
    }
    return this;
  }

  /**
   * Process weeklyBegin spec to extract dayOfWeek field
   */
  public Rate withWeeklyBegin (ReadablePartial begin)
  {
    if (begin != null) {
      return withWeeklyBegin(begin.get(DateTimeFieldType.dayOfWeek()));
    }
    return this;
  }

  /** Fluent setter */
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
   * Process weeklyEnd spec to extract dayOfWeek field
   */
  public Rate withWeeklyEnd (AbstractDateTime end)
  {
    if (end!= null) {
      return withWeeklyEnd(end.getDayOfWeek());
    }
    return this;
  }

  /**
   * Process weeklyEnd spec to extract dayOfWeek field
   */
  public Rate withWeeklyEnd (ReadablePartial end)
  {
    if (end!= null) {
      return withWeeklyEnd(end.get(DateTimeFieldType.dayOfWeek()));
    }
    return this;
  }

  // normal setter also
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
   * Process dailyBegin specification to extract hourOfDay field
   */
  public Rate withDailyBegin (AbstractDateTime begin)
  {
    if (begin != null) {
      return withDailyBegin(begin.getHourOfDay());
    }
    return this;
  }

  /**
   * Process dailyBegin specification to extract hourOfDay field
   */
  public Rate withDailyBegin (ReadablePartial begin)
  {
    if (begin != null) {
      return withDailyBegin(begin.get(DateTimeFieldType.hourOfDay()));
    }
    return this;
  }

  // normal setter also
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
   * Process dailyEnd specification to extract hourOfDay field
   */
  public Rate withDailyEnd (AbstractDateTime end)
  {
    if (end != null) {
      return withDailyEnd(end.getHourOfDay());
    }
    return this;
  }

  /**
   * Process dailyEnd specification to extract hourOfDay field
   */
  public Rate withDailyEnd (ReadablePartial end)
  {
    if (end != null) {
      return withDailyEnd(end.get(DateTimeFieldType.hourOfDay()));
    }
    return this;
  }

  // normal setter also
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
   * Truncate noticeInterval field to integer hours
   */
  public Rate withNoticeInterval (Duration interval)
  {
    // we assume that integer division will do the Right Thing here
    return withNoticeInterval(interval.getMillis() / TimeService.HOUR);
  }

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
   *  allows initial publication of HourlyCharge instances within the notification interval.
   */
  @StateChange
  public boolean addHourlyCharge (HourlyCharge newCharge, boolean publish)
  {
    boolean result = false;
    if (isFixed) {
      // cannot change this rate
      log.error("Cannot change Rate " + this.toString());
    }
    else {
      Instant now = timeService.getCurrentTime();
      long warning = newCharge.getAtTime().getMillis() - now.getMillis();
      if (warning < noticeInterval && !publish) {
        // too late
        log.error("Too late (" + now.toString() + ") to change rate for " + newCharge.getAtTime().toString());
      }
      else {
        // first, remove the existing charge for the specified time
        HourlyCharge probe = new HourlyCharge(newCharge.getAtTime().plus(1000l), 0);
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

  @StateChange
  public Rate withMaxValue (double maxValue)
  {
    this.maxValue = maxValue;
    return this;
  }
  
  public boolean isFixed ()
  {
    return isFixed;
  }

  @StateChange
  public Rate withFixed (boolean fixed)
  {
    isFixed = fixed;
    return this;
  }
  
  public double getExpectedMean ()
  {
    return expectedMean;
  }

  @StateChange
  public Rate withExpectedMean (double value)
  {
    expectedMean = value;
    return this;
  }

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
    return applies(timeService.getCurrentTime());
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
    return applies(usage, timeService.getCurrentTime());
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
   * Allows Hibernate to set the value
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
    //return getValue(Timeslot.currentTimeslot().getStartDateTime())
    return getValue(timeService.getCurrentTime());
  }

  /**
   * Returns the rate for some time in the past or future, regardless of
   * whether the Rate applies at that time, and regardless of whether
   * the requested time is beyond the notification interval of a
   * variable rate.
   */
  public double getValue (AbstractInstant when)
  {
    if (isFixed)
      return minValue;
    else if (rateHistory.size() == 0) {
      log.info("no rate history, return default");
      return expectedMean; // default
    }
    else {
      Instant inst = new Instant(when);
      // if looking beyond the notification interval, return default
      //long horizon = inst.getMillis() - timeService.getCurrentTime().getMillis();
      //if (horizon / TimeService.HOUR > noticeInterval) {
      //  log.info("Horizon " + (horizon / TimeService.HOUR) + " > notice interval " + noticeInterval);
      //  return expectedMean;
      //}
      // otherwise, return the most recent price announcement for the given time
      HourlyCharge probe = new HourlyCharge(inst.plus(1000l), 0);
      SortedSet<HourlyCharge> head = rateHistory.headSet(probe);
      if (head == null || head.size() == 0) {
        log.info("No hourly charge found for " + when.getMillis() + ", returning default");
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

  public String toString ()
  {
    String result = "Rate." + IdGenerator.getString(id) + ":";
    if (isFixed)
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
}
