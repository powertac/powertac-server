/*
 * Copyright (c) 2014 by John Collins
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
package org.powertac.customer;

import java.util.List;

import org.powertac.common.Tariff;

/**
 * Customer models must implement this interface.
 * @author John Collins
 */
public abstract class AbstractCustomer
{
  /**
   * Called to run the model forward one step.
   */
  public abstract void step ();

  /**
   * Called to evaluate tariffs.
   */
  public abstract void evaluateTariffs (List<Tariff> tariffs);
}
