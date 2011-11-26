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

import org.w3c.dom.*;

/**
 * @author Prashant
 *
 */
class ProbabilityDistribution
{
    enum DistributionType { DEGENERATE, POINTMASS, UNIFORM, NORMAL, GAUSSIAN,  LOGNORMAL, Z, STDNORMAL, INTERVAL };
             //	CAUCHY, BETA, BINOMIAL, POISSON, CHISQUARED, EXPONENTIAL, GAMMA, WEIBULL, T, STUDENT, F, SNEDECOR, }
	
    DistributionType distributionType;	
    long seed;
    DistributionSamplerBase sampler;
	
    /**
     * @param xml - XML GPathResult from config
     */
    ProbabilityDistribution(Element xml, long randomSeed)
    {
	seed = randomSeed;
	distributionType = Enum.valueOf(DistributionType.class, xml.getAttribute("probability"));
	switch (distributionType) {
	case POINTMASS:
	case DEGENERATE:
	    double value = Double.parseDouble(xml.getAttribute("value"));
	    sampler = new DistributionSamplerDegenerate(value);
	    break;
	case UNIFORM:
	    double low = Double.parseDouble(xml.getAttribute("low"));
	    double high = Double.parseDouble(xml.getAttribute("high"));
	    sampler = new DistributionSamplerUniform(low, high);
	    break;
	case NORMAL:
	case GAUSSIAN:
	    double mean = Double.parseDouble(xml.getAttribute("mean"));
	    double stdDev = Double.parseDouble(xml.getAttribute("stdDev"));
	    sampler = new DistributionSamplerNormal(mean, stdDev);
	    break;
	case LOGNORMAL:
	    double expMean = Math.log(Double.parseDouble(xml.getAttribute("expMean")));
	    double expStdDev = Math.log(Double.parseDouble(xml.getAttribute("expStdDev")));
	    sampler = new DistributionSamplerNormal(expMean, expStdDev);
	    break;
	case Z:
	case STDNORMAL:
	    sampler = new DistributionSamplerNormal(0, 1);
	    break;
	case INTERVAL:
	    double imean = Double.parseDouble(xml.getAttribute("mean"));
	    double istdDev = Double.parseDouble(xml.getAttribute("stdDev"));
	    double ilow = Double.parseDouble(xml.getAttribute("low"));
	    double ihigh = Double.parseDouble(xml.getAttribute("high")); 
	    sampler = new DistributionSamplerInterval(imean, istdDev, ilow, ihigh);
	    break;
	                /**
	                case CAUCHY:
				param1 = xml.getAttribute("median"));
				param2 = xml.getAttribute("scale"));
				sampler = new CauchyDistributionImpl(param1, param2)
				break
			case BETA:
				param1 = xml.getAttribute("alpha"));
				param2 = xml.getAttribute("beta"));
				sampler = new BetaDistributionImpl(param1)
				break
			case BINOMIAL:
				param1 = xml.getAttribute("trials").toInteger()
				param2 = xml.getAttribute("success"));
				sampler = new BinomialDistributionImpl(param1, param2)
				break
			case POISSON:
				param1 = xml.getAttribute("lambda"));
				sampler = new PoissonDistributionImpl(param1)
				break
			case CHISQUARED:
				param1 = xml.getAttribute("dof"));
				sampler = new ChiSquaredDistributionImpl(param1)
				break
			case EXPONENTIAL:
				param1 = xml.getAttribute("mean"));
				sampler = new ExponentialDistributionImpl(param1)
				break
			case GAMMA:
				param1 = xml.getAttribute("alpha"));
				param2 = xml.getAttribute("beta"));
				sampler = new BetaDistributionImpl(param1)
				break
			case WEIBULL:
				param1 = xml.getAttribute("alpha"));
				param2 = xml.getAttribute("beta"));
				sampler = new WeibullDistributionImpl(param1, param2)
				break
			case [T, STUDENT]:
				param1 = xml.getAttribute("dof"));
				sampler = new TDistributionImpl(param1, param2)
				break
			case [F, SNEDECOR]:
				param1 = xml.getAttribute("d1"));
				param2 = xml.getAttribute("d2"));
				sampler = new FDistributionImpl(param1, param2)
				break
			**/
	default:  throw new Error("Shouldn't be getting to unknown distributionType: " + distributionType);
	}
	sampler.init(seed);
    }
	
    double drawSample()
    {
        switch (distributionType) {
	case LOGNORMAL:
	    return Math.exp(sampler.sample());	
	default:
	    return sampler.sample();
        }
    }
	
    public String toString()
    {
	return "ProbabilityDistribution(distributionType: " + distributionType + ", seed: " + seed;
    }

} // end class

