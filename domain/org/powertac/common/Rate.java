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
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.annotations.*;

/**
 * Tariffs are composed of Rates.
 * Rates may be applicable on particular days of the week, particular times
 * of day, or above some usage threshold. Rates may be fixed or variable. 
 * Tariffs and their rates are public information. New tariffs and their Rates
 * are communicated to Customers and to Brokers when tariffs are published.
 * @author jcollins
 */
@XStreamAlias("rate")
public class Rate //implements Serializable
{
  static private Logger log = Logger.getLogger(Rate.class.getName());
  static private Logger stateLog = Logger.getLogger("State");

  @XStreamAsAttribute
  private long id = IdGenerator.createId();
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

  // introduce dependency on TimeService
  @Autowired
  private TimeService timeService;

  /**
   * Default constructor only. You create one of these with the
   * constructor and the builder-style setter methods.
   */
  public Rate (boolean fixed)
  {
    super();
    isFixed = fixed;
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

  /**
   * Process weeklyBegin spec to extract dayOfWeek field
   */
  public Rate setWeeklyBegin (AbstractDateTime begin)
  {
    if (begin != null) {
      weeklyBegin = begin.getDayOfWeek();
    }
    return this;
  }

  /**
   * Process weeklyBegin spec to extract dayOfWeek field
   */
  public Rate setWeeklyBegin (ReadablePartial begin)
  {
    if (begin != null) {
      weeklyBegin = begin.get(DateTimeFieldType.dayOfWeek());
    }
    return this;
  }

  // normal setter also, for Hibernate
  public Rate setWeeklyBegin (int begin)
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
  public Rate setWeeklyEnd (AbstractDateTime end)
  {
    if (end!= null) {
      weeklyEnd= end.getDayOfWeek();
    }
    return this;
  }

  /**
   * Process weeklyEnd spec to extract dayOfWeek field
   */
  public Rate setWeeklyEnd (ReadablePartial end)
  {
    if (end!= null) {
      weeklyEnd= end.get(DateTimeFieldType.dayOfWeek());
    }
    return this;
  }

  // normal setter also
  public Rate setWeeklyEnd (int end)
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
  public Rate setDailyBegin (AbstractDateTime begin)
  {
    if (begin != null) {
      dailyBegin = begin.getHourOfDay();
    }
    return this;
  }

  /**
   * Process dailyBegin specification to extract hourOfDay field
   */
  public Rate setDailyBegin (ReadablePartial begin)
  {
    if (begin != null) {
      dailyBegin = begin.get(DateTimeFieldType.hourOfDay());
    }
    return this;
  }

  // normal setter also
  public Rate setDailyBegin (int begin)
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
  public Rate setDailyEnd (AbstractDateTime end)
  {
    if (end != null) {
      dailyEnd = end.getHourOfDay();
    }
    return this;
  }

  /**
   * Process dailyEnd specification to extract hourOfDay field
   */
  public Rate setDailyEnd (ReadablePartial end)
  {
    if (end != null) {
      dailyEnd = end.get(DateTimeFieldType.hourOfDay());
    }
    return this;
  }

  // normal setter also
  public Rate setDailyEnd (int end)
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
  public Rate setNoticeInterval (Duration interval)
  {
    // we assume that integer division will do the Right Thing here
    noticeInterval = interval.getMillis() / TimeService.HOUR;
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
    boolean result = false;
    if (isFixed) {
      // cannot change this rate
      log.error("Cannot change Rate " + this.toString());
    }
    else {
      Instant now = timeService.getCurrentTime();
      long warning = newCharge.getAtTime().getMillis() - now.getMillis();
      if (warning < noticeInterval) {
        // too late
        log.error("Too late (" + now.getMillis() + ") to change rate for " + newCharge.getAtTime().getMillis());
      }
      else {
        // first, remove the existing charge for the specified time
        HourlyCharge probe = new HourlyCharge(newCharge.getAtTime().plus(1000l), 0);
        SortedSet<HourlyCharge> head = rateHistory.headSet(probe);
        if (head != null && head.size() > 0) {
          HourlyCharge item = head.last();
          if (item.getAtTime() == newCharge.getAtTime());
            rateHistory.remove(item);
        }
        log.info("Adding " + newCharge + " to " + this.toString());
        //println "Adding $newCharge to $this"
        rateHistory.add(newCharge);
        result = true;
      }
    }
    return result;
  }

  public double getTierThreshold ()
  {
    return tierThreshold;
  }

  public Rate setTierThreshold (double tierThreshold)
  {
    this.tierThreshold = tierThreshold;
    return this;
  }

  public double getMinValue ()
  {
    return minValue;
  }

  public Rate setMinValue (double minValue)
  {
    this.minValue = minValue;
    return this;
  }

  public double getMaxValue ()
  {
    return maxValue;
  }

  public Rate setMaxValue (double maxValue)
  {
    this.maxValue = maxValue;
    return this;
  }

  public long getId ()
  {
    return id;
  }

  public boolean isFixed ()
  {
    return isFixed;
  }

  public double getExpectedMean ()
  {
    return expectedMean;
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
  public Rate setValue(double value) {
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
      long horizon = inst.getMillis() - timeService.getCurrentTime().getMillis();
      if (horizon / TimeService.HOUR > noticeInterval) {
        //println "Horizon ${horizon / TimeService.HOUR} > notice interval ${noticeInterval}"
        return expectedMean;
      }
      // otherwise, return the most recent price announcement for the given time
      HourlyCharge probe = new HourlyCharge(inst.plus(1000l), 0);
      SortedSet<HourlyCharge> head = rateHistory.headSet(probe);
      if (head == null || head.size() == 0) {
        return expectedMean; // default
      }
      else {
        return head.last().getValue();
      }
    }
  }

  public String toString ()
  {
    String result = "Rate" + id + ":";
    if (isFixed)
      result += (" Fixed " + getValue());
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
