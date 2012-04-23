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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import org.powertac.common.state.Domain;

/**
 * Encapsulation represented real or hypothetical capacity over some 
 * fixed number of timeslots.  We will set the number of timeslots to 24.
 * 
 * @author Prashant Reddy
 */
@Domain
public final class CapacityProfile
{
    enum PermutationRule { TEMPORAL_SHIFTS, BALANCING_SHIFTS, ALL_SHIFTS }
    
    static final int NUM_TIMESLOTS = 24;
    
    private static final int MAX_BALANCING_SHIFTS = 24;
    private static final double BALANCING_SHIFTS_EPSILON = 0.1;  // range as percent of max
    
    private List<Double> values;
    
    
    CapacityProfile(Double uniformValue)
    {
        values = new ArrayList<Double>(NUM_TIMESLOTS);
        for (int i=0; i < NUM_TIMESLOTS; ++i) {
            values.add(uniformValue);
        }
    }
    
    CapacityProfile(List<Double> list)
    {
        values = new ArrayList<Double>(NUM_TIMESLOTS);
        values.addAll(list);
    }
    
    CapacityProfile(double[] array)
    {
        if (array.length != NUM_TIMESLOTS) {
            throw new Error("Number of elements in array does not match expected length: " + NUM_TIMESLOTS);
        }
        values = new ArrayList<Double>(NUM_TIMESLOTS);
        for (int i=0; i < NUM_TIMESLOTS; ++i) {
            values.add(array[i]);
        }
    }
    
    double distanceTo(CapacityProfile other) 
    {
        // sum of squared distances
        double distance = 0.0;
        for (int i=0; i < NUM_TIMESLOTS; ++i) {
            distance += Math.pow(Math.abs(this.getCapacity(i) - other.getCapacity(i)), 2);
        }
        return distance;
    }
    
    double getCapacity(int index) 
    {
        return values.get(index);
    }
    
    List<CapacityProfile> getPermutations(PermutationRule rule)
    {
        List<CapacityProfile> perms;
        switch (rule) {
        case TEMPORAL_SHIFTS:
            perms = getTimeShiftedPermutations();
            break;
        case BALANCING_SHIFTS:
            perms = getPeakShiftedPermutations();
            break;
        case ALL_SHIFTS:
            perms = new ArrayList<CapacityProfile>();
            perms.addAll(getTimeShiftedPermutations());
            perms.addAll(getPeakShiftedPermutations());
            break;
        default:
            throw new Error("Unexpected value for permutation rule: " + rule);
        }
        return perms;
    }
    
    private List<CapacityProfile> getTimeShiftedPermutations()
    {
        List<CapacityProfile> perms = new ArrayList<CapacityProfile>();
        for (int i=0; i < NUM_TIMESLOTS; ++i) {
            List<Double> perm = new ArrayList<Double>(NUM_TIMESLOTS);
            for (int j=i; j < i + NUM_TIMESLOTS; ++j) {
                perm.add(values.get(j % NUM_TIMESLOTS));
            }
            validatePermutation(perm);  // TODO TEMP
            perms.add(new CapacityProfile(perm));
        }
        return perms;
    }
    
    private List<CapacityProfile> getPeakShiftedPermutations()
    {
        List<CapacityProfile> perms = new ArrayList<CapacityProfile>();      
        recursivePeakShift(values, perms);
        return perms;
    }
    
    private void recursivePeakShift(List<Double> curr, List<CapacityProfile> perms)
    {
        int peakIndex = 0;
        int valleyIndex = 0;
        for (int i=0; i < NUM_TIMESLOTS; ++i) {
            Double val = curr.get(i);
            if (val > curr.get(peakIndex)) peakIndex = i;
            if (val < curr.get(valleyIndex)) valleyIndex = i;            
        }
        Double max = curr.get(peakIndex);
        Double min = curr.get(valleyIndex);
        Double mid = 0.5 * (max + min);
        if (peakIndex != valleyIndex) {
            List<Double> newList = new ArrayList<Double>();
            for (int j=0; j < NUM_TIMESLOTS; ++j) {
                if (j == peakIndex) {
                    newList.add(mid); // shift down (max - mid)
                } 
                else if (j == valleyIndex) {
                    newList.add(min + (max - mid)); // shift up (max - mid)
                }
                else {
                    newList.add(curr.get(j));
                }
            }
            validatePermutation(newList);  // TODO TEMP
            CapacityProfile newProfile = new CapacityProfile(newList);
            perms.add(newProfile);
            if (perms.size() < MAX_BALANCING_SHIFTS) {
                double newRange = Collections.max(newList) - Collections.min(newList);
                if (newRange > (BALANCING_SHIFTS_EPSILON * max)) {
                    recursivePeakShift(newList, perms);
                }
            }
        }
    }
    
    private void validatePermutation(List<Double> perm)
    {
        Double origTotal = 0.0;
        Double permTotal = 0.0;
        for (int i=0; i < NUM_TIMESLOTS; ++i) {
            origTotal += values.get(i);
            permTotal += perm.get(i);
        }
        if (Math.abs(permTotal - origTotal) > 0.01) {
            throw new Error("Total permutation capacity " + permTotal + " not approximately equal to original capacity " + origTotal);
        }
    }
    
    @Override
    public String toString()
    {
        return this.getClass().getCanonicalName() + ":" + values.toString();
    }
    
} // end class


