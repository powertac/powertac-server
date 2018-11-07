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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.joptimizer.optimizers.LPOptimizationRequest;
import com.joptimizer.optimizers.LPPrimalDualMethod;
import com.joptimizer.optimizers.OptimizationResponse;

/**
 * @author jcollins
 */
public class LpTest
{

  @BeforeEach
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
   * We use 5 chargers, 6kW/charger, no battery count constraints,
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
    int slackColumns = shifts * rps;
    double[] obj =
      {.09, .09, .09, .09, .09, .09, .09, .09,
       .15, .15, .15, .15, .15, .15, .15, .15, .15, .15, .15, .15,
       .09, .09, .09, .09,
       0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    assertEquals(columns + slackColumns, obj.length, "correct size");
    // constraints: 1 row/timeslot + three rows/ShiftEnergy
    int rows = shifts * rps;
    double[][] a = new double[rows][columns + slackColumns];
    double[] b = new double[rows];
    // bounds: lb and ub per column
    double[] ub = new double[columns + slackColumns]; // should be max(chargers, batteries)
    double[] lb = new double[columns + slackColumns];
    // max usage/timeslot
    Arrays.fill(ub, chargers * kW / eff);
    // min usage/timeslot
    Arrays.fill(lb, 0.0);
    // min & max usage per shift
    double[] req = {240.0/eff, 0.0, 192.0/eff};
    double[] cum = {236.0/eff, 236.0/eff, 428.0/eff};
    for (int i = 0; i < shifts; i++) {
      // sum(p(t)) + slackI1 = min(req, max), t in shift
      for (int j = 0; j < 8; j++) {
        a[i * rps][i * 8 + j] = -1.0;
        a[i * rps][columns + i * rps] = -1.0;
        b[i * rps] = -req[i];
      }
      // -sum(p(t)) + slackI2 = -total
      for (int j = 0; j < (i + 1) * 8; j++) {
        a[i * rps + 1][j] = -1.0;
        a[i * rps + 1][columns + i * rps + 1] = -1.0;
        b[i * rps + 1] = -cum[i];
      }
    }
    LPOptimizationRequest or = new LPOptimizationRequest();
    or.setC(obj);
    or.setA(a);
    or.setB(b);
    or.setLb(lb);
    or.setUb(ub);
    or.setDumpProblem(true); 

    //optimization
    LPPrimalDualMethod opt = new LPPrimalDualMethod();

    opt.setLPOptimizationRequest(or);
    try {
      int returnCode = opt.optimize();
      assertEquals(OptimizationResponse.SUCCESS, returnCode, "success");
      double[] sol = opt.getOptimizationResponse().getSolution();
      Date end = new Date();
      System.out.println("Duration = " + (end.getTime() - start.getTime()));
      System.out.println("Solution = " + Arrays.toString(sol));
    }
    catch (Exception e) {
      fail(e.toString());
    }
  }

  /**
   * Simple tariff eval example, constraints on running total only.
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
   * We use 5 chargers, 6kW/charger, no battery count constraints,
   * 0.9 charge efficiency.
   */
  @Test
  public void testLpTOU_Total ()
  {
    Date start = new Date();
    int chargers = 5;
    double kW = 6.0;
    double eff = 0.9;
    int columns = 24;
    int shifts = 3;
    int rps = 1; // rows per shift
    int slackColumns = shifts * rps;
    double[] obj =
      {.09, .09, .09, .09, .09, .09, .09, .09,
       .15, .15, .15, .15, .15, .15, .15, .15, .15, .15, .15, .15,
       .09, .09, .09, .09,
       0.0, 0.0, 0.0};
    assertEquals(columns + slackColumns, obj.length, "correct size");
    // constraints: 1 row/timeslot + one row/ShiftEnergy
    int rows = shifts * rps;
    double[][] a = new double[rows][columns + slackColumns];
    double[] b = new double[rows];
    // bounds: lb and ub per column
    double[] ub = new double[columns + slackColumns]; // should be max(chargers, batteries)
    double[] lb = new double[columns + slackColumns];
    // max usage/timeslot
    Arrays.fill(ub, chargers * kW / eff);
    // min usage/timeslot
    Arrays.fill(lb, 0.0);
    // min & max usage per shift
    //double[] req = {240.0/eff, 0.0, 192.0/eff};
    double[] cum = {236.0/eff, 236.0/eff, 428.0/eff};
    for (int i = 0; i < shifts; i++) {
      // sum(p(t)) + slackI1 = min(req, max), t in shift
//      for (int j = 0; j < 8; j++) {
//        a[i * rps][i * 8 + j] = -1.0;
//        a[i * rps][columns + i * rps] = 1.0;
//        b[i * rps] = -req[i];
//      }
      // -(sum(p(t)) + slackI2) = -total
      for (int j = 0; j < (i + 1) * 8; j++) {
        a[i * rps][j] = -1.0;
      }
      a[i * rps][columns + i * rps] = -1.0;
      b[i * rps] = -cum[i];
    }
    LPOptimizationRequest or = new LPOptimizationRequest();
    or.setC(obj);
    or.setA(a);
    or.setB(b);
    or.setLb(lb);
    or.setUb(ub);
    or.setDumpProblem(true); 

    //optimization
    LPPrimalDualMethod opt = new LPPrimalDualMethod();

    opt.setLPOptimizationRequest(or);
    try {
      int returnCode = opt.optimize();
      assertEquals(OptimizationResponse.SUCCESS, returnCode, "success");
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