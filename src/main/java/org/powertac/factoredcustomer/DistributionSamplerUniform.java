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

/**
 * @author Prashant Reddy
 * Continuous uniform distribution.
 */
class DistributionSamplerUniform extends DistributionSamplerBase
{
    double low;
    double high;

    DistributionSamplerUniform(double l, double h) 
    {
        low = l;
        high = h;
    }
    
    double sample()
    {
	return low + random.nextInt(safeLongToInt(Math.round(high - low)));
    }

    protected static int safeLongToInt(long l) 
    {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
	
} // end class

