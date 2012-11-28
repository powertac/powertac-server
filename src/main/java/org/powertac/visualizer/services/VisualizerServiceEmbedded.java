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

import org.apache.log4j.Logger;
import org.powertac.common.interfaces.VisualizerMessageListener;
import org.powertac.common.interfaces.VisualizerProxy;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.VisualizerApplicationContext;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.interfaces.Initializable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Main Visualizer service. Its main purpose is to register with Visualizer
 * proxy and to receive messages from simulator.
 * 
 * @author Jurica Babic
 * 
 */

@Service
public class VisualizerServiceEmbedded implements VisualizerMessageListener {
	static private Logger log =
      Logger.getLogger(VisualizerServiceEmbedded.class.getName());

	@Autowired
	private VisualizerBean visualizerBean;

	private boolean alreadyRegistered = false;

	@Autowired
	private MessageDispatcher dispatcher;

	/**
	 * Should be called before simulator run in order to prepare/reset
	 * Visualizer beans and register with the new simulator instance.
	 */
	public void init(VisualizerProxy visualizerProxy) {
		visualizerBean.newRun();

		// Register Visualizer with VisualizerProxy service
		if (!alreadyRegistered) {
			visualizerProxy.registerVisualizerMessageListener(this);
			alreadyRegistered = true;
		} 
		
		// registrations for message listeners:
		List<Initializable> initializers = VisualizerApplicationContext
        .listBeansOfType(Initializable.class);
		for (Initializable init: initializers) {
			log.debug("initializing..." + init.getClass().getName());
			init.initialize();
		}
	}

	public void receiveMessage(Object msg) {

		visualizerBean.incrementMessageCounter();

		if (msg != null) {
			log.debug("Counter: " + visualizerBean.getMessageCount() +
                ", Got message: " + msg.getClass().getName());
			dispatcher.routeMessage(msg);
		} else {
			log.warn("Counter:" + visualizerBean.getMessageCount() +
               " Received message is NULL!");
		}
	}
}
