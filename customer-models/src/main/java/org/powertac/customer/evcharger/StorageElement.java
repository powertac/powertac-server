package org.powertac.customer.evcharger;

import java.util.Arrays;

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
    // Number of active chargers
    private double activeChargers = 0.0;

    // Unsatisfied demand remaining in vehicles that will disconnect in this timeslot
    double[] energy = {0.0};

    // Population allocated to energy requirement breakdown
    private double[] population = {0.0};

    // commitment from previous timeslot, needed to distribute regulation
    //private double previousCommitment = 0.0;

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
      super();
      this.activeChargers = activeChargers;
      this.energy = energy;
      this.population = population;
    }

//    void extendArrays (int newLength)
//    {
//      if (population.length < newLength) {
//        double[] newPop = new double[newLength];
//        double[] newEnergy = new double[newLength];
//        for (int i = 0; i < population.length; i++) {
//          newPop[i] = population[i];
//          newEnergy[i] = energy[i];
//        }
//        population = newPop;
//        energy = newEnergy;
//      }
//    }

    // Shrinks energy and population arrays, dropping the final element
    // which is no longer needed
    void collapseArrays ()
    {
      int len = population.length;
      if (len < 2) {
        // nothing to do here
        return;
      }
      //population[len - 2] += population[len - 1];
      population = Arrays.copyOf(population, len - 1);
      //energy[len - 2] += energy[len - 1];
      energy = Arrays.copyOf(energy, len - 1);
    }

    double getActiveChargers ()
    {
      return activeChargers;
    }

    void addChargers (double n)
    {
      activeChargers += n;
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
      return new StorageElement(getActiveChargers(),
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
      return new StorageElement(getActiveChargers() * scale,
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
      activeChargers += element.getActiveChargers() * scale;
      for (int i = 0; i < population.length; i++) {
        population[i] += element.getPopulation()[i] * scale;
        energy[i] += element.getEnergy()[i] * scale;
      }
    }

    // Scale this element by a constant fraction
    // This should not change the population/energy relationship
    void scale (double fraction)
    {
      //tranche *= fraction;
      activeChargers *= fraction;
      //energy *= fraction;
      for (int i = 0; i < population.length; i++) {
        population[i] *= fraction;
        energy[i] *= fraction;
      }
    }

    // Create a String representation
    public String toString ()
    {
      return String.format("ch%.3f %s %s", activeChargers,
                           Arrays.toString(population), Arrays.toString(energy));
    }
  }