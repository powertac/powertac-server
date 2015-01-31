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

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.log4j.Logger;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.powertac.common.RandomSeed;
import org.powertac.common.Tariff;
import org.powertac.common.TariffSubscription;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.customer.AbstractCustomer;
import org.powertac.customer.StepInfo;

import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import com.joptimizer.optimizers.OptimizationResponse;

/**
 * Models the complement of lift trucks in a warehouse. There may be
 * multiple trucks, some number of battery packs, and a daily/weekly work
 * schedule. Since the lead-acid batteries in most lift trucks have
 * a limited number of charge/discharge cycles, we do not assume the
 * ability to discharge batteries into the grid to provide balancing
 * capacity. However, the charging rate is variable, and balancing capacity
 * can be provided by adjusting the rate. Batteries and battery chargers
 * are not modeled directly;
 * instead, we simply keep track of the overall battery capacity and how
 * it changes when shifts start and end, and when chargers run.
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

  @ConfigurableValue(valueType = "Double",
      description = "size of battery pack in kWh")
  private double batteryCapacity = 50.0;

  @ConfigurableValue(valueType = "Integer",
      description = "total number of battery packs")
  private int nBatteries = 15;

  @ConfigurableValue(valueType = "Integer",
      description = "number of battery chargers")
  private int nChargers = 8;

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

  // ==== Current state ====
//  private double currentChargeRate = 1.0;
//  private double stateOfCharge = 0.7;

  @ConfigurableValue(valueType = "Double",
      bootstrapState = true,
      description = "Battery capacity currently being used in trucks")
  private double capacityInUse = 0.0; // capacity in use during shift

  @ConfigurableValue(valueType = "Double",
      bootstrapState = true,
      description = "Offline battery energy, currently in the trucks")
  private double energyInUse = 0.0; // energy content of in-use batteries

  @ConfigurableValue(valueType = "Double",
      bootstrapState = true,
      description = "Online battery energy, currently being charged")
  private double energyCharging = 0.0; // energy content of charging batteries

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
    //ensureBatteryState();

    // make sure we have enough batteries and availableChargers
    validateBatteries();
    validateChargers();

    // all batteries are charging
    //energyCharging = getStateOfCharge() * getBatteryCapacity();
    capacityInUse = 0.0;
    energyInUse = 0.0;
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
    int neededBatteries = minBatteries - nBatteries;
    if (neededBatteries > 0) {
      log.error("Not enough batteries (" + nBatteries +
                ") for " + getName());
      // Add discharged batteries to fill out battery complement
      log.warn("Adding " + neededBatteries + " batteries for " + getName());
      //double totalCapacity = batteryCapacity * nBatteries;
      //double availableCapacity = energyInUse + energyCharging;
      nBatteries += neededBatteries;
      //setStateOfCharge(availableCapacity / (batteryCapacity * nBatteries));
    }
  }

  // make sure we have enough charging capacity to support
  // the shift schedule
  void validateChargers ()
  {
    // ToDo -- A single charging should be able to charge a truck worth
    // of batteries in a single shift

    // The total output of the availableChargers should be at least enough
    // to power the trucks over a 24-hour period. Note that the shift schedule
    // starts at midnight, which may not be the start of the current shift.
    double maxNeeded = 0.0;
    Shift currentShift = shiftSchedule[0];
    int hoursInShift = (HOURS_DAY - currentShift.getStart()) % HOURS_DAY;
    int remainingDuration = currentShift.getDuration() - hoursInShift;
    for (int i = 0; i < (shiftSchedule.length - HOURS_DAY); i++) {
      double totalEnergy = 0.0;
      Shift thisShift = shiftSchedule[i];
      if (thisShift != currentShift) {
        currentShift = thisShift;
        if (null != currentShift) {
          // first block of energy in 24h window starting at i
          remainingDuration = currentShift.getDuration();
        }
      }
      if (null != currentShift) {
        totalEnergy +=
            currentShift.getTrucks() * remainingDuration * truckKW;
      }
      // now run fwd 24h and add energy from future shifts
      Shift current = currentShift;
      int shiftStart = i;
      for (int j = i + 1; j < (i + HOURS_DAY); j++) {
        Shift newShift = shiftSchedule[j];
        if (null != newShift && current != newShift) {
          int durationInWindow =
              (int)Math.min((i + HOURS_DAY - j), newShift.getDuration());
          totalEnergy +=
              newShift.getTrucks() * durationInWindow * truckKW;
          current = newShift;
        }
      }
      maxNeeded = Math.max(maxNeeded, totalEnergy);
      remainingDuration -= 1;
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
      double totalEnergy = energyCharging + energyInUse;
      energyCharging += energyInUse;
      capacityInUse = 0.0;
      energyInUse = 0.0;
      
      // Put the strongest batteries in trucks for the next shift
      if (null != newShift) {
        capacityInUse = newShift.getTrucks() * batteryCapacity;
        energyInUse = Math.min(capacityInUse, totalEnergy);
        energyCharging = totalEnergy - energyInUse;
      }
    }

    // discharge batteries on active trucks
    double usage =
        Math.max(0.0,
                 normal.sample() * truckStd +
                 truckKW * currentShift.getTrucks());
    double deficit = usage - energyInUse;
    log.debug(getName() + ": trucks use " + usage + " kWh");
    if (deficit > 0.0) {
      log.warn(getName() + ": trucks use more energy than available by "
               + deficit + " kWh");
      energyInUse += deficit;
      energyCharging -= deficit;
    }
    energyInUse -= usage;

    // use energy on chargers
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
    else if (info.getSubscription().getTariff().hasRegulationRate()) {
      return useEnergyStorage(info);
    }
    else {
      return useEnergyEarly(info);
    }
  }

  double useEnergyEarly (StepInfo info)
  {
    double max = nChargers * maxChargeKW;
    double avail =
        nBatteries * batteryCapacity - capacityInUse - energyCharging;
    double energyUsed = Math.min(max, avail) / chargeEfficiency;
    log.debug(getName() + " useEarly " + energyUsed);
    return energyUsed;
  }

  // use energy in a time-of-use tariff
  double useEnergyTOU (StepInfo info)
  {
    double energyUsed = 0.0;
    ShiftEnergy[] constraints = ensureFutureEnergyNeeds(info);
    return energyUsed;
  }

  // uses energy to maximize storage capacity for regulation
  double useEnergyStorage (StepInfo info)
  {
    ShiftEnergy[] constraints = ensureFutureEnergyNeeds(info);
    ShiftEnergy constraint = constraints[0];
    double needed = constraint.getEnergyNeeded();
    double rate = needed / constraint.duration;
    // split the rate among available chargers?
    // or run as many as possible at full power?
    constraint.tick();
    return 0.0;
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
    else {
      // If we get here, the existing array is missing or no longer valid
      futureEnergyNeeds =
          getFutureEnergyNeeds(info.getTimeslot().getStartInstant(),
                               planningHorizon,
                               energyCharging);
    }
    return futureEnergyNeeds;
  }

  // Computes constraints on future energy needs
  // Amounts are energy needed to run the chargers. Energy input to trucks
  // will be smaller due to charge efficiency.
  ShiftEnergy[] getFutureEnergyNeeds (Instant start, int horizon,
                                      double initialCharging)
  {
    int index = indexOfShift(start);
    // current time is likely to be partway into first shift
    Shift currentShift = shiftSchedule[index]; // might be null
    int duration = 1;
    while (shiftSchedule[++index] == currentShift) {
      duration += 1;
    }
    Shift nextShift = shiftSchedule[index];
    // this gives us the info we need to start the sequence
    ArrayList<ShiftEnergy> data = new ArrayList<ShiftEnergy>();
    data.add(new ShiftEnergy(index, duration));
    int elapsed = duration;
    // add shifts until we run off the end of the horizon
    // keep in mind that a shift can be null
    while (elapsed < horizon) {
      duration = 0;
      while (nextShift == shiftSchedule[index]) {
        index = nextShiftIndex(index);
        duration += 1;
      }
      nextShift = shiftSchedule[index];
      data.add(new ShiftEnergy(index, duration));
      elapsed += duration;
    }
    // now we convert to array, then walk backward and fill in energy needs
    ShiftEnergy[] result = data.toArray(new ShiftEnergy[data.size()]);
    double shortage = 0.0;
    for (int i = result.length - 1; i >= 0; i--) {
      int endx = result[i].endIndex;
      int prev = previousShiftIndex(endx);
      currentShift = shiftSchedule[prev];
      Shift end = shiftSchedule[endx];
      double needed = 0.0;
      if (null != end) {
        // Assume we need, at the end of each shift, enough energy to
        // run the next shift
        needed =
            (end.getTrucks() * end.getDuration() * getTruckKW())
            / getChargeEfficiency();
      }
      // chargers is min of charger capacity and battery availability
      int chargers = getNChargers();
      int availableBatteries = nBatteries;
      if (null != currentShift) {
        availableBatteries -= currentShift.getTrucks();
      }
      chargers = (int)Math.min(chargers, availableBatteries);
      double available =
          getMaxChargeKW() * result[i].getDuration() * chargers
          / getChargeEfficiency();
      double surplus = available - needed - shortage;
      shortage = Math.max(0.0, -(available - needed - shortage));
      result[i].setEnergyNeeded(needed);
      result[i].setMaxSurplus(surplus);
    }
    // finally, we need to update the first element with
    // the current battery charge.
    double finalSurplus = result[0].getMaxSurplus();
    if (finalSurplus > 0.0) {
      result[0].setMaxSurplus(finalSurplus + initialCharging);
    }
    else if (shortage > 0.0) {
      result[0].setMaxSurplus(initialCharging - shortage);
    }
    return result;
  }

  CapacityPlan getCapacityPlan(Tariff tariff, Instant start, int size)
  {
    CapacityPlan result = new CapacityPlan(tariff, start, size);
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
    if (0 == index)
      return shiftSchedule.length - 1;
    return (index - 1) % shiftSchedule.length;
  }

  // Returns the next date/time when the given shift index will occur
  Instant indexToInstant (int index)
  {
    Instant now = timeslotRepo.currentTimeslot().getStartInstant();
    int probe = index;
    // get the probe within the shift schedule
    while (probe < 0) {
      probe += shiftSchedule.length;
    }
    while (probe > shiftSchedule.length) {
      probe -= shiftSchedule.length;
    }
    int nowIndex = indexOfShift(now);
    if (nowIndex <= index) {
      return (now.plus(HOUR * (index - nowIndex)));
    }
    return (now.plus(HOUR * (shiftSchedule.length + index - nowIndex)));
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
      description = "shift spec [block, shift, ..., block, shift, ...]")
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

  int getnBatteries ()
  {
    return nBatteries;
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
    private int endIndex; // shift index at start of next Shift or idle period
    private int duration; // in hours
    private double energyNeeded = 0.0; // in kWh at end of duration
    private double maxSurplus = 0.0; // possible kWh beyond needed - can be negative

    // Plan recommendations
    private double[] recommendedUsage;
    private double slack;
    private int usageIndex = 0;

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

    int getStartIndex ()
    {
      return previousShiftIndex(endIndex);
    }

    Shift getThisShift ()
    {
      return shiftSchedule[getStartIndex()];
    }

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
      usageIndex += 1;
    }

    double getEnergyNeeded ()
    {
      return energyNeeded;
    }

    void setEnergyNeeded (double energyNeeded)
    {
      this.energyNeeded = energyNeeded;
    }

    // Reduces energyNeeded by the given amount
    void addEnergy (double energy)
    {
      energyNeeded -= energy;
    }

    double getMaxSurplus ()
    {
      return maxSurplus;
    }

    void setMaxSurplus (double maxSurplus)
    {
      this.maxSurplus = maxSurplus;
    }

    // Increases the surplus by the given amount.
    void addSurplus (double surplus)
    {
      maxSurplus += surplus;
    }

    // ---- access to plan data ----
    // Returns the recommendedUsage array. This will be null unless this
    // instance has been decorated CapacityPlan.getNeeds().
    double[] getRecommendedUsage ()
    {
      return recommendedUsage;
    }

    // Returns the recommended usage for the current timeslot, assuming
    // tick() has been called at the conclusion of each timeslot.
    double getCurrentRecommendedUsage ()
    {
      return recommendedUsage[usageIndex];
    }

    // Returns amount by which usage can vary before plan is violated
    double getSlack ()
    {
      return slack;
    }

    // ---- decoration by plan ----
    void setRecommendedUsage (double[] usagePlan)
    {
      if (usagePlan.length != duration) {
        // These should be the same
        log.error("usagePlan length " + usagePlan.length
                  + " > duration " + duration);
      }
      this.recommendedUsage = usagePlan;
    }

    void setSlack (double slack)
    {
      this.slack = slack;
    }
  }

  // ======== Consumption plan ========
  // Used for tariff evaluation, and for guiding consumption.
  class CapacityPlan
  {
    private double[] usage;
    private double[] slack;
    private ShiftEnergy[] needs;
    private Instant start;
    private int size;
    private Tariff tariff;

    // Creates a plan for the standard planning horizon
    CapacityPlan (Tariff tariff, Instant start)
    {
      this(tariff, start, planningHorizon);
    }

    // Creates a plan for a custom size
    CapacityPlan (Tariff tariff, Instant start, int size)
    {
      super();
      this.tariff = tariff;
      this.start = start;
      this.size = size;
    }

    double[] getUsage ()
    {
      return usage;
    }

    // creates a plan using the default tariff and initial conditions
    void createPlan (double initialCharging)
    {
      createPlan(this.tariff, initialCharging);
    }

    // creates a plan, with specified start time and state-of-charge
    void createPlan (Tariff tariff,
                     double initialCharging)
    {
      needs = getFutureEnergyNeeds(start, size, initialCharging);
      // update size to use all of last ShiftEnergy instance
      int newSize = 0;
      for (ShiftEnergy need : needs)
        newSize += need.getDuration();
      size = newSize;
      LpPlan plan = new LpPlan(tariff, needs, size);
      usage = plan.getSolution();
      slack = plan.getSlack();
    }

    // returns the ShiftEnergy array used to create the plan,
    // decorated with the most recent solution
    ShiftEnergy[] getNeeds ()
    {
      if (null == needs)
        return null;
      int usageIndex = 0;
      for (int i = 0; i < needs.length; i++) {
        ShiftEnergy se = needs[i];
        se.setRecommendedUsage(Arrays.copyOfRange(usage, 
                                                  usageIndex, 
                                                  usageIndex + se.getDuration()));
        usageIndex += se.getDuration();
        se.setSlack(slack[i]);
      }
      return needs;
    }
  }

  // Creates a plan using the JOptimizer LP solver, gives access to
  // solution and slack values
  class LpPlan
  {
    double[] solution;
    double[] slack;
    boolean solved = false;
    Tariff tariff;
    ShiftEnergy[] needs;
    int size;

    LpPlan (Tariff tariff, ShiftEnergy[] needs, int size)
    {
      super();
      this.tariff = tariff;
      this.needs = needs;
      this.size = size;
    }

    // formulate and generate the solution, if necessary
    private void solve ()
    {
      if (solved)
        return;

      // min obj.x s.t. a.x=b, lb <= x <= ub
      // x is energy use per hour for size hours, slack var per shift
      Date start = new Date();
      int columns = size;
      int shifts = needs.length;
      Instant time =
          indexToInstant(needs[0].getStartIndex());
      double[] obj = new double[columns + shifts];
      double[][] a = new double[shifts][columns + shifts];
      double[] b = new double[shifts];
      double[] lb = new double[columns + shifts];
      double[] ub = new double[columns + shifts];
      int column = 0;
      double cumulativeMin = 0.0; // this is the primary constraint
      // construct the problem
      for (int i = 0; i < shifts; i++) {
        // one iteration per shift
        double kwh =
            needs[i].getEnergyNeeded() + needs[i].getMaxSurplus();
        for (int j = 0; j < needs[i].getDuration(); j++) {
          // one iteration per timeslot within a shift
          // fill in objective function
          // cost based on assumption that shift need is evenly distributed
          double cost =
              tariff.getUsageCharge(time, kwh / needs[i].getDuration(), kwh);
          obj[column] = cost / (kwh / needs[i].getDuration());
          // construct cumulative usage constraints
          //a[i][column] = -1.0;
          time = time.plus(HOUR);
          lb[column] = 0.0;
          ub[column] =
              (needs[i].getEnergyNeeded() + needs[i].getMaxSurplus())
              / needs[i].getDuration();
          column += 1;
        }
        // fill a row up to column
        for (int j = 0; j < column; j++) {
          a[i][j] = -1.0;
        }
        // b vector - one entry per constraint
        double need = needs[i].getEnergyNeeded();
        if (needs[i].getMaxSurplus() < 0.0)
          need += needs[i].getMaxSurplus();
        cumulativeMin += need;
        b[i] = -cumulativeMin;
        // fill in slack values, one per constraint
        obj[columns + i] = 0.0;
        a[i][columns + i] = 1.0;
        lb[columns + i] = 0.0;
        ub[columns + i] =
            (needs[i].getEnergyNeeded() + needs[i].getMaxSurplus())
            / needs[i].getDuration();
      }
      // run the optimization
      LPOptimizationRequest or = new LPOptimizationRequest();
      log.debug("Obj: " + Arrays.toString(obj));
      or.setC(obj);
      log.debug("a:");
      for (int i = 0; i < a.length; i++)
        log.debug(Arrays.toString(a[i]));
      or.setA(a);
      log.debug("b: " + Arrays.toString(b));
      or.setB(b);
      or.setLb(lb);
      log.debug("ub: " + Arrays.toString(ub));
      or.setUb(ub);
      or.setTolerance(1.0e-2);
      LPPrimalDualMethod opt = new LPPrimalDualMethod();
      opt.setLPOptimizationRequest(or);
      try {
        int returnCode = opt.optimize();
        assertEquals("success", OptimizationResponse.SUCCESS, returnCode);
        double[] sol = opt.getOptimizationResponse().getSolution();
        Date end = new Date();
        log.debug("Solution time: " + (end.getTime() - start.getTime()));
        log.debug("Solution = " + Arrays.toString(sol));
        recordSolution(sol);
      }
      catch (Exception e) {
        log.error(e.toString());
      }
      // we call it solved whether or not the solution was successful
      solved = true;
    }

    // pulls apart the usage and slack data
    void recordSolution(double[] lpResult)
    {
      solution = Arrays.copyOfRange(lpResult, 0, size);
      log.debug("Usage: " + Arrays.toString(solution));
      slack = Arrays.copyOfRange(lpResult, size, lpResult.length);
      log.debug("Slack: " + Arrays.toString(slack));
    }

    double[] getSolution ()
    {
      solve();
      return solution;
    }

    double[] getSlack ()
    {
      solve();
      return slack;
    }
  }
}
