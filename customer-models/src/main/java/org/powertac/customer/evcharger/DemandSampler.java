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

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.config.Configurator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Sampler class which holds the logic of sampling new EV charger demand from
 * the statistical model.
 *
 * @author Philipp Page <github@philipp-page.de>
 */
class DemandSampler
{
  private static final Logger log =
    LogManager.getLogger(DemandSampler.class.getSimpleName());
  private MixtureMultivariateNormalDistribution pluginProbability;
  private Map<String, MixtureMultivariateNormalDistribution> condHorizonDemandProbabilities =
    new HashMap<>();
  private XMLConfiguration config;

  void initialize ()
  {
    // TODO: Make this dynamic
    String path = "config/residential_ev.xml";
    try {
      config = Configurator.readXML(path);
    }
    catch (ConfigurationException e) {
      log.error("Problem loading configuration: " + e);
    }
    catch (Exception e) {
      log.error("Error loading configuration: " + e);
    }

    setupPluginProbability();
    setupDemandHorizonProbabilities();
  }

  // This will parse the XML config into a
  // MixtureMultivariateNormalDistribution.
  private void setupPluginProbability ()
  {
    double[] means = config.get(double[].class, "pluginProbability.means.mean");
    double[] variances =
      config.get(double[].class, "pluginProbability.covs.cov");
    Array2DRowRealMatrix covariances = new Array2DRowRealMatrix(variances);
    double[] weights = config.get(double[].class, "pluginProbability.weights");
    List<Pair<Double, MultivariateNormalDistribution>> mvns =
      new LinkedList<>();
    for (int i = 0; i < means.length; i++) {
      mvns.add(Pair
              .create(weights[i],
                      new MultivariateNormalDistribution(new double[] { means[i] },
                                                         new double[][] { covariances
                                                                 .getRow(i) })));
    }
    pluginProbability = new MixtureMultivariateNormalDistribution(mvns);
  }

  // This will parse the XML config into a
  // HashMap<hour of day, MixtureMultivariateNormalDistribtion>.
  // The demandHorizonProbabilities represent the conditional Gaussian Mixture
  // Model.
  private void setupDemandHorizonProbabilities ()
  {
    String[] instances = config.getStringArray("instances");

    for (String instance: instances) {
      final double[] flatMeans =
        config.get(double[].class, String.format("%s.means.mean", instance));
      final double[] flatCovs =
        config.get(double[].class, String.format("%s.covs.cov", instance));
      final double[] weights =
        config.get(double[].class, String.format("%s.weights", instance));

      final double[][] meanVectors = new double[weights.length][2];
      for (int i = 0; i < flatMeans.length / 2; i++) {
        meanVectors[i] = Arrays.copyOfRange(flatMeans, 2 * i, 2 * i + 2);
      }

      final double[][][] covarianceMatrices = new double[weights.length][2][2];
      for (int i = 0; i < flatCovs.length / 2 / 2; i++) {
        double[] row1 = Arrays.copyOfRange(flatCovs, 4 * i, 4 * i + 2);
        double[] row2 = Arrays.copyOfRange(flatCovs, 4 * i + 2, 4 * i + 4);
        covarianceMatrices[i] = new double[][] { row1, row2 };
      }

      condHorizonDemandProbabilities
              .put(instance,
                   new MixtureMultivariateNormalDistribution(weights,
                                                             meanVectors,
                                                             covarianceMatrices));
    }
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
    double result = pluginProbability.density(new double[] { hod }) * popSize;
    final NormalDistribution gaussianNoise =
      new NormalDistribution(0, result * 0.1);
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
   *          The current hour of day as integer. Must be in the interval [0,
   *          23].
   * @return Tuples of horizon and energy demand. The return dimension is n x 2.
   *         Samples are guaranteed to be larger or equal to zero.
   */
  double[][] sampleHorizonEnergyTuples (final int n, final int hod)
  {
    MixtureMultivariateNormalDistribution condDist =
      condHorizonDemandProbabilities.get("hod" + hod);
    if (condDist == null) {
      throw new IllegalArgumentException(String
              .format("Cannot find distribution for provided hour of day %s.",
                      hod));
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
      horizonEnergyTuples[i][0] = Math.max(horizonEnergyTuples[i][0], 0);
      horizonEnergyTuples[i][1] = Math.max(horizonEnergyTuples[i][1], 0);
    }

    return horizonEnergyTuples;
  }
}
