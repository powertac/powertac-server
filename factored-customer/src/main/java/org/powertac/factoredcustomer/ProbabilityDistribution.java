/* Copyright 2011 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an
* "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
* either express or implied. See the License for the specific language
* governing permissions and limitations under the License.
*/

package org.powertac.factoredcustomer;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;
import org.powertac.common.config.ConfigurableValue;
import org.powertac.factoredcustomer.interfaces.StructureInstance;
import org.powertac.factoredcustomer.utils.SeedIdGenerator;

import java.util.Random;


/**
 * Container class for one a large set of probability distribution samplers.
 * The various samplers are implemented as nested classes.
 *
 * @author Prashant Reddy
 */
public class ProbabilityDistribution implements StructureInstance
{
  private enum DistributionType
  {
    DEGENERATE, POINTMASS, UNIFORM, INTERVAL, NORMAL, GAUSSIAN, STDNORMAL,
    LOGNORMAL, CAUCHY, BETA, BINOMIAL, POISSON, CHISQUARED, EXPONENTIAL,
    GAMMA, WEIBULL, STUDENT, SNEDECOR
  }

  private String name;

  @ConfigurableValue(valueType = "String", dump = false)
  private String distribution;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double value;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double low;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double high;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double mean;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double stdDev;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double expMean;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double expStdDev;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double median;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double scale;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double alpha;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double beta;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double trials;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double success;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double lambda;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double dof;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double d1;
  @ConfigurableValue(valueType = "Double", dump = false)
  private double d2;

  private Sampler sampler;
  // TODO Check if we need to keep these, only used in toString
  private double param1, param2, param3, param4;

  public ProbabilityDistribution (String name)
  {
    this.name = name;
  }

  public void initialize (FactoredCustomerService service)
  {
    switch (DistributionType.valueOf(distribution)) {
      case POINTMASS:
      case DEGENERATE:
        param1 = value;
        sampler = new DegenerateSampler(value);
        break;
      case UNIFORM:
        param1 = low;
        param2 = high;
        sampler = new UniformSampler(low, high);
        break;
      case INTERVAL:
        param1 = mean;
        param2 = stdDev;
        param3 = low;
        param4 = high;
        sampler = new IntervalSampler(mean, stdDev, low, high);
        break;
      case NORMAL:
      case GAUSSIAN:
        param1 = mean;
        param2 = stdDev;
        sampler = new ContinuousSampler(new NormalDistribution(mean, stdDev));
        break;
      case STDNORMAL:
        param1 = 0;
        param2 = 1;
        sampler = new ContinuousSampler(new NormalDistribution(0, 1));
        break;
      case LOGNORMAL:
        param1 = expMean;
        param2 = expStdDev;
        sampler = new LogNormalSampler(expMean, expStdDev);
        break;
      case CAUCHY:
        param1 = median;
        param2 = scale;
        sampler = new ContinuousSampler(new CauchyDistribution(median, scale));
        break;
      case BETA:
        param1 = alpha;
        param2 = beta;
        sampler = new ContinuousSampler(new BetaDistribution(alpha, beta));
        break;
      case BINOMIAL:
        param1 = trials;
        param2 = success;
        sampler = new DiscreteSampler(new BinomialDistribution((int) trials, success));
        break;
      case POISSON:
        param1 = lambda;
        sampler = new DiscreteSampler(new PoissonDistribution(lambda));
        break;
      case CHISQUARED:
        param1 = dof;
        sampler = new ContinuousSampler(new ChiSquaredDistribution(dof));
        break;
      case EXPONENTIAL:
        param1 = mean;
        sampler = new ContinuousSampler(new ExponentialDistribution(mean));
        break;
      case GAMMA:
        param1 = alpha;
        param2 = beta;
        sampler = new ContinuousSampler(new GammaDistribution(alpha, beta));
        break;
      case WEIBULL:
        param1 = alpha;
        param2 = beta;
        sampler = new ContinuousSampler(new WeibullDistribution(alpha, beta));
        break;
      case STUDENT:
        param1 = dof;
        sampler = new ContinuousSampler(new TDistribution(dof));
        break;
      case SNEDECOR:
        param1 = d1;
        param2 = d2;
        sampler = new ContinuousSampler(new FDistribution(d1, d2));
        break;
      default:
        throw new Error("Invalid probability distribution type!");
    }

    sampler.reseedRandomGenerator
        (service.getRandomSeedRepo().getRandomSeed
            ("factoredcustomer.ProbabilityDistribution",
                SeedIdGenerator.getId(), "Sampler").getValue());
  }

  public double drawSample ()
  {
    return sampler.sample();
  }

  @Override
  public String toString ()
  {
    return getClass().getCanonicalName() + ":" + distribution +
        ":(" + param1 + ", " + param2 + ", " + param3 + ", " + param4 + ")";
  }

  public String getName ()
  {
    return name;
  }

  ///////////////////////////// HELPER CLASSES //////////////////////////////

  private interface Sampler
  {
    public void reseedRandomGenerator (long seed);

    public double sample ();
  }

  private final class DegenerateSampler implements Sampler
  {
    final double value;

    DegenerateSampler (double v)
    {
      value = v;
    }

    @Override
    public void reseedRandomGenerator (long seed)
    {
    }

    @Override
    public double sample ()
    {
      return value;
    }
  }

  private final class UniformSampler implements Sampler
  {
    final Random random;
    final double low;
    final int range;

    UniformSampler (double l, double h)
    {
      low = l;
      range = safeLongToInt(Math.round(h - low));
      random = new Random();
    }

    @Override
    public void reseedRandomGenerator (long seed)
    {
      random.setSeed(seed);
    }

    @Override
    public double sample ()
    {
      return low + random.nextInt(range);
    }

    protected int safeLongToInt (long x)
    {
      if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE) {
        throw new IllegalArgumentException(x +
            " cannot be cast to int without changing its value.");
      }
      else {
        return (int) x;
      }
    }
  }

  private final class IntervalSampler implements Sampler
  {
    final double low;
    final double high;
    final NormalDistribution normalSampler;

    IntervalSampler (double m, double s, double l, double h)
    {
      normalSampler = new NormalDistribution(m, s);
      low = l;
      high = h;
    }

    @Override
    public void reseedRandomGenerator (long seed)
    {
      normalSampler.reseedRandomGenerator(seed);
    }

    @Override
    public double sample ()
    {
      return Math.min(high, Math.max(low, normalSampler.sample()));
    }
  }

  private final class LogNormalSampler implements Sampler
  {
    final NormalDistribution normalSampler;

    LogNormalSampler (double m, double s)
    {
      normalSampler = new NormalDistribution(Math.log(m), Math.log(s));
    }

    @Override
    public void reseedRandomGenerator (long seed)
    {
      normalSampler.reseedRandomGenerator(seed);
    }

    @Override
    public double sample ()
    {
      return Math.exp(normalSampler.sample());
    }
  }

  private final class DiscreteSampler implements Sampler
  {
    final AbstractIntegerDistribution impl;

    DiscreteSampler (AbstractIntegerDistribution i)
    {
      impl = i;
    }

    @Override
    public void reseedRandomGenerator (long seed)
    {
      impl.reseedRandomGenerator(seed);
    }

    @Override
    public double sample ()
    {
      return impl.sample();
    }
  }

  private final class ContinuousSampler implements Sampler
  {
    final AbstractRealDistribution impl;

    ContinuousSampler (AbstractRealDistribution i)
    {
      impl = i;
    }

    @Override
    public void reseedRandomGenerator (long seed)
    {
      impl.reseedRandomGenerator(seed);
    }

    @Override
    public double sample ()
    {
      return impl.sample();
    }
  }
}
