/*
 * Copyright 2009-2010 the original author or authors.
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

package org.powertac.common.interfaces;

import org.powertac.common.Timeslot;

import java.util.List;

/**
 * Interface describing common methods for a Distribution Utility module
 * in PowerTAC.
 *
 * @author Carsten Block
 * @version 0.1 - January 2nd, 2011
 */
public interface DistributionUtility {

  /**
   * This method is invoked in order to make the distribution utility detect and
   * balance power differences for the given timeslot. As a result the distribution
   * utility produces a list of {@link org.powertac.common.msg.PositionDoUpdateCmd} and
   * {@link org.powertac.common.msg.CashDoUpdateCmd} objects that describe the bookings
   * required to balance overall energy consumption and overall energy production for the
   * given timeslot as well as the resulting payments for those brokers who cause potential
   * imbalances.
   *
   * @param currentTimeslot the timeslot to compute the balancing for
   * @return a list of {@link org.powertac.common.msg.PositionDoUpdateCmd} and {@link org.powertac.common.msg.CashDoUpdateCmd} objects
   */
  List balanceTimeslot(Timeslot currentTimeslot);

}
