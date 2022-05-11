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

import java.util.LinkedList;
import java.util.List;

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

  /**
   * Sample the number of new plugins for a given hour of day. Returns the
   * result as a double to allow for better accuracy in the simulation.
   *
   * @param hod
   *          Current hour of day
   * @param popSize
   *          Population size of chargers
   * @return Number of new plugins for the current timeslot
   */
  double sampleNewPlugins (final int hod, final int popSize)
  {
    double result = pluginProbability.density(new double[] { hod }) * popSize;
    final NormalDistribution gaussianNoise =
      new NormalDistribution(0, result * 0.1);
    result += gaussianNoise.sample();
    return Math.max(0, result);
  }
}
