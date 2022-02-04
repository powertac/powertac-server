/*
 * Copyright (c) 2013 by the original author
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
package org.powertac.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.interfaces.CustomerModelAccessor;
import org.powertac.common.interfaces.ServerConfiguration;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Tariff evaluation process intended to simplify customer models.
 * There should be one of these created for each CustomerInfo
 * instance within a customer model, since
 * tariff cost values are cached, and are dependent on PowerType.
 * 
 * @author John Collins
 */
public class TariffEvaluator
{
  static private Logger log = LogManager.getLogger(TariffEvaluator.class.getName());

  // component dependencies
  TariffRepo tariffRepo;
  TariffMarket tariffMarket;
  TariffSubscriptionRepo tariffSubscriptionRepo;

  // access to customer model
  private CustomerModelAccessor accessor;
  private CustomerInfo customerInfo;

  // inconvenience factors
  private double touFactorCap = 0.0;// disabling touFactor since it's incorrect
  private double touFactor = Math.min(0.2, touFactorCap); // 0.2;
  private double tieredRateFactor = 0.1;
  private double variablePricingFactor = 0.5;
  private double interruptibilityFactor = 0.2;

  // amortization period for negative signup payments
  private long signupFeePeriod = 6 * TimeService.HOUR;

  // minimum usable tariff-expiration interval
  private long minExpirationInterval = TimeService.DAY;

  // profile cost analyzer
  private TariffEvaluationHelper helper;

  // per-customer parameter settings
  private int chunkSize = 1; // max size of allocation chunks
  private int maxChunkCount = 200; // max number of chunks
  private int tariffEvalDepth = 5; // # of tariffs/powerType to eval
  private double inertia = 0.8;
  private double signupBonusFactor = 0.1; // inertia multiplier for signup bonus
  private double rationality = 0.9;
  private double inconvenienceWeight = 0.2;
  private double tariffSwitchFactor = 0.04;
  private double preferredDuration = 6;
  private boolean evaluateAllTariffs = false;

  // state
  private int evaluationCounter = 0;
  private HashMap<Tariff, EvalData> evaluatedTariffs;
  private HashMap<Tariff, Integer> allocations;

  // algorithm parameters - needed for numerical stablity
  private double lambdaMax = 50.0;
  private double maxLinearUtility = 7.0;
  private int stdDuration = 2; // two-day standardized profile length
  private int profileLength = 168; // length of customer-supplied profile

  public TariffEvaluator (CustomerModelAccessor cma,
                          ServerConfiguration config)
  {
    accessor = cma;
    customerInfo = cma.getCustomerInfo();
    helper = new TariffEvaluationHelper();
    if (null != config) {
      config.configureMe(helper);
    }
    else {
      log.error("null config");
    }
    // TODO - remove after testing
    log.info("Reg discount coefficients {}, {}, {}, {}",
             helper.getUpregHalf(), helper.getUpregSlope(),
             helper.getDownregHalf(), helper.getDownregSlope());
    evaluatedTariffs = new HashMap<>();
    allocations = new LinkedHashMap<>();
  }

  public TariffEvaluator (CustomerModelAccessor cma)
  {
    this(cma, null);
  }

  // convenience method for logging support
  private String getName()
  {
    return customerInfo.getName();
  }

  /**
   * Delegates profile cost factors to helper.
   */
  public void initializeCostFactors (double wtExpected, double wtMax,
                                     double wtRealized, double soldThreshold)
  {
    helper.initializeCostFactors(wtExpected, wtMax, wtRealized, soldThreshold);
  }

  /**
   * Initializes the per-tariff inconvenience factors.
   * These are not normalized; it is up to the customer model to normalize the
   * per-tariff and cross-tariff factors as appropriate
   */
  public void initializeInconvenienceFactors (double touFactor,
                                              double tieredRateFactor,
                                              double variablePricingFactor,
                                              double interruptibilityFactor)
  {
    this.touFactor = Math.min(touFactor, touFactorCap); //touFactor;
    this.tieredRateFactor = tieredRateFactor;
    this.variablePricingFactor = variablePricingFactor;
    this.interruptibilityFactor = interruptibilityFactor;
  }

  /**
   * Initializes the per-timeslot regulation-capacity estimates.
   * All three represent per-timeslot estimates of exercised regulation
   * capacity, and are applicable only for tariffs with regulation rates.
   * Note that the expectedDischarge parameter only applies to
   * storage devices that can be discharged (batteries, pumped storage).
   * Values are from the customer's viewpoint, so curtailment and discharge
   * are negative (less energy for the customer) and down-regulation
   * is positive.
   * Default value for each of these factors is zero.
   */
  public void initializeRegulationFactors (double expectedCurtailment,
                                           double expectedDischarge,
                                           double expectedDownRegulation)
  {
    double expCurtail = expectedCurtailment;
    if (expCurtail > 0.0) {
      log.error(getName() + ": expectedCurtailment " + expCurtail
                + " must be non-positive");
      expCurtail = 0.0;
    }
    double expDis = expectedDischarge;
    if (expDis > 0.0) {
      log.error(getName() + ": expectedDischarge " + expDis
                + " must be non-positive");
      expDis = 0.0;
    }
    double expDown = expectedDownRegulation;
    if (expDown < 0.0) {
      log.error(getName() + ": expectedDownRegulation " + expDown
                + " must be non-negative");
      expDown = 0.0;
    }
    helper.initializeRegulationFactors(expCurtail, expDis, expDown);
  }

  // parameter settings
  /**
   * Sets the target size of allocation chunks. Default is 1. Actual
   * chunk size will be at least 0.5% of the population size.
   */
  public TariffEvaluator withChunkSize (int size)
  {
    if (size > 0)
      chunkSize = size;
    else
      log.error("chunk size " + size + " < 0");
    return this;
  }

  /**
   * Sets the number of tariffs/broker of each applicable PowerType
   * to consider. Default is 5, which means that only the 5 most recent
   * tariffs of each applicable PowerType from each broker are considered.
   * So if there are 10 brokers, and the PowerType is ELECTRIC_VEHICLE,
   * the applicable PowerTypes would be CONSUMPTION, INTERRUPTIBLE_CONSUMPTION,
   * and ELECTRIC_VEHICLE. If each broker has published at least five tariffs
   * for each of these types, the default value of 5 would result in evaluating
   * up to 150 alternative tariffs in addition to the currently subscribed
   * tariff(s).
   */
  public TariffEvaluator withTariffEvalDepth (int depth)
  {
    tariffEvalDepth = depth;
    return this;
  }

  /**
   * If true, then tariff evaluations are not saved; instead, all tariffs
   * are evaluated each time. This is needed for customers that generate
   * usage profiles that are sensitive to current conditions or state.
   */
  public TariffEvaluator withEvaluateAllTariffs (boolean value)
  {
    evaluateAllTariffs = value;
    return this;
  }

  /**
   * Sets the steady-state evaluation inertia for the customer. This is a
   * value in [0,1], where 0 is no inertia (always evaluates), and 1 is
   * total couch-potato inertia (never bothers to evaluate). The instantaneous
   * value starts at zero, so customers will have a chance to switch away
   * from the default tariff at the beginning of a sim, and is temporarily
   * set to zero when a tariff is revoked. Default value is 0.8.
   */
  public TariffEvaluator withInertia (double inertia)
  {
    this.inertia = inertia;
    return this;
  }

  /**
   * Sets the factor by which inertia is multiplied for the case where
   * the current tariff had a positive signup bonus. Should be 0 \le sbf \le 1.
   * Default value is zero.
   */
  public TariffEvaluator withSignupBonusFactor (double factor)
  {
    this.signupBonusFactor = factor;
    return this;
  }

  /**
   * Sets the level of rationality for this customer.
   * Household customers are expected to have lower rationality than
   * business/industrial/institutional customers.
   * 1.0 is fully rational, 0.5 is quite irrational. A value of zero will
   * result in random choices. Default value is 0.9.
   */
  public TariffEvaluator withRationality (double rationality)
  {
    this.rationality = rationality;
    if (rationality < 0.0) {
      log.error("Rationality " + rationality + "< 0.0");
      this.rationality = 0.01;
    }
    else if (rationality > 1.0) {
      log.error("Rationality " + rationality + "> 1.0");
      this.rationality = 1.0;
    }
    return this;
  }

  /**
   * Sets the weight given to inconvenience (as opposed to cost)
   * in computing tariff utility. Must be
   * in the range [0,1]. Default value is 0.2.
   */
  public TariffEvaluator withInconvenienceWeight (double weight)
  {
    this.inconvenienceWeight = weight;
    return this;
  }

  /**
   * Sets the inconvenience of switching tariffs. Default value is 0.04.
   */
  public TariffEvaluator withTariffSwitchFactor(double factor)
  {
    this.tariffSwitchFactor = factor;
    return this;
  }

  /**
   * Sets the preferred maximum contract duration in days. For tariffs
   * having a non-zero early-withdrawal fee, this is the period after which
   * the cost of withdrawal is discounted. It is also the standard period
   * over which usage cost is compared against signup/withdrawal payments.
   * Default value is 6 days.
   */
  public TariffEvaluator withPreferredContractDuration (double days)
  {
    this.preferredDuration  = days;
    return this;
  }

  /**
   * Sets the length of the customer-supplied profile. Used internally and
   * for test support.
   */
  void setProfileLength (int length)
  {
    profileLength = length;
  }

  int getProfileLength ()
  {
    return profileLength;
  }

  /**
   * Returns the eval scale factor, the ratio of the preferred duration
   * (as determined by the profile length) to the standard duration.
   */
  double getScaleFactor ()
  {
    return (double)getProfileLength() / ((double)stdDuration * 24.0);
  }

  /**
   * Evaluates tariffs and updates subscriptions
   * for a single customer model with a single power type.
   * Also handles tariff revoke/supersede. This requires that each
   * Customer model call this method once on each tariff publication cycle.
   */
  public void evaluateTariffs ()
  {
    allocations.clear();
    HashSet<Tariff> newTariffs = new LinkedHashSet<>(getTariffRepo()
              .findRecentActiveTariffs(tariffEvalDepth,
                                       customerInfo.getPowerType()));

    // make sure all superseding tariffs are in the set
    addSupersedingTariffs(newTariffs);

    // adjust inertia for BOG, accounting for the extra
    // evaluation cycle at ts 0
    double actualInertia =
            Math.max(0.0,
                     (1.0 - Math.pow(2, 1 - evaluationCounter)) * inertia);
    evaluationCounter += 1;

    // Get the cost eval for the appropriate default tariff
    EvalData defaultEval = getDefaultTariffEval();
    log.info("customer {}: defaultEval={}",
              getName(), defaultEval.costEstimate);
    
    // ensure we have the cost eval for each of the new tariffs
    for (Tariff tariff : newTariffs) {
      EvalData eval = evaluatedTariffs.get(tariff);
      if (evaluateAllTariffs || null == eval) {
        // compute the projected cost for this tariff
        double cost = forecastCost(tariff);
        double hassle = computeInconvenience(tariff);
        log.info("Evaluated tariff " + tariff.getId()
                 + ": cost=" + cost
                 + ", inconvenience=" + hassle);
        eval = new EvalData(cost, hassle);
        evaluatedTariffs.put(tariff, eval);
      }
    }

    // Iterate through the current active subscriptions
    for (TariffSubscription subscription
            : getTariffSubscriptionRepo().
            findActiveSubscriptionsForCustomer(customerInfo)) {
      Tariff subTariff = subscription.getTariff();
      // find out how many of these customers can withdraw without penalty
      double withdrawCost = subTariff.getEarlyWithdrawPayment();
      int committedCount = subscription.getCustomersCommitted();
      int expiredCount = subscription.getExpiredCustomerCount();
      if (withdrawCost == 0.0 ||
          (expiredCount > 0 && expiredCount == committedCount)) {
        // no need to worry about expiration
        evaluateAlternativeTariffs(subscription, actualInertia,
                                   0.0, committedCount,
                                   getDefaultTariff(),
                                   defaultEval, newTariffs);
      }
      else {
        // Evaluate expired and unexpired subsets separately
        evaluateAlternativeTariffs(subscription, actualInertia,
                                   0.0, expiredCount,
                                   getDefaultTariff(),
                                   defaultEval, newTariffs);
        evaluateAlternativeTariffs(subscription, actualInertia,
                                   withdrawCost, committedCount - expiredCount,
                                   getDefaultTariff(), 
                                   defaultEval, newTariffs);
      }
    }
    updateSubscriptions();
  }

  // Ensures that superseding tariffs are evaluated by adding them
  // to the newTariffs list
  private void addSupersedingTariffs (HashSet<Tariff> newTariffs)
  {
    List<TariffSubscription> revokedSubscriptions =
            getTariffSubscriptionRepo().getRevokedSubscriptionList(customerInfo);
    for (TariffSubscription sub : revokedSubscriptions) {
      Tariff supTariff = sub.getTariff().getIsSupersededBy();
      if (null != supTariff && supTariff.isSubscribable())
        newTariffs.add(supTariff);
    }
  }

  // evaluate alternatives
  private void evaluateAlternativeTariffs (TariffSubscription current,
                                           double inertia,
                                           double withdraw0,
                                           int population,
                                           Tariff defaultTariff,
                                           EvalData defaultEval,
                                           Set<Tariff> initialTariffs)
  {
    if (0 == population) {
      log.info("eval tariffs for zero population {}", customerInfo.getName());
      return;
    }
    //log.info("evaluateAlternativeTariffs(" + current.getTariff().getId() + ")");
    // Associate each alternate tariff with its utility value
    TreeSet<TariffUtility> evals = new TreeSet<>();
    HashSet<Tariff> tariffs = new HashSet<>(initialTariffs);
    tariffs.add(defaultTariff);

    // Check whether the current tariff is revoked, add it if not
    Tariff currentTariff = current.getTariff();
    boolean revoked = false;
    Tariff replacementTariff = null;
    if (currentTariff.getState() == Tariff.State.KILLED) {
      revoked = true;
      replacementTariff = currentTariff.getIsSupersededBy();
      log.info("Customer " + customerInfo.getName() + ": tariff "
               + currentTariff.getId() + " revoked, superseded by "
               + ((null == replacementTariff)
                       ? "default": replacementTariff.getId()));
      if (null == replacementTariff) {
        replacementTariff = defaultTariff;
      }
      withdraw0 = 0.0; // withdraw without penalty
    }
    else {
      tariffs.add(currentTariff);
    }

    // Compute the final cost number for each tariff
    HashMap<Tariff, EvalData> costs = new HashMap<Tariff, EvalData>();
    double signupCost = 0.0;
    for (Tariff tariff: tariffs) {
      EvalData eval = evaluatedTariffs.get(tariff);
      double inconvenience = eval.inconvenience;
      double cost = eval.costEstimate;
      if (tariff != currentTariff
              && tariff != replacementTariff) {
        // add in tariff-switch factors
        inconvenience += tariffSwitchFactor;
        if (tariff.getBroker() != currentTariff.getBroker()) {
          inconvenience +=
                  accessor.getBrokerSwitchFactor(revoked);
        }
        signupCost = computeSignupCost(tariff);
        double withdrawCost = computeWithdrawCost(tariff);
        cost += signupCost;
        cost += withdraw0; // withdraw from current tariff
        cost += withdrawCost;
        log.debug("current {}, alt {} signup={}, withdraw0={}, withdraw={}",
                 currentTariff.getSpecId(), tariff.getSpecId(),
                 signupCost, withdraw0, withdrawCost);
        if (Double.isNaN(cost)) {
          log.error(getName() + ": cost is NaN for tariff "
                    + tariff.getId());
        }
      }
      costs.put(tariff, new EvalData(cost, inconvenience));
    }

    // for each tariff, including the current and default tariffs,
    // compute the utility
    for (Tariff tariff: tariffs) {
      EvalData finalCost = costs.get(tariff);
      // don't consider current tariff if it's revoked
      if (!revoked || tariff != currentTariff) {
        double utility =
            computeNormalizedDifference(finalCost.costEstimate,
                                        defaultEval.costEstimate);
        utility -= inconvenienceWeight * finalCost.inconvenience;
        //log.info("utility for tariff {} = {}, constrained to {}", tariff.getId(),
        //         utility, constrainUtility(utility));
        if (Double.isNaN(utility)) {
          log.error(getName() + ": utility is NaN for tariff "
                    + tariff.getId());
        }
        log.debug("tariff {}: adjCost={}, default={}, utility={}",
                  tariff.getId(), finalCost.costEstimate,
                  defaultEval.costEstimate, utility);
        TariffUtility tu =
            new TariffUtility(tariff, constrainUtility(utility));
        evals.add(tu);
      }
    }
    
    // We now have utility values for each possible tariff.
    // Time to make some choices -
    // -- first, compute lambda from rationality
    // -- second, we have to compute the sum of transformed utilities
    double logitDenominator = 0.0;
    double lambda = Math.pow(lambdaMax, rationality) - 1.0;
    for (TariffUtility util : evals) {
      logitDenominator +=
              Math.exp(lambda * util.utility);
    }
    // then we can compute the probabilities
    for (TariffUtility util : evals) {
      util.probability =
              Math.exp(lambda * util.utility)
              / logitDenominator;
      if (Double.isNaN(util.probability)) {
        log.error(getName() + ": Probability NAN, util=" + util.utility
                  + ", denom=" + logitDenominator
                  + ", tariff " + util.tariff);
        util.probability = 0.0;
      }
      log.debug("Tariff {} probability={}",
                util.tariff.getId(), util.probability);
    }
    int remainingPopulation = population;
    int chunk = remainingPopulation;
    if (customerInfo.isMultiContracting()) {
      // Ideally, each individual customer makes a choice.
      // For large populations, we do it in chunks.
      chunk = getChunkSize(population);
    }
    while (remainingPopulation > 0) {
      int count = (int)Math.min(remainingPopulation, chunk);
      remainingPopulation -= count;
      // allocate a chunk
      double inertiaSample = accessor.getInertiaSample();
      if (!revoked && withdraw0 <= 0.0 &&
          signupCost <= 0.0 && inertiaSample < inertia) {
        // skip this one if not processing revoked tariff,
        // or if there is no payment possible from withdrawing,
        // or if the customer was not induced by a positive signup cost,
        // or if the customer is not paying attention.
        continue;
      }
      else if (signupCost > 0.0 &&
          inertiaSample < inertia * signupBonusFactor) {
        // Use lower inertia in case the current tariff had a signup bonus
        continue;
      }
      double tariffSample = accessor.getTariffChoiceSample();
      // walk down the list until we run out of probability
      boolean allocated = false;
      for (TariffUtility tu : evals) {
        log.debug("tariff {}: sample={}, probability={}",
                  tu.tariff.getId(), tariffSample, tu.probability);
        if (tariffSample <= tu.probability) {
          addAllocation(currentTariff, tu.tariff, count);
          allocated = true;
          break;
        }
        else {
          tariffSample -= tu.probability;
        }
      }
      if (!allocated) {
        log.error(getName() + ": Failed to allocate: P=" + tariffSample);
      }
    }
  }

  // Customers really, really don't like paying to sign up. This computation
  // inflates the cost of signup fees by the ratio of the customer's
  // preferred duration to the duration of one tariff-publication cycle.
  // On the other hand, positive signup payments are scaled to amortize over
  // just the standard eval duration.
  double computeSignupCost (Tariff tariff)
  {
    if (tariff.getSignupPayment() < 0.0) {
      // penalize negative signup fees
      return tariff.getSignupPayment() *
          preferredDuration * TimeService.DAY / signupFeePeriod;
    }
    else {
      return tariff.getSignupPayment() * getScaleFactor();
    }
  }

  // If the tariff has a non-zero minDuration and a negative
  // earlyWithdrawPayment, then we prefer shorter values for minDuration.
  double computeWithdrawCost (Tariff tariff)
  {
    if (tariff.getMinDuration() <= minExpirationInterval
        || 0.0 == tariff.getEarlyWithdrawPayment()) {
      return 0.0;
    }
    double annoyance = 1.0;
    if (tariff.getEarlyWithdrawPayment() < 0.0) {
      annoyance =
          (double)tariff.getMinDuration()
          / (double)(preferredDuration * TimeService.DAY);
    }
    double scale = annoyance * getScaleFactor();
    return tariff.getEarlyWithdrawPayment() * scale;
  }

  // Ensures numeric stability by constraining range of utility values.
  private double constrainUtility (double utility)
  {
    if (utility > maxLinearUtility) {
      double compressed = Math.log10(utility - maxLinearUtility);
      return Math.min(maxLinearUtility + compressed,
                      maxLinearUtility * 2);
    }
    else if (utility < -maxLinearUtility) {
      return -maxLinearUtility; // these will never be chosen anyway
    }
    else
      return utility;
  }

  // Computes the normalized difference between the cost of the default tariff
  // and the cost of a proposed tariff
  private double computeNormalizedDifference (double cost, double defaultCost)
  {
    // Daniel temporary bug fix
    // John -- TODO -- I REALLY don't like this
    if (defaultCost == 0) {
      // this means that capacity is 0, so we don't want any changes
      // so return small utility
      log.warn("default cost is zero");
      return 0;
    }
    double ndiff = (cost - defaultCost) / Math.abs(defaultCost);
    return ndiff;
  }

  // Retrieves default tariff
  private Tariff getDefaultTariff ()
  {
    return getTariffMarket().getDefaultTariff(customerInfo.getPowerType());
  }

  private EvalData getDefaultTariffEval ()
  {
    Tariff defaultTariff = getDefaultTariff();
    EvalData defaultEval = evaluatedTariffs.get(defaultTariff);
    if (null == defaultEval) {
      defaultEval = new EvalData(forecastCost(defaultTariff), 0.0);
      evaluatedTariffs.put(defaultTariff, defaultEval);
    }
    return defaultEval;
  }
  
  // Cost forecaster
  private double forecastCost (Tariff tariff)
  {
    CapacityProfile profile = accessor.getCapacityProfile(tariff);
    if (0 == profile.getProfile().length) {
      log.error("Zero-length profile for " + customerInfo.getName());
      return 0.0;
    }
    setProfileLength(profile.getProfile().length);
    // NOTE: must call the next function after the previous,
    // since the previous writes inconv. factors.
    // Always 0 except for AdaptiveCapacityOriginator
    double inconv = accessor.getShiftingInconvenienceFactor(tariff);
    double profileCost = helper.estimateCost(tariff,
                                             profile.getProfile(),
                                             profile.getStart());
    if (Double.isNaN(profileCost)) {
      log.error(getName() + ": profile cost NaN for tariff "
                + tariff.getId());
    }
    //double scale = preferredDuration * 24.0 / profile.length;
    double scale = stdDuration * 24.0 / getProfileLength();
    if (Double.isNaN(scale)) {
      log.error(getName() + ": scale NaN for tariff " + tariff.getId());
    }
    log.debug("tariff {}: profileCost={}, inconv={}, scaled-charge={}, scaled (cost+inconv)={}",
              tariff.getId(), profileCost, inconv, profileCost * scale, (profileCost + inconv) * scale,
              (profileCost + inconv) * scale / (profileCost * scale));
    return (profileCost + inconv) * scale;
  }

  // Tracks additions and deletions for tariff subscriptions
  // This is where we have the old/new pairs for customer notification 
  private void addAllocation (Tariff current, Tariff newTariff, int count)
  {
    if (current == newTariff)
      // ignore no-change allocations
      return;
    // decrement the old
    Integer ac = allocations.get(current);
    if (null == ac)
      ac = -count;
    else
      ac -= count;
    allocations.put(current, ac);
    // increment the new
    ac = allocations.get(newTariff);
    if (null == ac)
      // first time on this one
      ac = count;
    else
      ac += count;
    allocations.put(newTariff, ac);
    
    // Notify customer of the transfer
    accessor.notifyCustomer(tariffSubscriptionRepo.getSubscription(customerInfo, current),
                            tariffSubscriptionRepo.getSubscription(customerInfo, newTariff),
                            count);
  }
  
  // updates subscriptions based on computed allocations
  private void updateSubscriptions ()
  {
    int check = 0;
    for (Tariff tariff : allocations.keySet()) {
      int count = allocations.get(tariff);
      check += count;
      if (count < 0) {
        //unsubscribe
        TariffSubscription sub =
                getTariffSubscriptionRepo().findSubscriptionForTariffAndCustomer
                  (tariff, customerInfo);
        sub.unsubscribe(-count);
        log.info("customer " + customerInfo.getName()
                 + " unsubscribes " + -count
                 + " from tariff " + tariff.getId());
      }
      else if (count > 0) {
        // subscribe
        getTariffMarket().subscribeToTariff(tariff, customerInfo, count);
        log.info("customer " + customerInfo.getName()
                 + " subscribes " + count
                 + " to tariff " + tariff.getId());
      }
    }
    // sanity check
    if (check != 0) {
      log.error("Subscription updates do not add up for "
                + customerInfo.getName() + ": " + check);
    }
  }

  // inconvenience computation
  /**
   * Returns inconvenience of time-of-use rate.
   */
  public double getTouFactor ()
  {
    return touFactor;
  }

  /**
   * Returns inconvenience of tiered rate.
   */
  public double getTieredRateFactor ()
  {
    return tieredRateFactor;
  }

  /**
   * Returns inconvenience of variable pricing.
   */
  public double getVariablePricingFactor ()
  {
    return variablePricingFactor;
  }

  /**
   * Returns inconvenience of interruptibility.
   */
  public double getInterruptibilityFactor ()
  {
    return interruptibilityFactor;
  }

  /**
   * Computes composite per-tariff inconvenience of a tariff.
   */
  public double computeInconvenience (Tariff tariff)
  {
    double result = 0.0;
    // Time-of-use tariffs have multiple Rates, at least one of which
    // has a daily or weekly begin/end
    if (tariff.isTimeOfUse())
      result += touFactor;

    // Tiered tariffs have multiple Rates, at least one having
    // a non-zero tier threshold.
    if (tariff.isTiered())
      result += tieredRateFactor;

    // Variable-rate tariffs have at least one non-fixed Rate
    if (tariff.isVariableRate())
      result += variablePricingFactor;

    // Interruptible tariffs are for an interruptible PowerType, and
    // have a Rate with a maxCurtailment != 0
    if (tariff.isInterruptible())
      result += interruptibilityFactor;
    return result;
  }
  
  // returns the correct chunk size for a given population
  private int getChunkSize (int population)
  {
    if (population <= chunkSize)
      return population;
    else
      return Math.max(population / maxChunkCount, chunkSize);
  }

  // Spring component access ------------------------------------------
  private TariffRepo getTariffRepo ()
  {
    if (null != tariffRepo)
      return tariffRepo;
    tariffRepo =
            (TariffRepo) SpringApplicationContext.getBean("tariffRepo");
    return tariffRepo;
  }

  private TariffSubscriptionRepo getTariffSubscriptionRepo ()
  {
    if (null != tariffSubscriptionRepo)
      return tariffSubscriptionRepo;
    tariffSubscriptionRepo =
            (TariffSubscriptionRepo) SpringApplicationContext.getBean("tariffSubscriptionRepo");
    return tariffSubscriptionRepo;
  }

  private TariffMarket getTariffMarket ()
  {
    if (null != tariffMarket)
      return tariffMarket;
    tariffMarket =
            (TariffMarket) SpringApplicationContext.getBean("tariffMarketService");
    return tariffMarket;
  }

  // Container for tariff-utility recording ------------------------------
  class TariffUtility implements Comparable<TariffUtility>
  {
    Tariff tariff;
    double adjustedCost = 0.0;
    double utility;
    double probability = 0.0;

    TariffUtility (Tariff tariff, double utility)
    {
      super();
      this.tariff = tariff;
      this.utility = utility;
    }

    @Override
    // natural ordering is by decreasing utility values
      public
      int compareTo (TariffUtility other)
    {
      double result = other.utility - utility;
      if (result == 0.0)
        // consistent with equals...
        return (int) (other.tariff.getId() - tariff.getId());
      else if (result > 0.0)
        return 1;
      else
        return -1;
    }
  }

  // Container for tariff-evaluation data
  class EvalData
  {
    double costEstimate;
    double inconvenience;

    EvalData (double cost, double inconvenience)
    {
      super();
      this.costEstimate = cost;
      this.inconvenience = inconvenience;
    }
  }
}
