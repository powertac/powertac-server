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

import java.util.Map;

/**
 * Utility class with functions used to build the various structure objects.
 * 
 * @author Prashant Reddy
 */
final class ParserFunctions
{
    static double[] parseDoubleArray(String input) {
        String[] items = input.split(",");
        double[] ret = new double[items.length];
        for (int i=0; i < items.length; ++i) {
            ret[i] = Double.parseDouble(items[i]);
        }
        return ret;
    }
    
    static double[][] parseMapToDoubleArray(String input) {
        String[] pairs = input.split(",");
        double[][] ret = new double[pairs.length][2];
        for (int i=0; i < pairs.length; ++i) {
            String[] vals = pairs[i].split(":");
            ret[i][0] = Double.parseDouble(vals[0]);
            ret[i][1] = Double.parseDouble(vals[1]);
        }
        return ret;
    }

    static void parseRangeMap(String input, Map<Integer, Double> map) 
    {
        String[] pairs = input.split(",");
        for (int i=0; i < pairs.length; ++i) {
            String[] parts = pairs[i].split(":");
            Double value = Double.parseDouble(parts[1]);
            String[] range = parts[0].split("~");
            Integer start = Integer.parseInt(range[0].trim());
            Integer end = Integer.parseInt(range[1].trim());
            for (Integer key=start; key <= end; ++key) {
                map.put(key, value);
            }
        }        
    }
        
} // end class

