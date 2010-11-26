package org.powertac.server.core.domain;

import java.util.Set;
import org.powertac.server.core.domain.Broker;
import java.util.HashSet;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;

public class Competition {

    String name;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "competition")
    private Set<Broker> brokers = new HashSet<Broker>();
}
