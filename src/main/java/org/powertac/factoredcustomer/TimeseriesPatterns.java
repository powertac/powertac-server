/*
* Copyright 2011 the original author or authors.
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

import java.io.*;

/**
 * Utility class that generates various time series patterns that can be 
 * used as base capacity series by implementations of @code{CapacityManager}.
 * 
 * @author Prashant Reddy
 */
final class TimeseriesPatterns
{
    static final int MAX_TIMESERIES_LENGTH = 2400;

    static InputStream generateTimeseriesStream(String seriesName)
        throws java.io.IOException
    {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(); 
        DataOutputStream dataOut = new DataOutputStream(bytesOut);

        if (seriesName.equals("DailyWeekly100")) {  
            final double[] dC = { 100, 120, 120, 120, 120, 120, 150 };
            final double[] hC = { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.1, 1.2, 1.4, 1.4, 1.2, 1.1, 
                                  1.1, 1.1, 1.1, 1.1, 1.2, 1.4, 1.5, 1.6, 1.5, 1.3, 1.1, 1.0 };
            for (int i=0; i < MAX_TIMESERIES_LENGTH; ++i) {
                dataOut.writeDouble(dC[i % 7] * hC[i % 24]);
            }
        } else throw new Error("Unknown builtin series name: " + seriesName);
        return new ByteArrayInputStream(bytesOut.toByteArray());
    }
}
