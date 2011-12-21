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

package org.powertac.visualizer.services;

import static org.powertac.util.MessageDispatcher.dispatch;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.aspectj.bridge.MessageHandler;
import org.mockito.cglib.proxy.Dispatcher;
import org.powertac.common.Orderbook;
import org.powertac.common.OrderbookOrder;
import org.powertac.common.interfaces.VisualizerMessageListener;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.common.msg.SimStart;
import org.powertac.common.repo.BrokerRepo;
import org.powertac.du.DefaultBrokerService;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.services.handler.VisualizerMessageHandlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sun.xml.bind.v2.TODO;

/**
 * Main Visualizer service. Its main purpose is to register with Visualizer
 * proxy and to receive messages from simulator.
 * 
 * @author Jurica Babic
 * 
 */

@Service
public class VisualizerService implements VisualizerMessageListener {
    static private Logger log = Logger.getLogger(VisualizerService.class
	    .getName());

    @Autowired
    private VisualizerBean visualizerBean;

    @Autowired
    private VisualizerLogService visualizerLogService;

    @Autowired
    private VisualizerMessageHandlerService visualizerMessageHandler;

    public VisualizerService() {
	super();

    }

    /**
     * Should be called before simulator run in order to prepare/reset
     * Visualizer beans and register with the new simulator instance.
     */
    public void init(VisualizerProxy visualizerProxy) {

	visualizerBean.newRun();

	// Register Visualizer with VisualizerProxy service
	visualizerProxy.registerVisualizerMessageListener(this);

	visualizerLogService.startLog(visualizerBean.getVisualizerRunCount());
	
    }

    public void receiveMessage(Object msg) {

	visualizerBean.incrementMessageCounter();

	log.info("Counter: " + visualizerBean.getMessageCount()
		+ ", Got message: " + msg.getClass().getName());

	if (msg != null) {
	    dispatch(visualizerMessageHandler, "handleMessage", msg);
	}

    }

}
