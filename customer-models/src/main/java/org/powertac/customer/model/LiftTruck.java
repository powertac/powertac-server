/*
 * Copyright (c) 2015 by the original author
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Instant;
import org.powertac.common.CapacityProfile;
import org.powertac.common.CustomerInfo;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.RandomSeed;
import org.powertac.common.RegulationCapacity;
import org.powertac.common.Tariff;
import org.powertac.common.TariffEvaluator;
import org.powertac.common.TariffSubscription;
import org.powertac.common.TimeService;
import org.powertac.common.config.ConfigurableInstance;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;
import org.powertac.customer.AbstractCustomer;

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
@Domain
@ConfigurableInstance
public class LiftTruck
extends AbstractCustomer
implements CustomerModelAccessor
{
  static private Logger log = LogManager.getLogger(LiftTruck.class.getName());

  // ==== static constants ====
  static final int HOURS_DAY = 24;
  static final int DAYS_WEEK = 7;
  //static final long HOUR = 3600*1000;

  // need a name so we can configure it (in case it's not an AbstractCustomer)
  //private String name;

  private double truckKW = 4.0;
  private double truckStd = 0.8;
  private double batteryCapacity = 50.0;
  private int nBatteries = 15;
  private int nChargers = 8;
  private double maxChargeKW = 6.0;
  private double chargeEfficiency = 0.9;
  private int planningHorizon = 60;
  private int minPlanningHorizon = 24;

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

  @ConfigurableValue(valueType = "Double", dump = false,
      bootstrapState = true,
      description = "Battery capacity currently being used in trucks")
  private double capacityInUse = 0.0; // capacity in use during shift

  @ConfigurableValue(valueType = "Double", dump = false,
      bootstrapState = true,
      description = "Offline battery energy, currently in the trucks")
  private double energyInUse = 0.0; // energy content of in-use batteries

  @ConfigurableValue(valueType = "Double", dump = false,
      bootstrapState = true,
      description = "Online battery energy, currently being charged")
  private double energyCharging = 0.0; // energy content of charging batteries

  // constraint on current charging energy usage
  private PowerType powerType;
  //private ShiftEnergy[] futureEnergyNeeds = null;
  private CapacityPlan plan;

  // random seeds
  private RandomSeed opSeed = null;
  private RandomSeed evalSeed = null;
  private NormalDistribution normal;

  // context references
  private TariffEvaluator tariffEvaluator;

  /**
   * Default constructor, requires manual setting of name
   */
  public LiftTruck ()
  {
    super();
  }

  /**
   * Standard constructor for named configurable type
   */
  public LiftTruck (String name)
  {
    super(name);
  }

  /**
   * Initialization must provide accessor to Customer instance and time.
   * We assume configuration has already happened. We also start with no
   * active trucks, and the weakest batteries on availableChargers. Trucks will not
   * be active (in bootstrap mode) until the first shift change.
   */
  @Override
  public void initialize ()
  {
    super.initialize();
    log.info("Initialize " + name);
    // fill out CustomerInfo. We label this model as thermal storage
    // because we don't allow battery discharge
    powerType = PowerType.THERMAL_STORAGE_CONSUMPTION;
    CustomerInfo info = new CustomerInfo(name, 1);
    // conservative interruptible capacity
    double interruptible =
        Math.min(nChargers * maxChargeKW, nBatteries * maxChargeKW / 3.0);
    info.withPowerType(powerType)
        .withCustomerClass(CustomerClass.LARGE)
        .withControllableKW(-interruptible)
        .withStorageCapacity(nBatteries * maxChargeKW / 3.0)
        .withUpRegulationKW(-nChargers * maxChargeKW)
        .withDownRegulationKW(nChargers * maxChargeKW); // optimistic, perhaps
    addCustomerInfo(info);
    ensureSeeds();

    // use default values when not configured
    ensureShifts();

    // make sure we have enough batteries and availableChargers
    validateBatteries();
    validateChargers();

    // all batteries are charging
    //energyCharging = getStateOfCharge() * getBatteryCapacity();
    //capacityInUse = 0.0;
    //energyInUse = 0.0;

    // set up the tariff evaluator. We are wide-open to variable pricing.
    tariffEvaluator = createTariffEvaluator(this);
    tariffEvaluator.withInertia(0.7).withPreferredContractDuration(14);
    tariffEvaluator.initializeInconvenienceFactors(0.0, 0.01, 0.0, 0.0);
    tariffEvaluator.initializeRegulationFactors(-nChargers * maxChargeKW * 0.05,
                                                0.0,
                                                nChargers * maxChargeKW * 0.04);
  }

  // Gets a new random-number opSeed just in case we don't already have one.
  // Useful for mock-based testing.
  private void ensureSeeds ()
  {
    if (null == opSeed) {
      RandomSeedRepo repo = service.getRandomSeedRepo();
      opSeed = repo.getRandomSeed(
                         LiftTruck.class.getName() + "-" + name, 0, "model");
      evalSeed = repo.getRandomSeed(
                         LiftTruck.class.getName() + "-" + name, 0, "eval");
      normal = new NormalDistribution(0.0, 1.0);
      normal.reseedRandomGenerator(opSeed.nextLong());
    }
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
      setNBatteries(getNBatteries() + neededBatteries);
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
    int offset = 0;
    while (null == shiftSchedule[offset]) {
      offset += 1;
    }
    Shift currentShift = shiftSchedule[offset];
    int remainingDuration = 0;
    int hoursInShift = (HOURS_DAY - currentShift.getStart()) % HOURS_DAY;
    remainingDuration = currentShift.getDuration() - hoursInShift;

    for (int i = offset; i < (shiftSchedule.length - HOURS_DAY); i++) {
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
      //int shiftStart = i;
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
      setNChargers(getNChargers() + add);
    }
  }

  @Override
  public CustomerInfo getCustomerInfo ()
  {
    return getCustomerInfo(powerType);
  }

  // ======== per-timeslot activities ========
  @Override
  public void step ()
  {
    // check for end-of-shift
    Shift newShift =
        shiftSchedule[indexOfShift(getNowInstant())];
    if (newShift != currentShift) {
      log.info(getName() + " start of shift");
      // Take all batteries out of service
      double totalEnergy = getEnergyCharging() + getEnergyInUse();
      setEnergyCharging(getEnergyCharging() + getEnergyInUse());
      setCapacityInUse(0.0);
      setEnergyInUse(0.0);
      
      // Put the strongest batteries in trucks for the next shift
      if (null != newShift) {
        setCapacityInUse(newShift.getTrucks() * batteryCapacity);
        setEnergyInUse(Math.min(getCapacityInUse(), totalEnergy));
        setEnergyCharging(totalEnergy - getEnergyInUse());
      }
      log.info(getName() + ": new shift cInUse " + capacityInUse
               + ", eInUse " + energyInUse + ", eCharging " + energyCharging);
      currentShift = newShift;
    }

    // discharge batteries on active trucks
    if (null != currentShift) {
      double usage =
          Math.max(0.0,
                   normal.sample() * truckStd +
                   truckKW * currentShift.getTrucks());
      double deficit = usage - getEnergyInUse();
      log.debug(getName() + ": trucks use " + usage + " kWh");
      if (deficit > 0.0) {
        log.warn(getName() + ": trucks use more energy than available by "
            + deficit + " kWh");
        addEnergyInUse(deficit);
        addEnergyCharging(-deficit);
      }
      addEnergyInUse(-usage);
    }

    // use energy on chargers, accounting for regulation
    double regulation = getSubscription().getRegulation();
    log.info(getName() + ": regulation " + regulation);
    double energyUsed = useEnergy(regulation);

    // Record energy used
    getSubscription().usePower(energyUsed);
    log.info(getName() + " cInUse " + capacityInUse + ", eInUse "
             + energyInUse + ", eCharging " + energyCharging);
  }

  // Computes energy use by chargers in the current timeslot.
  // Remember that the plan has computed usage in terms of AC power,
  // while the energy going into the batteries is lower by
  // the chargeEfficiency value.
  double useEnergy (double regulation)
  {
    TariffSubscription subscription = getSubscription();
    Tariff tariff = subscription.getTariff();
    ensureCapacityPlan(tariff);

    // positive regulation means we lost energy in the last timeslot
    // and should make it up in the remainder of the shift
    addEnergyCharging(-regulation * chargeEfficiency);
    ShiftEnergy need = plan.getCurrentNeed(getNowInstant());
    if (need.getDuration() <= 0) {
      log.error(getName() + " negative need duration " + need.getDuration());
    }
    need.addEnergy(-regulation);

    // Compute the max and min we could possibly use in this timeslot
    // -- start with max and avail for remainder of shift
    double max = nChargers * maxChargeKW * need.getDuration(); //  shift
    double avail = // for remainder of shift
        nBatteries * batteryCapacity - getCapacityInUse() - getEnergyCharging();
    double maxUsable = Math.min(max, avail) / chargeEfficiency;
    double needed = need.getEnergyNeeded();

    double used = 0;
    RegulationCapacity regCapacity = null;
    if (needed >= maxUsable) {
      // we just use the max, and allow no regulation capacity
      log.info(getName() + ": no slack - need " + needed
               + ", max " + max + ", avail " + avail + ", dur " + need.getDuration());
      used = Math.min(maxUsable, (needed / need.getDuration()));
      regCapacity = new RegulationCapacity(subscription, 0.0, 0.0);
    }
    else if (tariff.isTimeOfUse() || tariff.isVariableRate()) {
      // if the current tariff is not a flat rate, we will just use the
      // planned amout, without offering regulation capacity
      // TODO - figure out how to combine variable prices with regulation
      used = need.getRecommendedUsage()[need.getUsageIndex()];
      regCapacity = new RegulationCapacity(subscription, 0.0, 0.0);
    }
    else {
      // otherwise use energy to maximize regulation capacity
      double slack =
          (maxUsable - needed) / need.getDuration() / 2.0;
      log.info(getName() + " needed " + needed
               + ", maxUsable " + maxUsable
               + ", duration " + need.getDuration());
      used = needed / need.getDuration() + slack;
      regCapacity = new RegulationCapacity(subscription, slack, -slack);
    }

    // use it
    addEnergyCharging(used * chargeEfficiency);
    getSubscription().setRegulationCapacity(regCapacity);
    log.info(getName() + " uses " + used + "kWh, reg cap ("
             + regCapacity.getUpRegulationCapacity() + ", "
             + regCapacity.getDownRegulationCapacity() + ")");
    need.tick();
    need.addEnergy(used);
    return used;
  }

  // Ensures that there is a valid capacity plan in place
  void ensureCapacityPlan (Tariff tariff)
  {
    if (null == plan || !plan.isValid(getNowInstant(), tariff)) {
      plan = getCapacityPlan(tariff, getNowInstant(), getPlanningHorizon());
      plan.createPlan(getEnergyCharging());
    }
  }

  // Computes constraints on future energy needs
  // Amounts are energy needed to run the chargers. Energy input to trucks
  // will be smaller due to charge efficiency.
  ShiftEnergy[] getFutureEnergyNeeds (Instant start, int horizon,
                                      double initialCharging)
  {
    Instant seStart = start;
    int index = indexOfShift(start);
    // current time is likely to be partway into first shift
    Shift currentShift = shiftSchedule[index]; // might be null
    int duration = 0;
    while (shiftSchedule[index] == currentShift) {
      duration += 1;
      index = nextShiftIndex(index);
    }
    Shift nextShift = shiftSchedule[index];
    // this gives us the info we need to start the sequence
    ArrayList<ShiftEnergy> data = new ArrayList<ShiftEnergy>();
    data.add(new ShiftEnergy(seStart, index, duration));
    seStart = seStart.plus(duration * TimeService.HOUR);
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
      data.add(new ShiftEnergy(seStart, index, duration));
      elapsed += duration;
      seStart = seStart.plus(duration * TimeService.HOUR);
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
    Instant now = getNowInstant();
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
      return (now.plus(TimeService.HOUR * (index - nowIndex)));
    }
    return (now.plus(TimeService.HOUR * (shiftSchedule.length + index - nowIndex)));
  }

  private Instant getNowInstant ()
  {
    return service.getTimeslotRepo().currentTimeslot().getStartInstant();
  }
  
  // Get a beginning-of-week time for consistent tariff evaluation
  private Instant getNextSunday ()
  {
    Instant result = getNowInstant();
    int hour = result.get(DateTimeFieldType.hourOfDay());
    if (hour > 0)
      result = result.plus((24 - hour) * TimeService.HOUR);
    int day = result.get(DateTimeFieldType.dayOfWeek());
    result = result.plus((7 - day) * TimeService.DAY);
    return result;
  }

  // ================ getters and setters =====================
  // Note that list values must arrive and depart as List<String>,
  // while internally many of them are lists or arrays of numeric values.
  // Therefore we provide @ConfigurableValue setters that do the translation.
  @Override
  public String getName ()
  {
    return name;
  }

  @Override
  public void setName (String name)
  {
    this.name = name;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "mean power usage when truck is in use")
  @StateChange
  public void setTruckKW (double value)
  {
    truckKW = value;
  }

  public double getTruckKW ()
  {
    return truckKW;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "Std dev of truck power usage")
  @StateChange
  public void setTruckStd (double stdDev)
  {
    truckStd = stdDev;
  }

  public double getTruckStd ()
  {
    return truckStd;
  }

  /**
   * Converts a list of Strings to a sorted list of Shifts. Entries in the
   * list represent pairs of (start, duration) values. 
   */
  @ConfigurableValue(valueType = "List", dump = false,
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

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "size of battery pack in kWh")
  @StateChange
  public void setBatteryCapacity (double value)
  {
    batteryCapacity = value;
  }

  public double getBatteryCapacity ()
  {
    return batteryCapacity;
  }

  @ConfigurableValue(valueType = "Integer", dump = false,
      description = "total number of battery packs")
  @StateChange
  public void setNBatteries (int value)
  {
    nBatteries = value;
  }

  public int getNBatteries ()
  {
    return nBatteries;
  }

  @ConfigurableValue(valueType = "Integer", dump = false,
      description = "number of battery chargers")
  @StateChange
  public void setNChargers (int value)
  {
    nChargers = value;
  }

  public int getNChargers ()
  {
    return nChargers;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "maximum charge rate of one truck's battery pack")
  @StateChange
  public void setMaxChargeKW (double value)
  {
    maxChargeKW = value;
  }

  public double getMaxChargeKW ()
  {
    return maxChargeKW;
  }

  @ConfigurableValue(valueType = "Double", dump = false,
      description = "ratio of charge energy to battery energy")
  @StateChange
  public void setChargeEfficiency (double value)
  {
    chargeEfficiency = value;
  }

  public double getChargeEfficiency ()
  {
    return chargeEfficiency;
  }

  @ConfigurableValue(valueType = "Integer", dump = false,
      description = "planning horizon in timeslots - should be at least 48")
  @StateChange
  public void setPlanningHorizon (int horizon)
  {
    planningHorizon = horizon;
  }

  public int getPlanningHorizon ()
  {
    return planningHorizon;
  }

  @ConfigurableValue(valueType = "Integer", dump = false,
      description = "minimum useful horizon of existing plan")
  @StateChange
  public void setMinPlanningHorizon (int horizon)
  {
    minPlanningHorizon = horizon;
  }

  public int getMinPlanningHorizon ()
  {
    return minPlanningHorizon;
  }

  /**
   * Updates the energy content of offline batteries
   */
  @StateChange
  public void setEnergyCharging (double kwh)
  {
    energyCharging = kwh;
  }

  public double getEnergyCharging ()
  {
    return energyCharging;
  }

  public void addEnergyCharging (double kwh)
  {
    setEnergyCharging(getEnergyCharging() + kwh);
  }

  /**
   * Updates the energy content of in-use batteries
   */
  @StateChange
  public void setEnergyInUse (double kwh)
  {
    energyInUse = kwh;
  }

  public double getEnergyInUse ()
  {
    return energyInUse;
  }

  public void addEnergyInUse (double kwh)
  {
    setEnergyInUse(getEnergyInUse() + kwh);
  }

  /**
   * Updates the total capacity of in-use batteries
   */
  @StateChange
  public void setCapacityInUse (double kwh)
  {
    capacityInUse = kwh;
  }

  public double getCapacityInUse ()
  {
    return capacityInUse;
  }

  // ======== CustomerModelAccessor API ===========

  // digs out the current subscription for this thing. Since the population is
  // always one, there should only ever be one of them
  private TariffSubscription getSubscription ()
  {
    List<TariffSubscription> subs = getCurrentSubscriptions(powerType);
    if (subs.size() > 1) {
      log.warn("Multiple subscriptions " + subs.size() + " for " + getName());
    }
    return subs.get(0);
  }

  private Map<Tariff, CapacityPlan> profiles = null;
  @Override
  public CapacityProfile getCapacityProfile (Tariff tariff)
  {
    if (null == profiles) {
      profiles = new HashMap<Tariff, CapacityPlan>();
    }
    CapacityPlan plan = profiles.get(tariff);
    if (null != plan) {
      return plan.getCapacityProfile();
    }
    plan = getCapacityPlan(tariff, getNextSunday(), getPlanningHorizon());
    profiles.put(tariff, plan);
    plan.createPlan(tariff, 0.0);
    return plan.getCapacityProfile();
  }

  @Override
  public double getBrokerSwitchFactor (boolean isSuperseding)
  {
    if (isSuperseding)
      return 0;
    else
      return 0.02;
  }

  @Override
  public double getTariffChoiceSample ()
  {
    return evalSeed.nextDouble();
  }

  @Override
  public double getInertiaSample ()
  {
    return evalSeed.nextDouble();
  }

  @Override
  public void notifyCustomer (TariffSubscription oldsub,
                              TariffSubscription newsub, int population)
  {
    // method stub
  }

  @Override
  public void evaluateTariffs (List<Tariff> tariffs)
  {
    log.info(getName() + ": evaluate tariffs");
    tariffEvaluator.evaluateTariffs();
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
  
  // ======== Constant-price block within a shift ======
  // Each ShiftBlock is one column in the LP problem
  class ShiftBlock
  {
    ShiftEnergy shiftEnergy;
    int startOffset;
    int duration = 0;
    double cost = 0; // per-hour cost
    
    ShiftBlock (ShiftEnergy shiftEnergy, int startOffset)
    {
      super();
      this.shiftEnergy = shiftEnergy;
      this.startOffset = startOffset;
    }

    int getDuration ()
    {
      return duration;
    }
    
    void incrementDuration ()
    {
      this.duration += 1;
    }

    double getCost ()
    {
      return cost;
    }

    void setCost (double cost)
    {
      if (Double.isNaN(cost)) {
        log.error("cost NaN for truck {}", name);
      }
      this.cost = cost;
    }

    ShiftEnergy getShiftEnergy ()
    {
      return shiftEnergy;
    }

    int getStartOffset ()
    {
      return startOffset;
    }
  }

  // ======== Energy needed, available for a Shift ======
  class ShiftEnergy
  {
    private Instant start;
    private int endIndex; // shift index at start of next Shift or idle period
    private int duration; // in hours
    private double energyNeeded = 0.0; // in kWh at end of duration
    private double maxSurplus = 0.0; // possible kWh beyond needed - can be negative

    // Plan recommendations
    private double[] recommendedUsage;
    private double slack;
    private int usageIndex = 0;

    ShiftEnergy (Instant start, int end, int duration)
    {
      super();
      this.start = start;
      this.endIndex = end;
      Shift next = shiftSchedule[end];
      if (null != next) {
        energyNeeded = next.getTrucks() * next.getDuration() * getTruckKW();
      }
      this.duration = duration;
    }

    Instant getStart ()
    {
      return start;
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
      if (duration < 0) {
        log.error("{} SE start at {} ticked past duration {}",
                  getName(), start.toString(), duration);
      }
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
      energyNeeded = Math.max(0.0, (energyNeeded - energy));
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

    // Returns the current index into the recommendedUsage array. This
    // allows the caller to modify (smash) the array to reflect
    // the effects of regulation and curtailment events
    int getUsageIndex ()
    {
      return usageIndex;
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
      this(tariff, start, getPlanningHorizon());
    }

    // Creates a plan for a custom size
    CapacityPlan (Tariff tariff, Instant start, int size)
    {
      super();
      this.tariff = tariff;
      this.start = start;
      this.size = size;
    }

    // A plan is valid if it's for the given tariff and if there is still
    // sufficient time left on it
    public boolean isValid (Instant now, Tariff tariff)
    {
      if (tariff != this.tariff)
        return false;
      int remaining =
          (int)(size - (now.getMillis() - start.getMillis()) / TimeService.HOUR);
      if (remaining < getMinPlanningHorizon())
        return false;
      return true;
    }

    CapacityProfile getCapacityProfile ()
    {
      return new CapacityProfile(usage, start);
    }

    // Returns the ShiftEnergy instance for the current time
    // Note that for this to work, the ShiftEnergy.tick() method
    // must be called once/timeslot.
    ShiftEnergy getCurrentNeed (Instant when)
    {
      if (null == needs)
        return null;
      for (int i = 0; i < needs.length; i++) {
        // if it's the last one, return it.
        if ((i == needs.length - 1)
            || (needs[i + 1].getStart().isAfter(when))) {
          return needs[i];
        }
      }
      // should never get here
      log.error(getName() + " at " + when.toString()
                + " ran off end of plan length " + size
                + " starting " + start.toString());
      return null;
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
      updateNeeds();
    }

    // returns the ShiftEnergy array used to create the plan,
    // decorated with the most recent solution
    ShiftEnergy[] updateNeeds ()
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
    int size;  // number of hours in plan
    int blockCount = 0; // number of multi-hour blocks in solution

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
      // x is energy use per block for size hours, b is slack var per block.
      // Block is a shift, or portion of shift with constant price.
      // For multi-hour blocks, energy use is evenly distributed across hours
      // after solution.
      Date start = new Date();
      int shifts = needs.length;
      
      // Create blocks that break on both shift boundaries and tariff price
      // boundaries.
      ShiftBlock[] blocks = makeBlocks(shifts); 
      int columns = blocks.length;
      int blockIndex = -1;
      
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
        //double kwh =
        //    needs[i].getEnergyNeeded() + needs[i].getMaxSurplus();
        //for (int j = 0; j < needs[i].getDuration(); j++) {
        while ((blockIndex < blocks.length - 1) &&
                (blocks[blockIndex + 1].getShiftEnergy() == needs[i])) {
          blockIndex += 1;
          // one iteration per block within a shift
          // fill in objective function
          obj[column] = blocks[blockIndex].getCost();
          lb[column] = 0.0;
          if (0.0 == needs[i].getDuration()) {
            log.warn("Zero value in needs[{}]", i);
          }
          ub[column] =
                  (needs[i].getEnergyNeeded() + needs[i].getMaxSurplus())
                  * (double)blocks[blockIndex].getDuration() / needs[i].getDuration();
          if (Double.isNaN(ub[column])) {
            log.warn("NaN upper bound c={}, i={} blockDuration={}, en {}, bd{}, nb{}",
                      column, i,  blocks[blockIndex].getDuration(),
                      needs[i].getEnergyNeeded(), blocks[blockIndex].getDuration(),
                      needs[i].getDuration());
            ub[column] = 0.0;
          }
          column += 1;
          // construct cumulative usage constraints
          //a[i][column] = -1.0;
          //time = time.plus(TimeService.HOUR);
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
        // upper bound is max possible energy for shift
        double validEn = needs[i].getEnergyNeeded();
        if (Double.isNaN(validEn)) {
          log.warn("ub [{}] energyNeeded = NaN", i);
          validEn = 0.0;
        }
        double validSurplus = needs[i].getMaxSurplus();
        if (Double.isNaN(validSurplus)) {
          log.warn("ub[{}] maxSurplus = NaN", i);
          validSurplus = 0.0;
        }
        ub[columns + i] =
            (validEn + validSurplus);
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
        if (returnCode != OptimizationResponse.SUCCESS) {
          log.error(getName() + "bad optimization return code " + returnCode);
        }
        double[] sol = opt.getOptimizationResponse().getSolution();
        Date end = new Date();
        log.info("Solution time: " + (end.getTime() - start.getTime()));
        log.debug("Solution = " + Arrays.toString(sol));
        recordSolution(sol, blocks);
      }
      catch (Exception e) {
        log.error(e.toString());
      }
      // we call it solved whether or not the solution was successful
      solved = true;
    }

    ShiftBlock[] makeBlocks (int shifts)
    {
      ArrayList<ShiftBlock> blocks = new ArrayList<ShiftBlock>();
      double epsilon = 1e-3;  // min price difference to ignore
      Instant time =
              indexToInstant(needs[0].getStartIndex());
      for (int i = 0; i < shifts; i++) {
        // one iteration per shift
        double kwh =
            needs[i].getEnergyNeeded() + needs[i].getMaxSurplus();
        ShiftBlock currentBlock = null;
        double blockCost = 0.0; // per-kWh cost of current block
        //int blockLength = 0;    // length of current block
        for (int j = 0; j < needs[i].getDuration(); j++) {
          // one iteration per timeslot within a shift
          // fill in objective function
          // cost/kWh based on assumption that shift need is evenly distributed
          double kwhPerTs = kwh / needs[i].getDuration();
          double cost = tariff.getUsageCharge(time, kwhPerTs) / kwhPerTs;
          if (Double.isNaN(cost)) {
            log.warn("cost NaN for truck {}", name);
            cost = 0.0;
          }
          if (null == currentBlock) {
            blockCost = cost;
            currentBlock = new ShiftBlock(needs[i], j);
            currentBlock.setCost(blockCost);
            blocks.add(currentBlock);
          }
          else if (Math.abs(cost - blockCost) > epsilon) {
            // start of new block --
            // finish off last block
            currentBlock = new ShiftBlock(needs[i], j);
            currentBlock.setCost(cost);
            blocks.add(currentBlock);
          }
          currentBlock.incrementDuration();
        }
      }
      ShiftBlock[] result = new ShiftBlock[blocks.size()];
      return blocks.toArray(result);
    }

    // pulls apart the usage and slack data
    void recordSolution(double[] lpResult, ShiftBlock[] blocks)
    {
      double[] blockSolution = Arrays.copyOfRange(lpResult, 0, blocks.length);
      log.debug("Block soln: " + Arrays.toString(blockSolution));
      solution = new double[size];
      int solutionIndex = 0;
      int lpIndex = 0;
      for (ShiftBlock block: blocks) {
        double blockValue = blockSolution[lpIndex++] / block.getDuration();
        for (int i = 0; i < block.getDuration(); i++) {
          solution[solutionIndex++] = blockValue;
        }
      }
      log.debug("Usage: " + Arrays.toString(solution));
      slack = Arrays.copyOfRange(lpResult, blocks.length, lpResult.length);
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

  @Override
  public double getShiftingInconvenienceFactor(Tariff tariff) {
    return 0;
  }
}
