package org.powertac.visualizer.repository_ptac;

import org.powertac.visualizer.domain.Broker;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class BrokerRepository implements RecycleRepository<Broker> {

    private Map<String, Broker> brokerMapName = new LinkedHashMap<>();
    private Map<Long, Broker> brokerMapId = new LinkedHashMap<>();

    @Override
    public synchronized Broker save(Broker broker) {
        brokerMapName.put(broker.getName(), broker);
        brokerMapId.put(broker.getId(), broker);
        return broker;
    }

    @Override
    public synchronized Broker findByName(String brokerName) {
        return brokerMapName.get(brokerName);
    }

    @Override
    public synchronized Broker findById(long brokerId) {
        return brokerMapId.get(brokerId);
    }

    @Override
    public synchronized List<Broker> findAll() {
        return new ArrayList<>(brokerMapName.values());
    }

    @Override
    public synchronized void recycle() {
        brokerMapName.clear();
        brokerMapId.clear();
    }
}
