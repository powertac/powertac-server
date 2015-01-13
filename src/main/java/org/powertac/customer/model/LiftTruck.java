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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.Timeslot;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.customer.AbstractCustomer;
import org.powertac.customer.StepInfo;

/**
 * Models the complement of lift trucks in a warehouse. There may be
 * multiple trucks, a number of battery packs, and a daily/weekly work
 * schedule. We assume that each truck uses one battery pack while it
 * is running. Since the lead-acid batteries in most lift trucks have
 * a limited number of charge/discharge cycles, we do not assume the
 * ability to discharge batteries into the grid to provide balancing
 * capacity. However, the charging rate is variable, and balancing capacity
 * can be provided by adjusting the rate.
 * 
 * Instances are created using the configureInstances() method. In addition
 * to simple parameters, configuration can specify a shift schedule and
 * the number and initial state-of-charge of battery packs.
 * 
 * The work schedule is specified with a list of strings called
 * weeklySchedule that lays out
 * blocks of shifts. Each block is of the form<br>
 * "block", d1, d2, ..., "shift" start, duration, ntrucks, "shift", ...<br>
 * where d1, d2, etc. are integers giving the days of the week covered by
 * the block, with Sunday=0; start is an integer hour, duration is an
 * integer number of hours, and ntrucks is the number of trucks that will
 * be active during the shift.
 * 
 * @author John Collins
 */
public class LiftTruck
{
  static private Logger log =
      Logger.getLogger(LiftTruck.class.getName());

  // ==== static constants ====
  static final int HOURS_DAY = 24;
  static final int DAYS_WEEK = 7;
  static final long HOUR = 3600*1000;

  // need a name so we can configure it
  private String name;

  @ConfigurableValue(valueType = "Double",
      description = "mean power usage when truck is in use")
  private double truckKW = 4.0;

  @ConfigurableValue(valueType = "Double",
      description = "Std dev of truck power usage")
  private double truckStd = 0.8;

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
      description = "Minimum charge level to remove battery from charger")
  private double minChargedCapacity = 45.0;

  @ConfigurableValue(valueType = "Double",
      description = "maximum charge rate of one truck's battery pack")
  private double maxChargeKW = 6.0;

  @ConfigurableValue(valueType = "Double",
      description = "ratio of charge energy to battery energy")
  private double chargeEfficiency = 0.9;

  @ConfigurableValue(valueType = "Integer",
      description = "planning horizon in timeslots - should be at least 48")
  private int planningHorizon = 60;

  // ==== Shift data ====
  // These List values are configured through their setter methods.
  // The default value is set in the constructor to serialize the
  // construction-configuration process
  private List<String> shiftData;
  private List<String> defaultShiftData =
      Arrays.asList("block", "1", "2", "3", "4", "5",
                    "shift", "8", "8", "8",
                    "shift", "16", "8", "6",
                    "shift", "0", "8", "3");
  private Shift[] shiftSchedule = new Shift[DAYS_WEEK * HOURS_DAY];
  private Shift currentShift = null;

  private double[] defaultStateOfCharge =
    {50.0,48.0,25.0,22.0,20.0,18.0,16.0,
     14.0,12.0,10.0,8.0,6.0,4.0,2.0,1.5,1.0}; // one entry per battery

  // ==== Current state ====
  private BatteryState[] batteryState;
  private int availableChargers;
  private double currentChargeRate = 1.0;
  private TariffSubscription currentSubscription;
  // constraint on current charging energy usage
  private ShiftEnergy[] futureEnergyNeeds = null;
  //private CapacityPlan plan;
  //private RandomSeed rs;
  private NormalDistribution normal;

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
   * active trucks, and the weakest batteries on availableChargers. Trucks will not
   * be active (in bootstrap mode) until the first shift change.
   */
  public void initialize (AbstractCustomer customer,
                          TimeslotRepo timeslotRepo,
                          RandomSeed seed)
  {
    this.customer = customer;
    this.timeslotRepo = timeslotRepo;
    //this.rs = seed;
    normal = new NormalDistribution();
    normal.reseedRandomGenerator(seed.nextLong());

    // use default values when not configured
    ensureShifts();
    ensureBatteryState();

    // make sure we have enough batteries and availableChargers
    validateBatteries();
    validateChargers();

    // put the weakest batteries on availableChargers
    availableChargers = nChargers;
    BatteryState[] sortedBatteries = getSortedBatteryState();
    for (int i = 0; i < nChargers; i++) {
      sortedBatteries[i].setCharging(true);
      availableChargers -= 1;
    }
    //TariffSubscription sub = customer.getCurrentSubscriptions().get(0);
  }

  // use default data if unconfigured
  void ensureShifts ()
  {
    for (Shift s: shiftSchedule) {
      if (!(null == s))
        return; // there's at least one non-empty hour with data
    }
    // we get here only if the schedule is empty
    setShiftData(defaultShiftData);
  }

  // use default data if unconfigured
  void ensureBatteryState ()
  {
    if (null == batteryState) {
      setDefaultStateOfCharge();
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
    Shift s1 = null;
    Shift s2 = null;
    for (int i = 0; i < shiftSchedule.length; i++) {
      Shift s = shiftSchedule[i];
      if (null == s) {
        s1 = s2;
      }
      else if (s2 != s) {
        s1 = s2;
        s2 = s;
        if (null != s1) {
          int n1 = s1.getTrucks();
          int d1 = s1.getDuration();
          int n2 = s2.getTrucks();
          int d2 = s2.getDuration();
          double neededBatteries =
              (n1 * d1 + n2 * d2) * truckKW / getBatteryCapacity();
          minBatteries =
              (int)Math.max(minBatteries, (n1 + n2));
          minBatteries =
              (int)Math.max(minBatteries, Math.ceil(neededBatteries));
        }
      }
    }
    if (minBatteries > getBatteryState().length) {
      log.error("Not enough batteries (" + getBatteryState().length +
                ") for " + getName());
      // Add discharged batteries to fill out battery complement
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
    // ToDo -- A single charging should be able to charge a truck worth
    // of batteries in a single shift

    // The total output of the availableChargers should be at least enough
    // to power the trucks over a 24-hour period
    double maxNeeded = 0.0;
    for (int i = 0; i < (shiftSchedule.length - HOURS_DAY); i++) {
      double totalEnergy = 0.0;
      Shift currentShift = shiftSchedule[i];
      if (null != currentShift) {
        totalEnergy +=
            currentShift.getTrucks() * currentShift.getDuration() * truckKW;
      }
      for (int j = i + 1; j < (i + HOURS_DAY); j++) {
        Shift newShift = shiftSchedule[j];
        if (null != newShift && currentShift != newShift) {
          totalEnergy +=
              newShift.getTrucks() * newShift.getDuration() * truckKW;
          currentShift = newShift;
        }
      }
      maxNeeded = Math.max(maxNeeded, totalEnergy);
    }

    double chargeEnergy = nChargers * maxChargeKW * HOURS_DAY;
    if (maxNeeded > chargeEnergy) {
      double need = (maxNeeded - chargeEnergy) / (maxChargeKW * HOURS_DAY);
      int add = (int)Math.ceil(need);
      log.error("Insufficient charging capacity for " + getName() + ": have " +
                chargeEnergy + ", need " + maxNeeded +
                ". Adding " + add + " availableChargers.");
      nChargers += add;
    }
  }

  // ======== per-timeslot activities ========
  // deal with curtailment/regulation from previous timeslot.
  // must be called in each timeslot before step().
  public void regulate (double kWh)
  {
    
  }

  public void step (StepInfo info)
  {
    // check for end-of-shift
    Shift newShift =
        shiftSchedule[indexOfShift(info.getTimeslot().getStartInstant())];
    if (newShift != currentShift) {
      // start of shift
      // Take all batteries out of service
      for (BatteryState state: batteryState) {
        state.clearInUse();
      }
      // Put the strongest batteries in trucks for the next shift
      if (null != newShift) {
        BatteryState[] bts = getSortedBatteryState();
        for (int i = 0; i < newShift.getTrucks(); i++) {
          bts[i].setInUse(true);
        }
      }
    }

    // discharge batteries on active trucks
    BatteryState[] batteries = getSortedBatteryState();
    for (int i = batteries.length - 1; batteries[i].isInUse(); i--) {
      BatteryState bat = batteries[i];
      double usage = Math.max(0.0, normal.sample() * truckStd + truckKW);
      bat.discharge(usage);
      // switch batteries on run-down trucks
      if (bat.getStateOfCharge() < truckKW) {
        // switch this one out
        bat.clearInUse();
        // find strongest replacement
        useStrongestAvailableBattery();
      }
    }

    // switch out charged batteries
    batteries = getSortedBatteryState(); // sort again
    for (int i = 0; i < batteries.length; i++) {
      if (batteries[i].getStateOfCharge() < batteryCapacity) {
        break;
      }
      if (batteries[i].isCharging()) {
        stopCharging(batteries[i]);
        chargeWeakestAvailableBattery();
      }
    }

    // use energy on active chargers, swap out batteries that are fully charged
    double energyUsed = useEnergy(info);
    info.addkWh(energyUsed);

    // compute regulation capacity
  }

  double useEnergy (StepInfo info)
  {
    Tariff tariff = info.getSubscription().getTariff();
    if (tariff.isTimeOfUse()) {
      return useEnergyTOU(info);
    }
    else {
      return useEnergyFlat(info);
    }
  }

  // use energy in a flat-rate tariff
  double useEnergyFlat (StepInfo info)
  {
    if (info.getSubscription().getTariff().hasRegulationRate()) {
      return useEnergyStorage(info);
    }
    else {
      return (useEnergyFirst(info));
    }
  }

  // uses energy as soon as possible
  double useEnergyFirst (StepInfo info)
  {
    BatteryState[] batteries = batteryState;
    double energyUsed = 0.0;
    for (int i = 0; i < batteries.length; i++) {
      if (batteries[i].isCharging()) {
        energyUsed += batteries[i].charge(maxChargeKW * currentChargeRate);
        if (batteries[i].getStateOfCharge() >= batteryCapacity) {
          stopCharging(batteries[i]);
          chargeWeakestAvailableBattery();
        }
      }
    }
    return energyUsed;
  }

  // uses energy to maximize storage capacity for regulation
  double useEnergyStorage (StepInfo info)
  {
    ShiftEnergy[] constraints = ensureFutureEnergyNeeds(info);
    
    return 0.0;
  }

  // use energy in a time-of-use tariff
  double useEnergyTOU (StepInfo info)
  {
    ShiftEnergy[] constraints = ensureFutureEnergyNeeds(info);
    BatteryState[] batteries = batteryState;
    double energyUsed = 0.0;
    for (int i = 0; i < batteries.length; i++) {
      if (batteries[i].isCharging()) {
        energyUsed += batteries[i].charge(maxChargeKW * currentChargeRate);
        if (batteries[i].getStateOfCharge() >= batteryCapacity) {
          stopCharging(batteries[i]);
          chargeWeakestAvailableBattery();
        }
      }
    }
    return energyUsed;
  }

  // returns the battery with highest SOC that is not in use, and either
  // not charging or already sufficiently charged. If we choose one that's
  // charging, then we have to put another one on the charging we vacated.
  void useStrongestAvailableBattery ()
  {
    BatteryState[] state = getSortedBatteryState();
    for (BatteryState bat: state) {
      if (bat.isCharging() && bat.getStateOfCharge() > minChargedCapacity) {
        stopCharging(bat);
        bat.setInUse(true);
        chargeWeakestAvailableBattery();
        return;
      }
      bat.setInUse(true);
      return;
    }
  }

  // ======== battery mgmt ========
  // puts the weakest available battery on a charger
  void chargeWeakestAvailableBattery ()
  {
    if (getAvailableChargers() <= 0) {
      // no available availableChargers
      return;
    }
    BatteryState[] state = getSortedBatteryState();
    for (int i = state.length - 1; i >= 0; i--) {
      if (!(state[i].isInUse() || state[i].isCharging())
          && state[i].getStateOfCharge() < minChargedCapacity) {
        startCharging(state[i]);
        return;
      }
    }
  }

  // start charging a battery, updating its state and
  // the number of chargers in use
  private void startCharging (BatteryState bat)
  {
    bat.setCharging(true);
    availableChargers -= 1;
  }

  // stop charging a battery, updating its state and
  // the number of chargers in use
  private void stopCharging (BatteryState bat)
  {
    bat.setCharging(false);
    availableChargers += 1;
  }

  // Ensures that we know the minimum energy we need for the future
  ShiftEnergy[] ensureFutureEnergyNeeds (StepInfo info)
  {
    // If the list exists and it's valid, just return the array
    if (null != futureEnergyNeeds) {
      if (futureEnergyNeeds[0].getNextShift() != currentShift) {
        // first element is still current
        futureEnergyNeeds[0].tick(); // one period gone
        return futureEnergyNeeds;
      }
    }
    // If we get here, the existing array is missing or no longer valid
    int index = indexOfShift(info.getTimeslot().getStartInstant());
    Shift currentShift = shiftSchedule[index]; // might be null
    int duration = 1;
    while (shiftSchedule[++index] == currentShift) {
      duration += 1;
    }
    Shift nextShift = shiftSchedule[index];
    // this gives us the info we need to start the sequence
    ArrayList<ShiftEnergy> data = new ArrayList<ShiftEnergy>();
    data.add(new ShiftEnergy(index, duration));
    duration = 0;
    for (int hour = 1; hour <= planningHorizon; hour++) {
      duration += 1;
      index = nextShiftIndex(index);
      // TODO - this does not handle the case of only one 24h shift
      if (nextShift != shiftSchedule[index]) {
        // start of next shift or idle period
        nextShift = shiftSchedule[index];
        data.add(new ShiftEnergy(index, duration));
        duration = 0;
      }
    }
    // now we convert to array, then walk backward and fill in energy needs
    ShiftEnergy[] result = data.toArray(new ShiftEnergy[data.size()]);
    futureEnergyNeeds = result;
    double shortage = 0.0;
    for (int i = result.length - 1; i >= 0; i--) {
      Shift end = shiftSchedule[result[i].endIndex];
      double needed = 0.0;
      if (null != end) {
        needed =
            (end.getTrucks() * end.getDuration() * getTruckKW());
      }
      // chargers is min of charger capacity and battery availability
      int chargers = getNChargers();
      int availableBatteries = batteryState.length;
      if (null != currentShift) {
        availableBatteries -= currentShift.getTrucks();
      }
      chargers = (int)Math.min(chargers, availableBatteries);
      double available =
          getMaxChargeKW() * result[i].getDuration() * chargers;
      shortage = Math.max(0.0, (available - needed - shortage));
      double surplus = Math.min(0.0, available - needed - shortage);
      result[i].setEnergyNeeded(needed);
      result[i].setMaxSurplus(surplus);
    }
    return result;
  }

  // Returns the index into the shift array corresponding to the given time.
  int indexOfShift (Instant time)
  {
    int hour = time.get(DateTimeFieldType.hourOfDay());
    int day = time.get(DateTimeFieldType.dayOfWeek());
    return hour + (day - 1) * HOURS_DAY;
  }

  // Returns the next index in the shift schedule
  int nextShiftIndex (int index)
  {
    return (index + 1) % shiftSchedule.length;
  }

  // Returns the previous index in the shift schedule
  int previousShiftIndex (int index)
  {
    return (index - 1) % shiftSchedule.length;
  }

  // If the given timeslot is the start of a new shift, then returns
  // the shift that starts at the given time, otherwise returns null.
//  Shift indexStartOfShift (Instant time)
//  {
//    Integer result = null;
//    int hour = time.get(DateTimeFieldType.hourOfDay());
//    for (int i = 0; i < shifts.size(); i++) {
//      Shift shift = shifts.get(i);
//      if (hour == shift.getStart()) {
//        return (new Integer(i));
//      }
//    }
//    return result;
//  }

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
      description = "shifts - [start, duration, start, duration, ...]")
  public void setShiftData (List<String> data)
  {
    int blk = 0;
    int shf = 1;
    int state = shf;

    LinkedList<String> tokens = new LinkedList<String>(data);
    ArrayList<Integer> blockData = new ArrayList<Integer>();
    ArrayList<Integer> shiftData = new ArrayList<Integer>();
    while (!(tokens.isEmpty())) {
      String token = tokens.remove();
      if (token.equals("block")) {
        // finish shift, switch to block
        if (!shiftData.isEmpty()) {
          finishShift(blockData, shiftData);
          shiftData.clear();
        }
        blockData.clear();
        state = blk;
      }
      else if (token.equals("shift")) {
        // finish block or previous shift, switch to shift
        if (!shiftData.isEmpty()) {
          finishShift(blockData, shiftData);
          shiftData.clear();
        }
        state = shf;
      }
      else { // collect numbers into correct list
        try {
          if (state == shf) 
            shiftData.add(Integer.parseInt(token));
          else if (state == blk)
            blockData.add(Integer.parseInt(token));
        }
        catch (NumberFormatException nfe) {
          log.error("Config error for " + getName() +
                    ": bad numeric token " + token);
        }
      }
    }
    // finish up last shift
    if (!shiftData.isEmpty()) {
      finishShift(blockData, shiftData);
    }
  }

  private void finishShift (ArrayList<Integer> blockData,
                            ArrayList<Integer> shiftData)
  {
    if (blockData.isEmpty()) {
      log.error("Config error for " + getName() +
                ": empty block for shift " + shiftData.toString());
    }
    else {
      addShift(shiftData, blockData);
    }
  }

  // Validates and creates a shift instance and populates the schedule
  // with references to the new shift
  void addShift (List<Integer> shiftData, List<Integer> blockData)
  {
    if (shiftData.size() < 3) {
      // nothing to do here
      log.error("Bad shift spec for " + getName() + 
                ": " + shiftData.toString());
      return;
    }
    if (!validBlock(blockData)) {
      log.error("Bad block data for " + getName() +
                ": " + blockData.toString());
      return;
    }
    int start = shiftData.get(0);
    if (start < 0 || start > 23) {
      log.error("Bad shift start time " + start + " for " + getName());
      return;
    }
    int duration = shiftData.get(1);
    if (duration < 1 || duration > 24) {
      log.error("Bad shift duration " + duration + " for " + getName());
      return;
    }
    int trucks = shiftData.get(2);
    if (trucks < 0) {
      log.error("Negative shift truck count " + trucks + " for " + getName());
      return;
    }
    Shift shift = new Shift(start, duration, trucks);

    // populate the schedule, ignoring overlaps. Later shifts may overlap
    // earlier ones. TODO; warn about overlaps
    for (int day: blockData) {
      for (int hour = shift.getStart();
          hour < shift.getStart() + shift.getDuration();
          hour++) {
        // Remember that Sunday is 1, not 0
        int index = (hour + (day - 1) * HOURS_DAY) % shiftSchedule.length;
        shiftSchedule[index] = shift;
      }
    }
  }

  // a valid block has integers in the range [1..7]
  boolean validBlock (List<Integer> data)
  {
    if (data.isEmpty()) {
      return false;
    }
    for (Integer datum: data) {
      if (datum < 1 || datum > 7) {
        return false;
      }
    }
    return true;
  }

  public List<String> getShiftData ()
  {
    return shiftData;
  }

  Shift[] getShiftSchedule()
  {
    return shiftSchedule;
  }

  double getBatteryCapacity ()
  {
    return batteryCapacity;
  }

  int getNChargers ()
  {
    return nChargers;
  }

  int getAvailableChargers ()
  {
    return availableChargers;
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
    private int trucks = 0;

    Shift (int start, int duration, int trucks)
    {
      super();
      this.start = start;
      this.duration = duration;
      this.trucks = trucks;
    }

    int getStart ()
    {
      return start;
    }

    int getDuration ()
    {
      return duration;
    }

    int getTrucks ()
    {
      return trucks;
    }

    void setTrucks (int count)
    {
      this.trucks = count;
    }

    @Override
    public int compareTo (Shift s)
    {
      return start - s.start;
    }

    @Override
    public String toString()
    {
      return ("Shift(" + start + "," + duration + "," + trucks + ")");
    }
  }

  // ======== Energy needed, available for a Shift ======
  class ShiftEnergy
  {

    //private int startIndex; // shift index of start
    private int endIndex; // shift index at start of next Shift or idle period
    private int duration; // in hours
    private double energyNeeded = 0.0; // in kWh at end of duration
    private double maxSurplus = 0.0; // possible kWh beyond needed

    ShiftEnergy (int end, int duration)
    {
      super();
      this.endIndex = end;
      Shift next = shiftSchedule[end];
      if (null != next) {
        energyNeeded = next.getTrucks() * next.getDuration() * getTruckKW();
      }
      this.duration = duration;
    }

//    int getStartIndex ()
//    {
//      return startIndex;
//    }
//
//    Shift getThisShift ()
//    {
//      if (startIndex >= shiftSchedule.length)
//        return null;
//      return shiftSchedule[startIndex];
//    }

    int getEndIndex ()
    {
      return endIndex;
    }

    Shift getNextShift ()
    {
      if (endIndex >= shiftSchedule.length)
        return null;
      return shiftSchedule[endIndex];
    }

    int getDuration ()
    {
      return duration;
    }

    // reduce duration by one on each tick of the clock 
    void tick ()
    {
      duration -= 1;
    }

    double getEnergyNeeded ()
    {
      return energyNeeded;
    }

    void setEnergyNeeded (double energyNeeded)
    {
      this.energyNeeded = energyNeeded;
    }

    double getMaxSurplus ()
    {
      return maxSurplus;
    }

    void setMaxSurplus (double maxSurplus)
    {
      this.maxSurplus = maxSurplus;
    }

    void addSurplus (double surplus)
    {
      maxSurplus += surplus;
    }
  }

  // ======== State of an individual battery ========
  class BatteryState implements Comparable<BatteryState>
  {
    private boolean inUse = false;
    private boolean charging = false;
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
      this.charging = original.charging;
    }

    int getIndex()
    {
      return index;
    }

    boolean isInUse ()
    {
      return inUse;
    }

    // clears the inUse property
    void clearInUse ()
    {
      inUse = false;
    }

    // sets the truck index for this battery
    // TODO - do we need this??
    void setInUse (boolean state)
    {
      inUse = state;
    }

    boolean isCharging ()
    {
      return charging;
    }

    void setCharging (boolean state)
    {
      charging = state;
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
      return actualCharge / chargeEfficiency;
    }

    void discharge (double kWh)
    {
      soc = Math.max(0.0, soc - kWh);
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
    private int localChargers;

    CapacityPlan (Instant start, int size)
    {
      this.start = start;
      this.usage = new double[size];
      // copy in the battery state
      localBatteryState = new BatteryState[batteryState.length];
      for (int i = 0; i < batteryState.length; i++) {
        localBatteryState[i] = new BatteryState(batteryState[i]);
      }
      // copy in the charging state, using the local batteries
      localChargers = availableChargers;
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
//      // Run localChargers as soon as there are batteries that need them
//      int hour = start.get(DateTimeFieldType.hourOfDay());
//      int shiftIndex = 0;
//      int offset = 0;
//      while (shifts.get(shiftIndex).getStart() + offset < hour) {
//        shiftIndex += 1;
//        if (shiftIndex >= shifts.size()) {
//          // ran off the end
//          shiftIndex = 0;
//          offset += HOURS_DAY;
//        }
//      }
//      // Now run forward to the end
//      for (int ts = 0; ts < usage.length; ts++) {
//        int hr = ts % HOURS_DAY;
//        // if it's shift-change, then swap batteries
//        Shift shift = shifts.get(shiftIndex);
//        if (hr == shifts.get(shiftIndex).getStart()) {
//          shiftIndex = (shiftIndex + 1) % shifts.size();
//          // all the in-use batteries are no longer in-use
//          for (BatteryState battery : localBatteryState) {
//            if (battery.isInUse())
//              battery.setInUse(null);
//          }
//          // sort the batteries and put the strongest in use for the next shift
//          BatteryState[] sorted = getSortedBatteryState(localBatteryState);
//          for (int off = 1; off <= getTrucksRounded(shift, hr); off--) {
//            sorted[sorted.length - off].setInUse(off);
//          }
//          // put the weakest on the localChargers
//          for (int i = 0; i < localChargers.length; i++) {
//            if (!sorted[i].isInUse()) {
//              localChargers[i] = sorted[i];
//              sorted[i].setCharger(i);
//            }
//            else {
//              localChargers[i] = null;
//            }
//            // TODO - what happens if one gets full?
//          }
//        }
//        // all the in-use batteries run down some
//        for (int i = 0; i < localBatteryState.length; i++) {
//          BatteryState battery = localBatteryState[i];
//          if (battery.isInUse())
//            battery.discharge(truckKW);
//          // TODO - what do we do if battery is completely discharged?
//        }
//        // all the charging batteries run up some
//        for (int i = 0; i < localChargers.length; i++) {
//          BatteryState battery = localChargers[i];
//          double kWh = Math.min(maxChargeKW, batteryCapacity -
//                                battery.getStateOfCharge());
//          usage[ts] += kWh / chargeEfficiency;
//        }
//        // TODO - work out regulation capacity
//      }
//    }
//
//    // Returns the number of trucks likely to be on this shift.
//    // Used in the context of generating a tariff eval profile. 
//    int getTrucksRounded (Shift shift, int hour)
//    {
//      Instant when = start.plus(hour * HOUR);
//      int dow = when.get(DateTimeFieldType.dayOfWeek());
//      double trucks= 0.0;
//      if (dow <= 4)
//        trucks = shift.getWeekdayTrucks();
//      else
//        trucks = shift.getWeekendTrucks();
//      return (int)Math.round(trucks);
    }
  }
}
