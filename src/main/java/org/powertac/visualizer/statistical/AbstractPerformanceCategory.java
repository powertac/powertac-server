package org.powertac.visualizer.statistical;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.powertac.visualizer.domain.broker.BrokerModel;


/**
 * This abstract class is used as a template to build more detailed performance category for a broker.
 * 
 * @author Jurica Babic
 *
 */
public abstract class AbstractPerformanceCategory {
	BrokerModel broker;
	double grade = 0;
	
	public AbstractPerformanceCategory(BrokerModel broker) {
		this.broker = broker;
	}
	
	public double getGrade() {
		return grade;
	}
	
	public void setGrade(double d) {
		this.grade = d;
	}
	
}
