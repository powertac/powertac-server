package org.powertac.visualizer.services;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.joda.time.Instant;
import org.powertac.common.Broker;
import org.powertac.visualizer.domain.genco.Genco;
import org.powertac.visualizer.interfaces.Recyclable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;

/**
 * Takes care of gencos' data.
 * 
 * @author Jurica Babic
 * 
 */
@Service
public class GencoService implements TimeslotCompleteActivation, Recyclable {

	private static Logger log = LogManager.getLogger(GencoService.class);

	private HashMap<String, Genco> gencoMap;

	public GencoService() {
		recycle();
	}

	/**
	 * Creates and adds genco for the specified broker.
	 * 
	 * @param broker
	 * @return created genco
	 */
	public Genco addGenco(Broker broker) {
		Genco genco = new Genco(broker);

		gencoMap.put(genco.getBroker().getUsername(), genco);
		log.info(genco.toString() + " added.");
		return genco;
	}

	/**
	 * Returns null if genco cannot be found.
	 * 
	 * @param username
	 * @return genco
	 */
	public Genco findGencoByUsername(String username) {
		return gencoMap.get(username);
	}

	@SuppressWarnings("unchecked")
	public List<Genco> getGencoList() {
		return (List<Genco>) gencoMap.values();
	}

	public void activate(int timeslotIndex, Instant postedTime) {
    for (Genco genco: gencoMap.values()) {
      genco.update(timeslotIndex, postedTime);
    }
	}

	public void recycle() {
		gencoMap = new HashMap<String, Genco>();
	}

}
