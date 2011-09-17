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
package org.powertac.common.interfaces;

import org.powertac.common.TariffMessage;
import org.powertac.common.TariffSpecification;
import org.powertac.common.msg.TariffExpire;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TariffStatus;
import org.powertac.common.msg.VariableRateUpdate;

/**
 * Interface for a service that processed tariff-related messages.
 * Needed to implement visitor pattern.
 * @author John Collins
 */
public interface TariffMessageProcessor
{
  /**
   * Processes a tariff message. To correctly support double-dispatch,
   * we provide a method signature for each subclass of TariffMessage.
   */
  public TariffStatus processTariff (TariffSpecification message);
  public TariffStatus processTariff (TariffExpire message);
  public TariffStatus processTariff (TariffRevoke message);
  public TariffStatus processTariff (VariableRateUpdate message);
}
