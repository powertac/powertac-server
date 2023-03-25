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
package org.powertac.common.interfaces;

import org.powertac.common.TimeService;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.repo.WeatherReportRepo;

/**
 * Accessors to allow customer models (which are not Spring beans) 
 * to interface with sim services that are Spring beans
 * 
 * @author John Collins
 */
public interface CustomerServiceAccessor
{

  /**
   * Returns a reference to a CustomerRepo
   */
  public CustomerRepo getCustomerRepo ();

  /**
   * Returns a reference to a ServerConfiguration service
   */
  public ServerConfiguration getServerConfiguration ();

  /**
   * Returns a reference to a RandomSeedRepo
   */
  public RandomSeedRepo getRandomSeedRepo ();

  /**
   * Returns a reference to the TariffMarket
   */
  public TariffMarket getTariffMarket();

  /**
   * Returns a reference to a TariffRepo
   */
  public TariffRepo getTariffRepo ();

  /**
   * Returns a reference to a TariffSubscriptionRepo
   */
  public TariffSubscriptionRepo getTariffSubscriptionRepo ();

  /**
   * Returns a reference to a TimeslotRepo
   */
  public TimeslotRepo getTimeslotRepo ();

  /**
   * Returns a reference to the TimeService
   */
  public TimeService getTimeService ();

  /**
   * Returns a reference to a WeatherReportRepo
   */
  public WeatherReportRepo getWeatherReportRepo ();

  /**
   * Returns the XMLMessageConverter needed to serialize bootstrap state
   */
  public XMLMessageConverter getMessageConverter ();
}
