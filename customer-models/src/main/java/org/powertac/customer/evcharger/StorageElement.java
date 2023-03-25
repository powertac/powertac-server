package org.powertac.customer.evcharger;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** ---------------------------------------------------------------------------------------
   * Mutable element of the StorageState forward capacity vector for the EV Charger model.
   * Each contains a capacity histogram of length n + 1 for a timeslot n slots in the future.
   * 
   * Max demand is simply the sum of individual capacities of the chargers, constrained by to
   * remaining unfilled capacity in the batteries of attached vehicles.
   * 
   * Min demand is the minimum amount that must be consumed in a given timeslot to meet the
   * charging requirements of attached vehicles, constrained by max demand.
   * 
   * @author John Collins
   *
   */
  class StorageElement // package visibility
  {
    static Logger log =
            LogManager.getLogger(StorageElement.class.getName());

    // Number of active chargers
    // TODO - it's possible this activeChargers value is not used anywhere
    //private double activeChargers = 0.0;

    // Unsatisfied demand remaining in vehicles that will disconnect in this timeslot
    double[] energy = {0.0};

    // Population allocated to energy requirement breakdown
    private double[] population = {0.0};

    private double epsilon = 1e-8;

    // default constructor
    StorageElement (int arrayLength)
    {
      super();
      this.energy = new double[arrayLength];
      this.population = new double[arrayLength];
    }

    // populated constructor
    StorageElement (double activeChargers, double[] energy, double[] population)
    {
      this(energy, population);
    }

    StorageElement (double[] energy, double[] population)
    {
      super();
      //this.activeChargers = activeChargers;
      this.energy = energy;
      this.population = population;
    }

    public static StorageElement restoreElement (int length, String data)
    {
      // data is "n, n] [n, n] if length == 2"
      Pattern num = Pattern.compile("(\\d+.\\d+),?\\]? ?");
      Matcher m;
      StorageElement se = new StorageElement(length);
      String remains = data;
      // population
      for (int count = 0; count < length; count++) {
        m = num.matcher(remains);
        if (m.lookingAt()) {
          se.population[count] = Double.valueOf(m.group(1));
          remains = remains.substring(m.end());
        }
        else {
          log.error("Failed to match population value, seeing {}", remains);
          return null;
        }
      }
      // end of population array
      remains = remains.substring(1); // skip opening bracket
      // energy
       for (int count = 0; count < length; count++) {
        m = num.matcher(remains);
        if (m.lookingAt()) {
          se.energy[count] = Double.valueOf(m.group(1));
          remains = remains.substring(m.end());
        }
        else {
          log.error("Should be looking at population value, seeing {}", remains);
          return null;
        }
      }
      //System.out.println("finished " + length);
      return se;
    }

    // Shrinks energy and population arrays, dropping the final element
    // which is no longer needed
    void collapseArrays ()
    {
      int len = population.length;
      if (len < 2) {
        // nothing to do here
        return;
      }
      population = Arrays.copyOf(population, len - 1);
      energy = Arrays.copyOf(energy, len - 1);
    }

    // Moves energy and population to smaller indices as needed to preserve
    // hourly constraints
    void rebalance (double chargerCapacity)
    {
      // This only works if there are multiple groups
      if (1 == population.length)
        return;
      // Each group i should have energy ratio <= (len - i - 1) + 0.5
      for (int i = energy.length - 1; i > 0; i--) {
        //note that we are not moving energy and population above index 0
        //first, find the surplus in this timeslot
        double xRatio = (energy.length - 1 - i) + 0.5; // current cell
        if (population[i] < epsilon) {
          // clear this one out
          population[i] = 0.0;
          energy[i] = 0.0;
          continue;
        }
        double chunk = population[i] * chargerCapacity;
        double currentRatio = 0.0;
        currentRatio = energy[i] / chunk;
        if (currentRatio <= xRatio) {
          continue;
        }
        else {
          double move = (currentRatio - xRatio);
          if (move > 1.0 + epsilon || move < 0.0 - epsilon) {
            log.error("Move ratio = {} out of range", move);
          }
          double moveP = population[i] * move;
          population[i] -= moveP;
          population[i - 1] += moveP;
          double moveE = energy[i] - population[i] * chargerCapacity * xRatio;
          energy[i] -= moveE;
          energy[i - 1] += moveE;
        }
      }
    }

    double[] getRemainingCommitment ()
    {
      return energy;
    }

    double[] getPopulation ()
    {
      return population;
    }

    double[] getEnergy ()
    {
      return energy;
    }

    // Adjusts commitment, either as a result of exercised regulation, or as
    // a result of new demand
    void addCommitments (double[] population, double[] energy) {
      // We assume our arrays have already been re-sized
      if (population.length > this.population.length)
        StorageState.log.error("array size mismatch {} into {}", population.length, this.population.length);
      for (int i = 0; i < population.length; i++) {
        this.population[i] += population[i];
        this.energy[i] += energy[i];
      }
    }

    // returns a new StorageElement with the same contents as an old one
    StorageElement copy ()
    {
      return new StorageElement(//getActiveChargers(),
                                Arrays.copyOf(energy, energy.length),
                                Arrays.copyOf(population, population.length));
    }

    // returns a new StorageElement containing a portion of an old element
    StorageElement copyScaled (double scale)
    {
      double[] scaledPop = new double[population.length];
      double[] scaledEnergy = new double[energy.length];
      for (int i = 0; i < population.length; i++) {
        scaledPop[i] = population[i] * scale;
        scaledEnergy[i] = energy[i] * scale;
      }
      return new StorageElement(//getActiveChargers() * scale,
                                scaledEnergy, scaledPop);
    }

    // adds a portion of an existing element to the contents of this one
    void addScaled (StorageElement element, double scale)
    {
      if (element.getPopulation().length != population.length) {
        // should not happen
        StorageState.log.error("Attempt to add element of length {} to element of length {}",
                  population.length, element.getPopulation().length);
      }
      //activeChargers += element.getActiveChargers() * scale;
      for (int i = 0; i < population.length; i++) {
        population[i] += element.getPopulation()[i] * scale;
        energy[i] += element.getEnergy()[i] * scale;
      }
    }

    // Scale this element by a constant fraction
    // This should not change the population/energy relationship
    void scale (double fraction)
    {
      for (int i = 0; i < population.length; i++) {
        population[i] *= fraction;
        energy[i] *= fraction;
      }
    }

    // Returns the charger-hours needed for each cohort
    double[] getRatios (double chargerCapacity)
    {
      double[] result = new double[population.length];
      for (int i = 0; i < result.length; i++) {
        if (population[i] < 1e-9) {
          result[i] = 0.0;
        }
        else {
          result[i] = energy[i] / chargerCapacity / population[i];
        }
      }
      return result;
    }

    // Create a String representation
    public String toString ()
    {
      return String.format("SE %s %s", //activeChargers,
                           Arrays.toString(population), Arrays.toString(energy));
    }
  }