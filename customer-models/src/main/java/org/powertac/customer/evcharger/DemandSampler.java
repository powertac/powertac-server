/**
 * Copyright (c) 2022 by John Collins.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.customer.evcharger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.RandomSeed;
import org.powertac.common.config.Configurator;

/**
 * Sampler class which holds the logic of sampling new EV charger demand from
 * the statistical model.
 *
 * @author Philipp Page <github@philipp-page.de>
 */
class DemandSampler
{
  private static final Logger log = LogManager.getLogger(DemandSampler.class.getSimpleName());
  private MixtureMultivariateNormalDistribution pluginProbability;
  private HashMap<String, MixtureMultivariateNormalDistribution> condHorizonDemandProbabilities = new HashMap<>();
  private Long currentSeed;
  private RandomDataGenerator randomSeedGenerator;
  private XMLConfiguration config;
  private boolean enabled = true;

  void initialize (String model, RandomSeed randomSeed)
  {
    this.initialize(model);
    randomSeedGenerator = new RandomDataGenerator();
    randomSeedGenerator.reSeed(randomSeed.getValue());
  }

  void initialize (String model)
  {
    String path = "config/" + model;
    try {
      config = Configurator.readXML(path);
    }
    catch (ConfigurationException e) {
      log.error("Problem loading configuration. Disabling sampler: " + e);
      enabled = false;
    }
    catch (Exception e) {
      log.error("Error loading configuration. Disabling sampler: " + e);
      enabled = false;
    }

    setupPluginProbability();
    setupDemandHorizonProbabilities();
  }

  boolean isEnabled ()
  {
    return enabled;
  }

  void setCurrentSeed (long seedValue)
  {
    this.currentSeed = seedValue;
  }

  // This will parse the XML config for pluginProbability into a
  // MixtureMultivariateNormalDistribution.
  private void setupPluginProbability ()
  {
    if (!isEnabled()) {
      return;
    }
    double[] means = config.get(double[].class, "pluginProbability.means.mean");
    double[] variances = config.get(double[].class, "pluginProbability.covs.cov");
    Array2DRowRealMatrix covariances = new Array2DRowRealMatrix(variances);
    double[] weights = config.get(double[].class, "pluginProbability.weights");
    List<Pair<Double, MultivariateNormalDistribution>> mvns = new LinkedList<>();
    for (int i = 0; i < means.length; i++) {
      // We pass null as random number generator because we do not need sampling
      // of plug-in probability. We evaluate the density at the current hour of
      // day.
      mvns.add(Pair.create(weights[i], new MultivariateNormalDistribution(null, new double[] { means[i] },
                                                                          new double[][] { covariances.getRow(i) })));
    }
    pluginProbability = new MixtureMultivariateNormalDistribution(null, mvns);
  }

  // This will parse the XML config into a
  // HashMap<hour of day, MixtureMultivariateNormalDistribtion>.
  // The demandHorizonProbabilities represent the conditional Gaussian Mixture
  // Model conditional on the hour of day.
  private void setupDemandHorizonProbabilities ()
  {
    if (!isEnabled()) {
      return;
    }
    String[] instances = config.getStringArray("instances");

    for (String instance: instances) {
      final double[] flatMeans = config.get(double[].class, String.format("%s.means.mean", instance));
      final double[] flatCovs = config.get(double[].class, String.format("%s.covs.cov", instance));
      final double[] weights = config.get(double[].class, String.format("%s.weights", instance));

      // We have a bivariate distribution, therefore the mean vectors have
      // length 2.
      final double[][] meanVectors = new double[weights.length][2];
      for (int i = 0; i < flatMeans.length / 2; i++) {
        meanVectors[i] = Arrays.copyOfRange(flatMeans, 2 * i, 2 * i + 2);
      }

      // The bivariate covariance matrices have dimension 2x2.
      final double[][][] covarianceMatrices = new double[weights.length][2][2];
      for (int i = 0; i < flatCovs.length / 2 / 2; i++) {
        double[] row1 = Arrays.copyOfRange(flatCovs, 4 * i, 4 * i + 2);
        double[] row2 = Arrays.copyOfRange(flatCovs, 4 * i + 2, 4 * i + 4);
        covarianceMatrices[i] = new double[][] { row1, row2 };
      }

      condHorizonDemandProbabilities
              .put(instance, new MixtureMultivariateNormalDistribution(weights, meanVectors, covarianceMatrices));
    }
  }

  /**
   * Returns a list of DemandElement instances. One DemandElement represents the
   * energy demand of a cohort of vehicles over a given horizon. A cohort is a
   * group of vehicles who unplug at the same timeslot in the future. This means
   * that they have the same horizon.
   * 
   * @param hod
   *          Current hour of day
   * @param popSize
   *          The population size of chargers
   * @param chargerCapacity
   *          The assumed charger capacity in kWh (e.g. 7.2 kWh)
   * @return A list of DemandElement instances representing the new generated
   *         demand by the statistical model
   */
  List<DemandElement> sample (final int hod, final int popSize, final double chargerCapacity)
  {
    if (!isEnabled()) {
      return new ArrayList<DemandElement>();
    }
    
    // We set a new seed using the randomSeedGenerator each timeslot to make
    // sure that experiments are reproducible for the same random seed (see
    // initialize method).
    if (randomSeedGenerator != null) {
      setCurrentSeed(randomSeedGenerator.nextLong(Long.MIN_VALUE,
                                                  Long.MAX_VALUE));
    }

    // Sample N = new plug-ins in this timeslot.
    int nVehicles = (int) sampleNewPlugins(hod, popSize);

    // Sample N (horizon, energy) tuples.
    double[][] horizonEnergyTuples = sampleHorizonEnergyTuples(nVehicles, hod);

    // Tracks the charger hour histogram for each cohort. The charger hour
    // histogram is a TreeMap where the key is the integer number of charger
    // hours needed and the value the number of vehicles in that group.
    HashMap<Integer, TreeMap<Integer, Integer>> cohortChargerHoursHistogram = new HashMap<>();
    // Tracks the number of vehicles in each cohort.
    HashMap<Integer, Integer> cohortVehicleSum = new HashMap<>();

    // We need to initialize all cohorts until maxHorizon and
    // maxChargerHours respectively to avoid gaps in the HashMap indices.
    // There will be maxHorizon + 1 and maxChargerHours + 1 elements to
    // accommodate for those who need energy immediately.
    int maxHorizon =
      Arrays.stream(horizonEnergyTuples).mapToInt(horizonEnergyTuple -> (int) horizonEnergyTuple[0]).max().getAsInt();
    double maxEnergy =
      Arrays.stream(horizonEnergyTuples).mapToDouble(horizonEnergyTuple -> horizonEnergyTuple[1]).max().getAsDouble();
    int maxChargerHours = (int) (maxEnergy / chargerCapacity);
    for (int i = 0; i <= maxHorizon; i++) {
      // The histogram should be ordered naturally meaning that 0 implies the
      // vehicle needs at least 0 charging hours, 1 means the vehicle needs 1
      // charger hour, ...
      cohortChargerHoursHistogram.put(i, new TreeMap<>(Comparator.naturalOrder()));
      for (int j = 0; j <= maxChargerHours; j++) {
        cohortChargerHoursHistogram.get(i).put(j, 0);
      }
      cohortVehicleSum.put(i, 0);
    }

    // Now, we fill in the the values for the charger hour histograms, vehicle
    // sum and energy sum of each cohort.
    for (double[] horizonEnergyTuple: horizonEnergyTuples) {
      int horizon = (int) horizonEnergyTuple[0];
      double energy = horizonEnergyTuple[1];
      int chargerHours = (int) (energy / chargerCapacity);
      TreeMap<Integer, Integer> histogram = cohortChargerHoursHistogram.get(horizon);
      histogram.merge(Math.min(chargerHours, horizon), 1, Integer::sum);
      cohortChargerHoursHistogram.put(horizon, histogram);

      cohortVehicleSum.merge(horizon, 1, Integer::sum);
    }

    return cohortVehicleSum.keySet().stream()
            .map(horizon -> 
            new DemandElement(horizon, cohortVehicleSum.get(horizon),
                              cohortChargerHoursHistogram.get(horizon)
                              .values().stream().mapToDouble(Integer::doubleValue).toArray()))
            .collect(Collectors.toList());
  }

  /**
   * Sample the number of new plug-ins for a given hour of day. Returns the
   * result as a double to allow for better accuracy in the simulation.
   *
   * @param hod
   *          Current hour of day
   * @param popSize
   *          Population size of chargers
   * @return Number of new plug-ins for the current timeslot
   */
  double sampleNewPlugins (final int hod, final int popSize)
  {
    if (!isEnabled()) {
      return 0.0;
    }
    double result = pluginProbability.density(new double[] { hod }) * popSize;
    final NormalDistribution gaussianNoise = new NormalDistribution(0, result * 0.1);
    if (currentSeed != null) {
      gaussianNoise.reseedRandomGenerator(currentSeed);
    }
    result += gaussianNoise.sample();
    return Math.max(0, result);
  }

  /**
   * Sample n (horizon, energy demand) tuples.
   * 
   * @param n
   *          The number of samples to be retrieved. This is typically given by
   *          {@link DemandSampler#sampleNewPlugins}.
   * @param hod
   *          The current hour of day as integer. Must be in the interval
   *          [0, 23].
   * @throws IllegalArgumentException
   *           If no distribution is found for the given hour of day.
   * @return Tuples of horizon and energy demand. The return dimension is n x 2.
   *         Samples are guaranteed to be larger or equal to zero.
   */
  double[][] sampleHorizonEnergyTuples (final int n, final int hod)
  {
    if (!isEnabled()) {
      return new double[][] {};
    }
    MixtureMultivariateNormalDistribution condDist =
            condHorizonDemandProbabilities.get("hod" + hod);
    if (condDist == null) {
      throw new IllegalArgumentException(String.format("Cannot find distribution for provided hour of day %d.", hod));
    }
    if (currentSeed != null) {
      condDist.reseedRandomGenerator(currentSeed);
    }
    // [[d_1, e_1],
    // [d_2, e_2],
    // ...
    // [d_n, e_n]]
    double[][] horizonEnergyTuples = condDist.sample(n);

    // Make sure to replace negative values by zero (in rare cases the model
    // might return negative values due to the symmetry of the
    // normal distribution).
    for (int i = 0; i < horizonEnergyTuples.length; i++) {
      horizonEnergyTuples[i][0] = Math.max(horizonEnergyTuples[i][0], 0.0);
      horizonEnergyTuples[i][1] = Math.max(horizonEnergyTuples[i][1], 0.0);
    }

    return horizonEnergyTuples;
  }
}
