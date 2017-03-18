package org.powertac.visualizer.repository_ptac;

import org.powertac.visualizer.domain.Tariff;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class TariffRepository implements RecycleRepository<Tariff> {

    private Map<Long, Tariff> tariffMap = new LinkedHashMap<>();

    @Override
    public synchronized Tariff save(Tariff tariff) {
        tariffMap.put(tariff.getTariffSpecId(), tariff);
        return tariff;
    }

    @Override
    public synchronized Tariff findById(long specId) {
        return tariffMap.get(specId);
    }

    @Override
    public synchronized Tariff findByName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized List<Tariff> findAll() {
        return new ArrayList<>(tariffMap.values());
    }

    @Override
    public synchronized void recycle() {
        tariffMap.clear();
    }

    
}
