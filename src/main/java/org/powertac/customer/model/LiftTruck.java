/*
 * Copyright (c) 2014 by the original author
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
package org.powertac.customer.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.Timeslot;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.customer.AbstractCustomer;

/**
 * Models the complement of lift trucks in a warehouse. There may be
 * multiple trucks, a number of battery packs, and a daily/weekly work
 * schedule. We assume that each truck needs one battery while it's running.
 * 
 * @author John Collins
 */
public class LiftTruck
{
  static private Logger log =
      Logger.getLogger(LiftTruck.class.getName());

  // ==== static constants ====
  private static final int HOURS_DAY = 24;
  private static final int DAYS_WEEK = 7;
  private static final long HOUR = 360*1000;

  // need a name so we can configure it
  private String name;

  @ConfigurableValue(valueType = "Double",
      description = "power usage when truck is in use")
  private double truckKW = 4.0;

//  @ConfigurableValue(valueType = "Integer",
//      description = "number of trucks in facility")
//  private int nTrucks = 10;

  @ConfigurableValue(valueType = "Integer",
      description = "number of battery localChargers")
  private int nChargers = 8;

  @ConfigurableValue(valueType = "Double",
      description = "battery capacity per truck")
  private double batteryCapacity = 50.0;

  @ConfigurableValue(valueType = "Double",
      description = "maximum chargers rate of one truck's battery pack")
  private double maxChargeKW = 6.0;

  @ConfigurableValue(valueType = "Double",
      description = "ratio of charge energy to battery energy")
  private double chargeEfficiency = 0.9;

  @ConfigurableValue(valueType = "Integer",
      description = "planning horizon in timeslots - should be at least 48")
  private double planningHorizon = 48;

  // ==== Shift data ====
  // These List values are configured through their setter methods.
  // The default value is set in the constructor to serialize the
  // construction-configuration process
  private List<String> defaultShiftData =
      Arrays.asList("8", "8", "16", "8", "0", "8");
  private List<Shift> shifts;

  private List<Double> trucksInUseWeekday =
      Arrays.asList(9.0, 5.0, 1.2);

  private List<Double> trucksInUseWeekend =
      Arrays.asList(4.0, 1.0, 0.5);

  private double[] defaultStateOfCharge =
    {50.0,48.0,25.0,22.0,20.0,18.0,16.0,
     14.0,12.0,10.0,8.0,6.0,4.0,2.0,1.5,1.0}; // one entry per battery

  // ==== Current state ====
  private BatteryState[] batteryState;
  private BatteryState[] chargers;
  private CapacityPlan plan;
  private RandomSeed rs;

  // context references
  private AbstractCustomer customer;
  private TimeslotRepo timeslotRepo;

  /**
   * Standard constructor for named configurable type
   */
  public LiftTruck (String name)
  {
    super();
    this.name = name;
  }

  /**
   * Initialization must provide accessor to Customer instance and time.
   * We assume configuration has already happened. We also start with no
   * active trucks, and the weakest batteries on chargers. Trucks will not
   * be active (in bootstrap mode) until the first shift change.
   */
  public void initialize (AbstractCustomer customer,
                          TimeslotRepo timeslotRepo,
                          RandomSeed seed)
  {
    this.customer = customer;
    this.timeslotRepo = timeslotRepo;
    this.rs = seed;

    // use default values when not configured
    ensureShifts();
    ensureBatteryState();

    // validate config data
    validateShifts();
    // At this point, we have the shifts and the correct numbers of trucks.
    // Next we populate the shifts with truck usage data
    populateShifts();

    // make sure we have enough batteries and chargers
    validateBatteries();
    validateChargers();

    // put the weakest batteries on chargers
    chargers = new BatteryState[nChargers];
    BatteryState[] sortedBatteries = getSortedBatteryState();
    for (int i = 0; i < chargers.length; i++) {
      chargers[i] = sortedBatteries[i];
    }
    //TariffSubscription sub = customer.getCurrentSubscriptions().get(0);
  }

  // use default data if unconfigured
  void ensureShifts ()
  {
    if (null == shifts) {
      setShiftData(defaultShiftData);
    }
  }

  // use default data if unconfigured
  void ensureBatteryState ()
  {
    if (null == batteryState) {
      setDefaultStateOfCharge();
    }
  }

  // ensure correspondence between shifts and trucksInuse,
  // copy usage data into Shift instances
  void validateShifts ()
  {
    int weekdayError = shifts.size() - trucksInUseWeekday.size();
    if (weekdayError > 0) {
      // not enough usage records
      log.error("not enough weekday records for " + getName() + ", adding "
               + weekdayError + " zeros");
      while (weekdayError > 0) {
        trucksInUseWeekday.add(0.0);
        weekdayError -= 1;
      }
    }
    else if (weekdayError < 0) {
      log.error("Too many weekday records for " + getName() + ", removing "
                + weekdayError + " records");
      while (weekdayError < 0) {
        trucksInUseWeekday.remove(trucksInUseWeekday.size() - 1);
        weekdayError += 1;
      }
    }
    int weekendError = shifts.size() - trucksInUseWeekend.size();
    if (weekendError > 0) {
      // not enough usage records
      log.warn("not enough weekend records for " + getName() + ", adding "
               + weekendError + " zeros");
      while (weekendError > 0) {
        trucksInUseWeekend.add(0.0);
        weekendError -= 1;
      }
    }
    else if (weekendError < 0) {
      log.error("Too many weekend records for " + getName() + ", removing "
                + weekendError + " records");
      while (weekendError < 0) {
        trucksInUseWeekend.remove(trucksInUseWeekend.size() - 1);
        weekendError += 1;
      }
    }
  }

  void populateShifts ()
  {
    // Update the shift instances.
    for (int i = 0; i < shifts.size(); i++) {
      shifts.get(i).setWeekdayTrucks(trucksInUseWeekday.get(i));
      shifts.get(i).setWeekendTrucks(trucksInUseWeekend.get(i));
    }
  }

  // We have to ensure that there are enough batteries to 
  // support the shift schedule. There must be at least enough batteries to
  // supply the two largest adjacent shifts, and there must also be enough
  // to power the two largest adjacent shifts, in case a single battery
  // cannot power an entire shift.
  void validateBatteries ()
  {
    int minBatteries = 0;
    for (int i = 0; i < shifts.size(); i++) {
      double n1 = shifts.get(i).getWeekdayTrucks();
      int d1 = shifts.get(i).getDuration();
      double n2 = shifts.get((i + 1) % shifts.size()).getWeekdayTrucks();
      int d2 = shifts.get((i + 1) % shifts.size()).getDuration();
      double neededBatteries =
          (n1 * d1 + n2 * d2) * truckKW / getBatteryCapacity();
      minBatteries =
          (int)Math.max(minBatteries, Math.ceil(n1 + n2));
      minBatteries =
          (int)Math.max(minBatteries, Math.ceil(neededBatteries));
      if ((n1 + n2) > getBatteryState().length) {
        log.error("Not enough batteries for " + getName() +
                  " (" + getBatteryState().length +
                  ") for shift combination [" + n1 + ", " + n2 + "]");
      }
    }
    // Add discharged batteries to fill out battery complement
    if (minBatteries > getBatteryState().length) {
      log.warn("Adding " + (minBatteries - getBatteryState().length) +
               " batteries for " + getName());
      List<Double> soc = getDoubleStateOfCharge();
      List<String> newSoc = new ArrayList<String>(minBatteries);
      for (Double b: soc) {
        newSoc.add(b.toString());
      }
      for (int i = 0; i < (minBatteries - getBatteryState().length); i++) {
        newSoc.add("0.0");
      }
      setStateOfCharge(newSoc);
    }
  }

  // make sure we have enough charging capacity to support
  // the shift schedule
  void validateChargers ()
  {
    // A single charger should be able to charge a truck worth of batteries
    // in a single shift
    

    // The total output of the chargers should be at least enough
    // to power the trucks over a 24-hour period
    double totalEnergy = 0;
    for (Shift shift: shifts) {
      totalEnergy += shift.getWeekdayTrucks() * shift.getDuration() * truckKW;
    }
    double chargeEnergy = nChargers * maxChargeKW * HOURS_DAY;
    if (totalEnergy > chargeEnergy) {
      double need = (totalEnergy - chargeEnergy) / (maxChargeKW * HOURS_DAY);
      int add = (int)Math.ceil(need);
      log.error("Insufficient charger capacity for " + getName() + ": have " +
                chargeEnergy + ", need " + totalEnergy +
                ". Adding " + add + " chargers.");
      nChargers += add;
    }
  }

  // ======== per-timeslot activities ========
  public void step (Timeslot timeslot)
  {
    // check for end-of-shift
    
    // discharge batteries on active trucks
    
    // deal with curtailment
    
    // use energy on chargers
    
    // switch out charged batteries
    
    // switch batteries on run-down trucks
  }

  

  // ================ getters and setters =====================
  // Note that list values must arrive and depart as List<String>,
  // while internally many of them are lists or arrays of numeric values.
  // Therefore we provide @ConfigurableValue setters that do the translation.
  String getName ()
  {
    return name;
  }

  void setName (String name)
  {
    this.name = name;
  }

  double getTruckKW ()
  {
    return truckKW;
  }

//  int getnTrucks ()
//  {
//    return nTrucks;
//  }

  /**
   * Converts a list of Strings to a sorted list of Shifts. Entries in the
   * list represent pairs of (start, duration) values. 
   */
  @ConfigurableValue(valueType = "List",
      name = "shifts",
      description = "shifts - [start, duration, start, duration, ...]")
  public List<Shift> setShiftData (List<String> data)
  {
    List<Integer> shiftData = new ArrayList<Integer>();
    for (String item: data) {
      shiftData.add(Integer.parseInt(item));
    }
    // make sure we have an even number of entries
    if (shiftData.size() % 2 != 0) {
      log.error("Odd number of entries in shift data - removing last entry");
      shiftData.remove(shiftData.size() - 1);
    }
    // create and sort the shifts
    Shift[] shiftArray = new Shift[shiftData.size() / 2];
    for (int i = 0; i < shiftArray.length; i++) {
      shiftArray[i] = new Shift(shiftData.remove(0), shiftData.remove(0));
    }
    Arrays.sort(shiftArray);
    List<Shift> result = Arrays.asList(shiftArray);
    shifts = result;
    return result;
  }

  List<Shift> getShifts ()
  {
    return shifts;
  }

  @ConfigurableValue(valueType = "List",
      name = "trucksInUseWeekday",
      description = "Number of trucks in use, by shift")
  public void setWeekdayUsageData (List<String> data)
  {
    trucksInUseWeekday = new ArrayList<Double>();
    for (String usage: data) {
      trucksInUseWeekday.add(Double.parseDouble(usage));
    }
  }

  List<Double> getTrucksInUseWeekday ()
  {
    return trucksInUseWeekday;
  }

  @ConfigurableValue(valueType = "List",
      name = "trucksInUseWeekend",
      description = "Number of trucks in use, by shift")
  public void setWeekendUsageData (List<String> data)
  {
    trucksInUseWeekend = new ArrayList<Double>();
    for (String usage: data) {
      trucksInUseWeekend.add(Double.parseDouble(usage));
    }
  }

  List<Double> getTrucksInUseWeekend ()
  {
    return trucksInUseWeekend;
  }

  double getBatteryCapacity ()
  {
    return batteryCapacity;
  }

  int getNChargers ()
  {
    return nChargers;
  }

  double getMaxChargeKW ()
  {
    return maxChargeKW;
  }

  double getChargeEfficiency ()
  {
    return chargeEfficiency;
  }

  @ConfigurableValue(valueType = "List",
      name = "stateOfCharge",
      bootstrapState = true,
      description = "current state-of-charge per battery")
  public void setStateOfCharge (List<String> data)
  {
    batteryState = new BatteryState[data.size()];
    int index = 0;
    for (String value: data) {
      batteryState[index] =
          new BatteryState(index, Double.parseDouble(value));
      index += 1;
    }
  }

  // intended to be used only if soc not configured
  void setDefaultStateOfCharge ()
  {
    batteryState = new BatteryState[defaultStateOfCharge.length];
    int index = 0;
    for (double value: defaultStateOfCharge) {
      batteryState[index] =
          new BatteryState(index, value);
      index += 1;
    }
  }

  public List<String> getStateOfCharge ()
  {
    if (null == batteryState)
      return null;
    ArrayList<String> soc = new ArrayList<String>(batteryState.length);
    for (Double state: getDoubleStateOfCharge()) {
      soc.add(state.toString());
    }
    return soc;
  }

  public List<Double> getDoubleStateOfCharge ()
  {
    if (null == batteryState)
      return null;
    ArrayList<Double> soc = new ArrayList<Double>(batteryState.length);
    for (BatteryState state: batteryState) {
      soc.add(state.getStateOfCharge());
    }
    return soc;
  }

  BatteryState[] getBatteryState ()
  {
    return batteryState;
  }

  // returns battery state sorted by inUse, then by soc
  BatteryState[] getSortedBatteryState ()
  {
    return getSortedBatteryState(batteryState);
  }

  BatteryState[] getSortedBatteryState (BatteryState[] state)
  {
    BatteryState[] result = state.clone();
    Arrays.sort(result);
    return result;
  }

  double getPlanningHorizon ()
  {
    return planningHorizon;
  }

  // ======== start, duration of a shift ========
  class Shift implements Comparable<Shift>
  {
    private int start;
    private int duration;
    private double weekdayTrucks = 0.0;
    private double weekendTrucks = 0.0;

    Shift (int start, int duration)
    {
      super();
      this.start = start;
      this.duration = duration;
    }

    int getStart ()
    {
      return start;
    }

    int getDuration ()
    {
      return duration;
    }

    double getWeekdayTrucks ()
    {
      return weekdayTrucks;
    }

    void setWeekdayTrucks (double weekdayTrucks)
    {
      this.weekdayTrucks = weekdayTrucks;
    }

    double getWeekendTrucks ()
    {
      return weekendTrucks;
    }

    void setWeekendTrucks (double weekendTrucks)
    {
      this.weekendTrucks = weekendTrucks;
    }

    double getTrucksFor (Instant date)
    {
      int day = date.get(DateTimeFieldType.dayOfWeek());
      if (day > 0 && day <= 5) // Sunday is 0
        return weekdayTrucks;
      else
        return weekendTrucks;
    }

    @Override
    public int compareTo (Shift s)
    {
      return start - s.start;
    }
  }

  // ======== State of an individual battery ========
  class BatteryState implements Comparable<BatteryState>
  {
    private Integer inUse = null;
    private Integer charger = null;
    private double soc = 0.0; // in kWh
    private int index;

    // normal constructor
    BatteryState (int index, double stateOfCharge)
    {
      super();
      this.index = index;
      this.soc = stateOfCharge;
    }

    // clone constructor
    BatteryState (BatteryState original)
    {
      super();
      this.index = original.index;
      this.soc = original.soc;
      this.inUse = original.inUse;
      this.charger = original.charger;
    }

    int getIndex()
    {
      return index;
    }

    boolean isInUse ()
    {
      return (null != inUse);
    }

    Integer getInUse ()
    {
      return inUse;
    }

    void setInUse (Integer index)
    {
      inUse = index;
    }

    boolean isCharging ()
    {
      return (null != charger);
    }

    Integer getCharger ()
    {
      return charger;
    }

    void setCharger (Integer index)
    {
      charger = index;
    }

    double getStateOfCharge ()
    {
      return soc;
    }

    // adds energy in kWh, returns actual energy used
    double charge (double kWh)
    {
      double actualCharge = kWh * chargeEfficiency;
      if (soc + actualCharge > batteryCapacity) {
        actualCharge = batteryCapacity - soc;
      }
      soc = soc + actualCharge;
      //stateOfCharge[index] = soc; // maintain bootstrap/planning data
      return actualCharge / chargeEfficiency;
    }

    void discharge (double kWh)
    {
      soc = Math.max(0.0, soc - kWh);
      //stateOfCharge[index] = soc; // maintain bootstrap/planning data
    }

    // sorting should start with the lowest batteries that are not in use
    @Override
    public int compareTo (BatteryState bs)
    {
      if (!isInUse() && bs.isInUse())
        return -1;
      if (isInUse() && !bs.isInUse())
        return 1;
      return ((Double)soc).compareTo((Double)bs.soc);
    }
  }

  // ======== Consumption plan ========
  class CapacityPlan
  {
    private Instant start;
    private double[] usage;
    private BatteryState[] localBatteryState;
    private BatteryState[] localChargers;

    CapacityPlan (Instant start, int size)
    {
      this.start = start;
      this.usage = new double[size];
      // copy in the battery state
      localBatteryState = new BatteryState[batteryState.length];
      for (int i = 0; i < batteryState.length; i++) {
        localBatteryState[i] = new BatteryState(batteryState[i]);
      }
      // copy in the charger state, using the local batteries
      localChargers = new BatteryState[nChargers];
      for (int i = 0; i < chargers.length; i++) {
        localChargers[i] = localBatteryState[chargers[i].getIndex()];
      }
    }

    double[] getUsage ()
    {
      return usage;
    }

    // creates a plan, with specified start time and state-of-charge
    void createPlan (Tariff tariff)
    {
      // behavior depends on tariff rate structure
      if (!tariff.isTimeOfUse()) {
        // flat rates, just charge ASAP
        createFlatRatePlan();
      }
    }

    void createFlatRatePlan ()
    {
      // Run localChargers as soon as there are batteries that need them
      int hour = start.get(DateTimeFieldType.hourOfDay());
      int shiftIndex = 0;
      int offset = 0;
      while (shifts.get(shiftIndex).getStart() + offset < hour) {
        shiftIndex += 1;
        if (shiftIndex >= shifts.size()) {
          // ran off the end
          shiftIndex = 0;
          offset += HOURS_DAY;
        }
      }
      // Now run forward to the end
      for (int ts = 0; ts < usage.length; ts++) {
        int hr = ts % HOURS_DAY;
        // if it's shift-change, then swap batteries
        Shift shift = shifts.get(shiftIndex);
        if (hr == shifts.get(shiftIndex).getStart()) {
          shiftIndex = (shiftIndex + 1) % shifts.size();
          // all the in-use batteries are no longer in-use
          for (BatteryState battery : localBatteryState) {
            if (battery.isInUse())
              battery.setInUse(null);
          }
          // sort the batteries and put the strongest in use for the next shift
          BatteryState[] sorted = getSortedBatteryState(localBatteryState);
          for (int off = 1; off <= getTrucksRounded(shift, hr); off--) {
            sorted[sorted.length - off].setInUse(off);
          }
          // put the weakest on the localChargers
          for (int i = 0; i < localChargers.length; i++) {
            if (!sorted[i].isInUse()) {
              localChargers[i] = sorted[i];
              sorted[i].setCharger(i);
            }
            else {
              localChargers[i] = null;
            }
            // TODO - what happens if one gets full?
          }
        }
        // all the in-use batteries run down some
        for (int i = 0; i < localBatteryState.length; i++) {
          BatteryState battery = localBatteryState[i];
          if (battery.isInUse())
            battery.discharge(truckKW);
          // TODO - what do we do if battery is completely discharged?
        }
        // all the charging batteries run up some
        for (int i = 0; i < localChargers.length; i++) {
          BatteryState battery = localChargers[i];
          double kWh = Math.min(maxChargeKW, batteryCapacity -
                                battery.getStateOfCharge());
          usage[ts] += kWh / chargeEfficiency;
        }
        // TODO - work out regulation capacity
      }
    }

    // Returns the number of trucks likely to be on this shift.
    // Used in the context of generating a tariff eval profile. 
    int getTrucksRounded (Shift shift, int hour)
    {
      Instant when = start.plus(hour * HOUR);
      int dow = when.get(DateTimeFieldType.dayOfWeek());
      double trucks= 0.0;
      if (dow <= 4)
        trucks = shift.getWeekdayTrucks();
      else
        trucks = shift.getWeekendTrucks();
      return (int)Math.round(trucks);
    }
  }
}
