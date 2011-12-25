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

import java.util.List;

import org.powertac.common.Tariff;
import org.powertac.common.Timeslot;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Prashant Reddy
 */
abstract class UtilityManager
{
    @Autowired
    protected TariffMarket tariffMarketService;

    @Autowired
    protected TariffSubscriptionRepo tariffSubscriptionRepo;

    
    UtilityManager()
    {
        tariffMarketService = (TariffMarket) SpringApplicationContext.getBean("tariffMarketService");
        tariffSubscriptionRepo = (TariffSubscriptionRepo) SpringApplicationContext.getBean("tariffSubscriptionRepo");
    }
    
    abstract void handleNewTariffs (List<Tariff> newTariffs);
            
    abstract public void handleNewTimeslot(Timeslot timeslot);
    
}
