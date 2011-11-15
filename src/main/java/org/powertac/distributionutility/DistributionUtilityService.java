/*
 * Copyright 2009-2011 the original author or authors.
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

package org.powertac.distributionutility;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.BigMatrix;
import org.ojalgo.optimisation.quadratic.QuadraticSolver;
import org.ojalgo.type.StandardType;
import org.powertac.common.interfaces.Accounting;
import org.powertac.common.Broker;
import org.powertac.common.Orderbook;
import org.powertac.common.PluginConfig;
import org.powertac.common.RandomSeed;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.CompetitionControl;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.common.repo.OrderbookRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DistributionUtilityService extends TimeslotPhaseProcessor
{
  public class ChargeInfo
  {
    public String itsBrokerName = "";
    public double itsNetLoadKWh = 0.0;
    public double itsBalanceCharge = 0.0;

    public ChargeInfo (String inBrokerName, double inNetLoadKWh,
                       double inBalanceCharge)
    {
      itsBrokerName = inBrokerName;
      itsNetLoadKWh = inNetLoadKWh;
      itsBalanceCharge = inBalanceCharge;
    }
  }

  Logger log = Logger.getLogger(this.getClass().getName());

  @Autowired
  private BrokerRepo brokerRepo;

  @Autowired
  private TimeslotRepo timeslotRepo;

  @Autowired
  private OrderbookRepo orderbookRepo;

  @Autowired
  private Accounting accountingService;

  @Autowired
  private CompetitionControl competitionControlService;

  @Autowired
  private RandomSeedRepo randomSeedService;
  private RandomSeed randomGen;

  long id = 0;
  // fees and prices should be negative, because they are debits against brokers
  private double distributionFee = -0.01;
  private double balancingCost = -0.06;
  private double defaultSpotPrice = -30.0; // per mwh

  /**
   * Computes actual distribution and balancing costs by random selection
   */
  public void init(PluginConfig config)
  {
    double distributionFeeMin =
        config.getDoubleValue("distributionFeeMin", -0.005);
    double distributionFeeMax = 
        config.getDoubleValue("distributionFeeMax", -0.15);
    double balancingCostMin = 
        config.getDoubleValue("balancingCostMin", -0.04);
    double balancingCostMax =
        config.getDoubleValue("balancingCostMax", -0.08);

    randomGen = randomSeedService.getRandomSeed("DistributionUtilityService",
                                                id, "model");
    distributionFee = (distributionFeeMin + randomGen.nextDouble()
        * (distributionFeeMax - distributionFeeMin));
    balancingCost = (balancingCostMin + randomGen.nextDouble()
        * (balancingCostMax - balancingCostMin));
    super.init();
    log.info("Configured DU: distro fee = " + distributionFee
             + ", balancing cost = " + balancingCost);
  }

  public void activate (Instant time, int phaseNumber)
  {
    log.info("Activate");
    List<Broker> brokerList = brokerRepo.findRetailBrokers();
    if (brokerList == null) {
      log.error("Failed to retrieve retail broker list");
      return;
    }

    // Run the balancing market
    // Transactions are posted to the Accounting Service and Brokers are
    // notified of balancing transactions
    balanceTimeslot(timeslotRepo.currentTimeslot(), brokerList);
  }

  /**
   * Returns the difference between a broker's current market position and its
   * net load. Note: market position is computed in MWh and net load is computed
   * in kWh, conversion is needed to compute the difference.
   * 
   * @return a broker's current energy balance within its market. Pos for
   *         over-production, neg for under-production
   */
  public double getMarketBalance (Broker broker)
  {
    return accountingService.getCurrentMarketPosition(broker) * 1000.0
           + accountingService.getCurrentNetLoad(broker);
  }

  /**
   * Returns the spot market price - the clearing price for the current timeslot
   * in the most recent trading period.
   */
  public double getSpotPrice ()
  {
    Double result = defaultSpotPrice;
    // most recent trade is determined by Competition parameters
    // TODO - not sure if still needed
    // Competition comp = Competition.currentCompetition();
    // int offset = comp.getDeactivateTimeslotsAhead();
    // Instant executed = new Instant(timeService.getCurrentTime().getMillis()
    // - offset * comp.getTimeslotLength() * TimeService.MINUTE);
    // orderbooks have timeslot and execution time
    Orderbook ob =
        orderbookRepo.findSpotByTimeslot(timeslotRepo.currentTimeslot());
    if (ob != null) {
      result = -ob.getClearingPrice();
    }
    else {
      log.info("null Orderbook");
    }
    return result / 1000.0; // convert to kwh
  }

  /**
   * Generates a list of Transactions that balance the overall market.
   * Transactions are generated on a per-broker basis depending on the broker's
   * balance within its own market.
   * 
   * @return List of MarketTransactions
   */
  public List<ChargeInfo> balanceTimeslot (Timeslot currentTimeslot,
                                           List<Broker> brokerList)
  {
    List<Double> brokerBalances = new ArrayList<Double>();
    for (Broker broker : brokerList) {
      brokerBalances.add(getMarketBalance(broker));
    }
    List<Double> balanceCharges = computeNonControllableBalancingCharges(brokerList,
                                                                         brokerBalances);

    List<ChargeInfo> chargeInfoList = new ArrayList<ChargeInfo>();
    // Add transactions for distribution and balancing
    double theNetLoad, theBalanceCharge;
    Broker theBroker;
    for (int i = 0; i < brokerList.size(); i++) {
      theBroker = brokerList.get(i);
      theNetLoad = -accountingService.getCurrentNetLoad(theBroker);
      accountingService.addDistributionTransaction(theBroker, theNetLoad,
                                                   theNetLoad * distributionFee);

      theBalanceCharge = -balanceCharges.get(i);
      chargeInfoList.add(new ChargeInfo(theBroker.getUsername(), theNetLoad,
                                        theBalanceCharge));
      if (theBalanceCharge != 0.0) {
        accountingService.addBalancingTransaction(theBroker,
                                                  brokerBalances.get(i),
                                                  theBalanceCharge);
      }
    }
    return chargeInfoList;
  }

  List<Double> computeNonControllableBalancingCharges (List<Broker> brokerList,
                                                       List<Double> balanceList)
  {
    QuadraticSolver myQuadraticSolver;
    BasicMatrix[] inputMatrices = new BigMatrix[6];
    int numOfBrokers = brokerList.size();
    double P = -getSpotPrice(); // market price in day ahead market
    double c0 = -balancingCost; // cost function per unit of energy produced by
                               // the DU
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
      x += brokerBalance[i] = ((Double) balanceList.get(i)).doubleValue();
    }

    // Initialize all the matrices with the proper values
    for (int i = 0; i < numOfBrokers; i++) {
      AE[0][i] = 0;
      C[i][0] = brokerBalance[i] * P;
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
      BI[i][0] = brokerBalance[i] * P;
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
    final org.ojalgo.optimisation.quadratic.QuadraticSolver.Builder tmpBuilder = new QuadraticSolver.Builder(
                                                                                                             inputMatrices[2].round(StandardType.DECIMAL_032)
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
    List<Double> solutionList = new ArrayList<Double>();
    for (int i = 0; i < numOfBrokers; i++) {
      solutionList.add((Double) result.doubleValue(i, 0));
    }
    return solutionList;
  }

}
