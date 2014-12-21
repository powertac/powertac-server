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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.customer.AbstractCustomer;

/**
 * Models the complement of lift trucks in a warehouse. There may be
 * multiple trucks, a number of battery packs, and a daily/weekly work
 * schedule.
 * 
 * @author John Collins
 */
public class LiftTruck
{
  static private Logger log =
      Logger.getLogger(LiftTruck.class.getName());

  // need a name so we can configure it
  private String name;

  @ConfigurableValue(valueType = "Double",
      description = "power usage when truck is in use")
  private double truckKW = 4.0;

  @ConfigurableValue(valueType = "Integer",
      description = "number of trucks in facility")
  private int nTrucks = 10;

  @ConfigurableValue(valueType = "Double",
      description = "battery capacity per truck")
  private double batteryCapacity = 50.0;

  @ConfigurableValue(valueType = "Double",
      description = "maximum charging rate of one truck's battery pack")
  private double maxChargeKw = 6.0;

  @ConfigurableValue(valueType = "Double",
      description = "ratio of charge energy to battery energy")
  private double chargeEfficiency = 0.9;

  @ConfigurableValue(valueType = "Integer",
      description = "planning horizon in timeslots - should be at least 48")
  private double planningHorizon = 48;

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

//  private double[] stateOfCharge = {50.0,48.0,25.0,22.0,20.0,18.0,16.0,
//                                    14.0,12.0,10.0,8.0,6.0,4.0,2.0};

  private BatteryState[] batteryState;

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
    if (null == shifts)
      setShiftData(defaultShiftData);
  }

  /**
   * Initialization must provide accessor to Customer instance and time.
   * We assume configuration has already happened.
   */
  public void initialize (AbstractCustomer customer,
                          TimeslotRepo timeslotRepo)
  {
    this.customer = customer;
    this.timeslotRepo = timeslotRepo;

    // validate config data
    validateShifts();

    //TariffSubscription sub = customer.getCurrentSubscriptions().get(0);
  }

  // ensure correspondence between shifts and trucksInuse
  void validateShifts ()
  {
    int weekdayError = shifts.size() - trucksInUseWeekday.size();
    if (weekdayError > 0) {
      // not enough usage records
      log.error("not enough weekday records, adding "
               + weekdayError + " zeros");
      while (weekdayError > 0) {
        trucksInUseWeekday.add(0.0);
        weekdayError -= 1;
      }
    }
    else if (weekdayError < 0) {
      log.error("Too many weekday records, removing "
                + weekdayError + " records");
      while (weekdayError < 0) {
        trucksInUseWeekday.remove(trucksInUseWeekday.size() - 1);
        weekdayError += 1;
      }
    }
    int weekendError = shifts.size() - trucksInUseWeekend.size();
    if (weekendError > 0) {
      // not enough usage records
      log.warn("not enough weekend records, adding "
               + weekendError + " zeros");
      while (weekendError > 0) {
        trucksInUseWeekend.add(0.0);
        weekendError -= 1;
      }
    }
    else if (weekendError < 0) {
      log.error("Too many weekend records, removing "
                + weekendError + " records");
      while (weekendError < 0) {
        trucksInUseWeekend.remove(trucksInUseWeekend.size() - 1);
        weekendError += 1;
      }
    }
  }

  public void step (Timeslot timeslot)
  {
    
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

  int getnTrucks ()
  {
    return nTrucks;
  }

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

  double getMaxChargeKw ()
  {
    return maxChargeKw;
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

  public List<Double> getStateOfCharge ()
  {
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

  double getPlanningHorizon ()
  {
    return planningHorizon;
  }

  class Shift implements Comparable
  {
    private int start;
    private int duration;

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

    @Override
    public int compareTo (Object o)
    {
      return start - ((Shift)o).start;
    }
  }

  class BatteryState
  {
    private boolean inUse = false;
    private double soc = 0.0; // in kWh
    private int index;

    BatteryState (int index, double stateOfCharge)
    {
      super();
      this.index = index;
      this.soc = stateOfCharge;
    }

    boolean isInUse ()
    {
      return inUse;
    }

    void setInUse (boolean flag)
    {
      inUse = flag;
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
  }

  class CapacityPlan
  {
    private Instant start;
    private double[] usage;

    CapacityPlan (Instant start, int size)
    {
      this.start = start;
      this.usage = new double[size];
    }

    double[] getUsage ()
    {
      return usage;
    }

    // creates a plan, with specified start time and state-of-charge
    void createPlan (Tariff tariff, Instant start)
    {
      // behavior depends on tariff rate structure
      if (!tariff.isTimeOfUse()) {
        // flat rates, just charge ASAP
        createFlatRatePlan(start);
      }
    }

    void createFlatRatePlan (Instant start)
    {
      // all we need is the average consumption over all timeslots
      double dailyUsage = 0.0;
      Iterator trucks = trucksInUseWeekday.iterator();
      Iterator theShifts = shifts.iterator();
      while (trucks.hasNext()) {
        double truckCount = (Double)trucks.next();
        
      }
    }
  }
}
