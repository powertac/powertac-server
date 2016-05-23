package org.powertac.visualizer.repository_ptac;

import org.powertac.visualizer.domain.Customer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class CustomerRepository implements RecycleRepository<Customer> {

    private Map<String, Customer> customerNameMap = new LinkedHashMap<>();
    private Map<Long, Customer> customerIdMap = new LinkedHashMap<>();

    @Override
    public synchronized Customer save(Customer customer) {
        customerNameMap.put(customer.getName(), customer);
        customerIdMap.put(customer.getIdCustomerInfo(), customer);
        return customer;
    }

    @Override
    public synchronized Customer findByName(String brokerName) {
        return customerNameMap.get(brokerName);
    }

    @Override
    public synchronized Customer findById(long idCustomerInfo) {
        return customerIdMap.get(idCustomerInfo);
    }

    @Override
    public synchronized List<Customer> findAll() {
        return new ArrayList<>(customerNameMap.values());
    }

    @Override
    public synchronized void recycle() {
        customerNameMap.clear();
        customerIdMap.clear();
    }
}
