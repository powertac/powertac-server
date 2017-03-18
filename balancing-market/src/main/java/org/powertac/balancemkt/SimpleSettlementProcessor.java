/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.balancemkt;

import java.util.List;

//import org.apache.log4j.Logger;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.BigMatrix;
import org.ojalgo.optimisation.quadratic.QuadraticSolver;
import org.ojalgo.type.StandardType;
import org.powertac.common.interfaces.CapacityControl;
import org.powertac.common.repo.TariffRepo;

/**
 * DU settlement processor for Scenario 1 - no controllable capacities.
 * @author John Collins, Travis Daudelin
 */
public class SimpleSettlementProcessor extends SettlementProcessor
{
  public SimpleSettlementProcessor (TariffRepo tariffRepo, CapacityControl capacityControl)
  {
    super(tariffRepo, capacityControl);
  }
  
  /* (non-Javadoc)
   * @see org.powertac.balancemkt.SettlementProcessor#settle(java.util.Collection)
   */
  @SuppressWarnings("deprecation")
  @Override
  public void settle (SettlementContext service,
                      List<ChargeInfo> brokerData)
  {
    QuadraticSolver myQuadraticSolver;
    BasicMatrix[] inputMatrices = new BigMatrix[6];
    int numOfBrokers = brokerData.size();
    
    double pMax = service.getPPlus();
    double pMin = -service.getPMinus();
    log.debug("pMax=" + pMax + ", pMin=" + pMin);
    
    double c0 = -service.getBalancingCost(); // cost per kwh for energy sourced by DU
    double x = 0.0; // total market balance
    double[] brokerBalance = new double[numOfBrokers];

    double[][] AE = new double[1][numOfBrokers]; // equality constraints lhs
    double[][] BE = new double[1][1]; // equality constraints rhs
    double[][] Q = new double[numOfBrokers][numOfBrokers]; // quadratic
                                                           // objective
    double[][] C = new double[numOfBrokers][1]; // linear objective
    double[][] AI = new double[numOfBrokers + 1][numOfBrokers]; // inequality
                                                                // constraints
                                                                // lhs
    double[][] BI = new double[numOfBrokers + 1][1]; // inequality constraints
                                                     // rhs

    for (int i = 0; i < numOfBrokers; i++) {
      x += (brokerBalance[i] = brokerData.get(i).getNetLoadKWh());
      log.debug("broker[" + i + "].balance=" + brokerBalance[i]);
    }

    // Initialize all the matrices with the proper values
    for (int i = 0; i < numOfBrokers; i++) {
      AE[0][i] = 0;
      if (brokerBalance[i] < 0.0) {
        BI[i][0] = brokerBalance[i] * pMax;
        C[i][0] = brokerBalance[i] * pMax;
      }
      else {
        BI[i][0] = brokerBalance[i] * pMin;
        C[i][0] = brokerBalance[i] * pMin;
      }
      for (int j = 0; j < numOfBrokers; j++) {
        if (i == j) {
          Q[i][j] = 1;
          AI[i][j] = -1;
        }
        else {
          Q[i][j] = 0;
          AI[i][j] = 0;
        }
      }
      AI[numOfBrokers][i] = -1;
    }
    BE[0][0] = 0;
    BI[numOfBrokers][0] = x * c0;

    // format the above data for the solver
    inputMatrices[0] = BigMatrix.FACTORY.copy(AE);
    inputMatrices[1] = BigMatrix.FACTORY.copy(BE);
    inputMatrices[2] = BigMatrix.FACTORY.copy(Q);
    inputMatrices[3] = BigMatrix.FACTORY.copy(C);
    inputMatrices[4] = BigMatrix.FACTORY.copy(AI);
    inputMatrices[5] = BigMatrix.FACTORY.copy(BI);

    // create a new builder to initialize the solver with Q and C
    final org.ojalgo.optimisation.quadratic.QuadraticSolver.Builder tmpBuilder =
            new QuadraticSolver.Builder(inputMatrices[2].round(StandardType.DECIMAL_032)
                                                        .toPrimitiveStore(),
                                        inputMatrices[3].round(StandardType.DECIMAL_032)
                                                        .negate()
                                                        .toPrimitiveStore());
    // input the equality constraints
    tmpBuilder.equalities(inputMatrices[0].round(StandardType.DECIMAL_032)
                                          .toPrimitiveStore(),
                          inputMatrices[1].round(StandardType.DECIMAL_032)
                                          .toPrimitiveStore());
    // input the inequality constraints
    tmpBuilder.inequalities(inputMatrices[4].round(StandardType.DECIMAL_032)
                                            .toPrimitiveStore(),
                            inputMatrices[5].round(StandardType.DECIMAL_032)
                                            .toPrimitiveStore());
    // configure the solver
    myQuadraticSolver = tmpBuilder.build();

    // solve the system, and return the result as a list of balancing
    // charges
    BasicMatrix result = myQuadraticSolver.solve().getSolution();
    //List<Double> solutionList = new ArrayList<Double>();
    for (int i = 0; i < numOfBrokers; i++) {
      brokerData.get(i).setBalanceChargeP1(-result.doubleValue(i, 0));
    }
    //log.debug("result=" + solutionList);
    //return solutionList;

  }

}
