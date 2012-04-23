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

import java.util.Random;
import org.w3c.dom.*;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.*; 
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Container class for one a large set of probability distribution samplers.
 * The various samplers are implemented as nested classes.
 * 
 * @author Prashant Reddy
 */
final class ProbabilityDistribution
{       
    enum DistType { DEGENERATE, POINTMASS, UNIFORM, INTERVAL, NORMAL, GAUSSIAN, STDNORMAL, LOGNORMAL, 
                    CAUCHY, BETA, BINOMIAL, POISSON, CHISQUARED, EXPONENTIAL, GAMMA, WEIBULL, STUDENT, SNEDECOR }  

    private RandomSeedRepo randomSeedRepo;

    private static long distCounter = 0;
    private final long distId = ++distCounter;

    private final DistType type;
    private final Sampler sampler;
    private double param1, param2, param3, param4;
        
    
    ProbabilityDistribution(Element xml)
    {
        randomSeedRepo = (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");

        type = Enum.valueOf(DistType.class, xml.getAttribute("distribution"));
        switch (type) {
        case POINTMASS:
        case DEGENERATE:
            param1 = Double.parseDouble(xml.getAttribute("value"));
            sampler = new DegenerateSampler(param1);
            break;
        case UNIFORM:
            param1 = Double.parseDouble(xml.getAttribute("low"));
            param2 = Double.parseDouble(xml.getAttribute("high"));
            sampler = new UniformSampler(param1, param2);
            break;
        case INTERVAL:
            param1 = Double.parseDouble(xml.getAttribute("mean"));
            param2 = Double.parseDouble(xml.getAttribute("stdDev"));
            param3 = Double.parseDouble(xml.getAttribute("low"));
            param4 = Double.parseDouble(xml.getAttribute("high")); 
            sampler = new IntervalSampler(param1, param2, param3, param4);
            break;
        case NORMAL:
        case GAUSSIAN:
            param1 = Double.parseDouble(xml.getAttribute("mean"));
            param2 = Double.parseDouble(xml.getAttribute("stdDev"));
            sampler = new ContinuousSampler(new NormalDistributionImpl(param1, param2));
            break;
        case STDNORMAL:
            param1 = 0; param2 = 1;
            sampler = new ContinuousSampler(new NormalDistributionImpl(param1, param2));
            break;
        case LOGNORMAL:
            param1 = Double.parseDouble(xml.getAttribute("expMean"));
            param2 = Double.parseDouble(xml.getAttribute("expStdDev"));
            sampler = new LogNormalSampler(param1, param2);
            break;         
        case CAUCHY:
            param1 = Double.parseDouble(xml.getAttribute("median"));
            param2 = Double.parseDouble(xml.getAttribute("scale"));
            sampler = new ContinuousSampler(new CauchyDistributionImpl(param1, param2));
            break;
        case BETA:
            param1 = Double.parseDouble(xml.getAttribute("alpha"));
            param2 = Double.parseDouble(xml.getAttribute("beta"));
            sampler = new ContinuousSampler(new BetaDistributionImpl(param1, param2));
            break;
        case BINOMIAL:
            param1 = Double.parseDouble(xml.getAttribute("trials"));
            param2 = Double.parseDouble(xml.getAttribute("success"));
            sampler = new DiscreteSampler(new BinomialDistributionImpl((int) param1, param2));
            break;
        case POISSON:
            param1 = Double.parseDouble(xml.getAttribute("lambda"));
            sampler = new DiscreteSampler(new PoissonDistributionImpl(param1));
            break;
        case CHISQUARED:
            param1 = Double.parseDouble(xml.getAttribute("dof"));
            sampler = new ContinuousSampler(new ChiSquaredDistributionImpl(param1));
            break;
        case EXPONENTIAL:
            param1 = Double.parseDouble(xml.getAttribute("mean"));
            sampler = new ContinuousSampler(new ExponentialDistributionImpl(param1));
            break;
        case GAMMA:
            param1 = Double.parseDouble(xml.getAttribute("alpha"));
            param2 = Double.parseDouble(xml.getAttribute("beta"));
            sampler = new ContinuousSampler(new GammaDistributionImpl(param1, param2));
            break;
        case WEIBULL:
            param1 = Double.parseDouble(xml.getAttribute("alpha"));
            param2 = Double.parseDouble(xml.getAttribute("beta"));
            sampler = new ContinuousSampler(new WeibullDistributionImpl(param1, param2));
            break;
        case STUDENT:
            param1 = Double.parseDouble(xml.getAttribute("dof"));
            sampler = new ContinuousSampler(new TDistributionImpl(param1));
            break;
        case SNEDECOR:
            param1 = Double.parseDouble(xml.getAttribute("d1"));
            param2 = Double.parseDouble(xml.getAttribute("d2"));
            sampler = new ContinuousSampler(new FDistributionImpl(param1, param2));
            break;
        default: throw new Error("Invalid probability distribution type!");
        } 
        sampler.reseedRandomGenerator(randomSeedRepo.getRandomSeed("factoredcustomer.ProbabilityDistribution", 
                                                                   distId, "Sampler").getValue());
    }
        
    double drawSample()
    {
        try {
            return sampler.sample();
        } 
        catch (MathException e) 
        {
            System.err.println("ProbabilityDistribution(" + toString() + ") - drawSample():" + toString() + " Caught MathException:\n");
            e.printStackTrace(System.err);
            throw new Error("ProbabilityDistribution(" + toString() + ") - drawSample(): Caught MathException: " + e.toString());
        }
    }
 
    @Override
    public String toString() 
    {
        return this.getClass().getCanonicalName() + ":" + distId + ":" + type + 
               "(" + param1 + ", " + param2 + ", " + param3 + ", " + param4 + ")";
    }

    
    ///////////////////////////// HELPER CLASSES //////////////////////////////

    interface Sampler
    {
        public void reseedRandomGenerator(long seed);
        
        public double sample() throws MathException;
    }
    
    final class DegenerateSampler implements Sampler
    {
        final double value;
        
        DegenerateSampler(double v) { value = v; }
        
        public void reseedRandomGenerator(long seed) {}
            
        public double sample()  throws MathException { return value; }
    }
    
    final class UniformSampler implements Sampler
    {
        final Random random;
        final double low;
        final int range;
        
        UniformSampler(double l, double h) 
        { 
            low = l; 
            range = safeLongToInt(Math.round(h - low));
            random = new Random();
        }
        
        public void reseedRandomGenerator(long seed)
        {
            random.setSeed(seed);
        }
        
        public double sample() throws MathException
        {
            return low + random.nextInt(range);
        }

        protected int safeLongToInt(long x) 
        {
            if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(x + " cannot be cast to int without changing its value.");
            } else return (int) x;
        }
    }
    
    final class IntervalSampler implements Sampler
    {
        final double low;
        final double high;
        final NormalDistributionImpl normalSampler;

        IntervalSampler(double m, double s, double l, double h) 
        {
            normalSampler = new NormalDistributionImpl(m, s);
            low = l;
            high = h;
        }
        
        public void reseedRandomGenerator(long seed)
        {
            normalSampler.reseedRandomGenerator(seed);
        }
        
        public double sample() throws MathException
        {
            return Math.min(high, Math.max(low, normalSampler.sample()));
        }
    }
    
    final class LogNormalSampler implements Sampler
    {
        final NormalDistributionImpl normalSampler;

        LogNormalSampler(double m, double s) 
        {
            normalSampler = new NormalDistributionImpl(Math.log(m), Math.log(s));
        }
        
        public void reseedRandomGenerator(long seed)
        {
            normalSampler.reseedRandomGenerator(seed);
        }
        
        public double sample() throws MathException
        {
            return Math.exp(normalSampler.sample());
        }
    }
    
    final class DiscreteSampler implements Sampler
    {
        final AbstractIntegerDistribution impl;
        
        DiscreteSampler(AbstractIntegerDistribution i)
        {
            impl = i;
        }
        
        public void reseedRandomGenerator(long seed)
        {
            impl.reseedRandomGenerator(seed);
        }
        
        public double sample() throws MathException
        {
            return impl.sample();
        }
    }
    
    final class ContinuousSampler implements Sampler
    {
        final AbstractContinuousDistribution impl;
        
        ContinuousSampler(AbstractContinuousDistribution i)
        {
            impl = i;
        }
        
        public void reseedRandomGenerator(long seed)
        {
            impl.reseedRandomGenerator(seed);
        }
        
        public double sample() throws MathException
        {
            return impl.sample();
        }
    }
    
}

