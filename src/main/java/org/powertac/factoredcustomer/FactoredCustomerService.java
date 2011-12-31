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

import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Key class that processes the configuration file and creates a collection of customers 
 * during the initialization process.  It also delegates tariff selection callbacks and 
 * timeslot activation (i.e., capacity reporting) to the collection of customers.
 * 
 * @author Prashant Reddy
 */
@Service  // allow autowiring
public class FactoredCustomerService extends TimeslotPhaseProcessor implements BrokerMessageListener, NewTariffListener
{
    private static Logger log = Logger.getLogger(FactoredCustomerService.class.getName());

    @Autowired
    private TariffMarket tariffMarketService;

    private String configResource = null;
    
    private List<CustomerProfile> customerProfiles = new ArrayList<CustomerProfile>();
    private List<FactoredCustomer> customers = new ArrayList<FactoredCustomer>();
    private CustomerFactory customerFactory = new CustomerFactory();
        
    
    public FactoredCustomerService()
    {
        super();
    }

    /**
     * This is called once at the beginning of each game by the initialization service. 
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
        for (CustomerProfile customerProfile: customerProfiles) { 
            FactoredCustomer customer = customerFactory.processProfile(customerProfile);
            if (customer != null) {
                customers.add(customer);
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

            NodeList profileNodes = doc.getElementsByTagName("customer");
            int numProfiles = profileNodes.getLength();
            log.info("Loading " + numProfiles + " factored customer profiles.");
          
            for (int i = 0; i < numProfiles; ++i) {
                Element profileElement = (Element) profileNodes.item(i);
                CustomerProfile profile = new CustomerProfile(profileElement);
                customerProfiles.add(profile);
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
        for (FactoredCustomer customer : customers) {
            customer.handleNewTariffs(tariffs);
        }
    }

    /**
     * @Override @code{TimeslotPhaseProcessor}
     */
    public void activate(Instant now, int phase)
    {
        for (FactoredCustomer customer : customers) {
            customer.handleNewTimeslot();
        }
    }

    /**
     * @Override @code{BrokerMessageListener}
     */
    public void receiveMessage(Object msg)
    {
        // Implement per-message behavior here. Note that incoming messages
        // from brokers arrive in a JMS thread, so you need to synchronize
        // access to shared data structures. See AuctionService for an example.

        // If you need to handle a number of different message types, it may
        // make sense to use a reflection-based dispatcher. Both
        // TariffMarketService and AccountingService work this way.
    }

    String getConfigResource() 
    {
        return configResource;
    }
    
    void setConfigResource(String resource) 
    {
        configResource = resource;
    }
    
    List<FactoredCustomer> getCustomers() 
    {
        return customers;
    }
    
}
