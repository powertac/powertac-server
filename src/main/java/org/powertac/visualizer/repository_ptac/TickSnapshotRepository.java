package org.powertac.visualizer.repository_ptac;

import org.powertac.visualizer.domain.TickSnapshot;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class TickSnapshotRepository implements RecycleRepository<TickSnapshot> {

    private List<TickSnapshot> tickList = new ArrayList<>();

    public synchronized TickSnapshot save(TickSnapshot ts) {
        tickList.add(ts);
        return ts;
    }

    public synchronized List<TickSnapshot> findAll() {
        return tickList;
    }

    @Override
    public TickSnapshot findById(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TickSnapshot findByName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void recycle() {
        tickList.clear();
    }

}
