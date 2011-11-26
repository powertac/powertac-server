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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.powertac.common.PluginConfig;
import org.powertac.common.Tariff;
import org.powertac.common.interfaces.BrokerMessageListener;
import org.powertac.common.interfaces.NewTariffListener;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.interfaces.TimeslotPhaseProcessor;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Prashant Reddy
 */
@Service  // allow autowiring
public class FactoredCustomerService extends TimeslotPhaseProcessor implements BrokerMessageListener, NewTariffListener
{
    private static Logger log = Logger.getLogger(FactoredCustomerService.class.getName());

    @Autowired
    TariffMarket tariffMarketService;

    @Autowired
    protected RandomSeedRepo randomSeedRepo;

    String configResource = null;
    
    Map<String,CustomerProfile> customerProfiles = new HashMap<String,CustomerProfile>();
    Map<String,FactoredCustomer> customers = new HashMap<String,FactoredCustomer>();
    CustomerFactory customerFactory = new CustomerFactory();
        
    public FactoredCustomerService()
    {
        super();
        randomSeedRepo = (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
    }

    /**
     * This is called once at the beginning of each game by the initialization service. 
     * @throws IOException
     */
    void init(PluginConfig config) 
    {
        customerProfiles.clear();
        customers.clear();

        super.init();

        tariffMarketService.registerNewTariffListener(this);

        configResource = config.getConfigurationValue("configResource");
        loadCustomerProfiles(configResource);
        
        customerFactory.registerDefaultCreator(DefaultFactoredCustomer.getCreator());
        //customerFactory.registerCreator(ResidentialConsumerPopulation.getCreator());
        
        log.info("Creating factored customers from configuration profiles.");
        for (CustomerProfile customerProfile: customerProfiles.values()) { 
            FactoredCustomer customer = customerFactory.processProfile(customerProfile);
            if (customer != null) {
                customers.put(customerProfile.name, customer);
                //customer.subscribeDefault();
            } else throw new Error("Could not create factored customer for profile: " + customerProfile.name);
        }
        log.info("Successfully initialized factored customers from configuration profiles.");     
    }

    protected void loadCustomerProfiles(String configResource)
    {
        log.info("Attempting to load factored customer profiles from config resource: " + configResource);
        try {
            InputStream configStream = ClassLoader.getSystemClassLoader().getResourceAsStream(configResource);
            
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(configStream);

            NodeList profileNodes = doc.getElementsByTagName("profile");
            int numProfiles = profileNodes.getLength();
            log.info("Loading " + numProfiles + " factored customer profiles.");
          
            for (int i = 0; i < numProfiles; ++i) {
                Element profileElement = (Element) profileNodes.item(i);
                String profileName = profileElement.getAttribute("name");
                CustomerProfile profile = new CustomerProfile(profileElement);
                customerProfiles.put(profileName, profile);
            }
        } catch (Exception e) {
            log.error("Error loading factored customer profiles from config resourcee: " + configResource + 
                      "; exception = " + e.toString());
            throw new Error(e);
        }
        log.info("Successfully loaded factored customer profiles.");
    }

    /**
     * @Override @code{NewTariffListener}
     **/
    public void publishNewTariffs(List<Tariff> tariffs)
    {
        for (FactoredCustomer customer : customers.values()) {
            customer.handleNewTariffs(tariffs);
        }
    }

    /**
     * @Override @code{TimeslotPhaseProcessor}
     */
    public void activate(Instant now, int phase)
    {
        for (FactoredCustomer customer : customers.values()) {
            customer.handleNewTimeslot();
        }
    }

    /**
     * @Override @code{BrokerMessageListener}
     */
    public void receiveMessage(Object msg)
    {
        // TODO Implement per-message behavior. Note that incoming messages
        // from brokers arrive in a JMS thread, so you need to synchronize
        // access to shared data structures. See AuctionService for an example.

        // If you need to handle a number of different message types, it may
        // make sense to use a reflection-based dispatcher. Both
        // TariffMarketService and AccountingService work this way.
    }

    public String getConfigResource() {
        return configResource;
    }
    
    public void setConfigResource(String resource) {
        configResource = resource;
    }
    
}
