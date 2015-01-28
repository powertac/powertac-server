/*
 * Copyright (c) 2015 by John Collins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powertac.customer.model;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import com.joptimizer.optimizers.OptimizationResponse;

/**
 * @author jcollins
 */
public class LpTest
{

  /**
   *
   */
  @Before
  public void setUp () throws Exception
  {
  }

  /**
   * Simple sanity test using the intended form.
   * 
   */
  @Test
  public void testSimpleLp ()
  {
    
  }

  /**
   * Simple tariff eval example.
   * We ignore periodic charges, because those are handled by the
   * TariffEvaluator. We'll just use a simple TOU with daytime rates
   * (8:00-19:00) of 0.15 and night rates (20:00-7:00) of 0.09.
   * Example is 24 hours, starting at the beginning of a midnight
   * shift. The numbers are from the "idle" version of the
   * futureEnergyNeeds test, with an initial charge of 20 kwh.
   *  end  trk  req  max  sur     min total
   *    8    6  256  240  -16+20    236
   *   16    8    0  240  240       236
   *    0    0  192  240   32       428
   *    8    6  256  240  -16       684
   * We use 5 chargers, 4kW/charger, no battery count constraints,
   * 0.9 charge efficiency.
   */
  @Test
  public void testLpTOU ()
  {
    Date start = new Date();
    int chargers = 5;
    double kW = 6.0;
    double eff = 0.9;
    int columns = 24;
    int shifts = 3;
    int rps = 2; // rows per shift
    double[] obj =
      {.09, .09, .09, .09, .09, .09, .09, .09,
       .15, .15, .15, .15, .15, .15, .15, .15, .15, .15, .15, .15,
       .09, .09, .09, .09};
    assertEquals("correct size", columns, obj.length);
    // constraints: 1 row/timeslot + three rows/ShiftEnergy
    int rows = shifts * rps;
    double[][] g = new double[rows][24];
    double[] h = new double[rows];
    // bounds: lb and ub per column
    double[] ub = new double[columns];
    double[] lb = new double[columns];
    // max usage/timeslot
    Arrays.fill(ub, chargers * kW / eff);
    // min usage/timeslot
    Arrays.fill(lb, 0.0);
    // min & max usage per shift
    double[] req = {240.0/eff, 0.0, 192.0/eff};
    double[] cum = {236.0/eff, 236.0/eff, 428.0/eff};
    for (int i = 0; i < shifts; i++) {
      // sum(p(t)) >= min(req, max), t in shift
      for (int j = 0; j < 8; j++) {
        g[i * rps][i * 8 + j] = -1.0;
        h[i * rps] = -req[i];
      }
      // sum(p(t)) >= total
      for (int j = 0; j < (i + 1) * 8; j++) {
        g[i * rps + 1][j] = -1.0;
        h[i * rps + 1] = -cum[i];
      }
    }
    LPOptimizationRequest or = new LPOptimizationRequest();
    or.setC(obj);
    or.setG(g);
    or.setH(h);
    or.setLb(lb);
    or.setUb(ub);
    //or.setDumpProblem(true); 

    //optimization
    LPPrimalDualMethod opt = new LPPrimalDualMethod();

    opt.setLPOptimizationRequest(or);
    try {
      int returnCode = opt.optimize();
      assertEquals("success", OptimizationResponse.SUCCESS, returnCode);
      double[] sol = opt.getOptimizationResponse().getSolution();
      Date end = new Date();
      System.out.println("Duration = " + (end.getTime() - start.getTime()));
      System.out.println("Solution = " + Arrays.toString(sol));
    }
    catch (Exception e) {
      fail(e.toString());
    }
  }
}