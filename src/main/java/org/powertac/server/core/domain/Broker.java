package org.powertac.server.core.domain;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@RooJavaBean
@RooToString
@RooEntity(finders = { "findBrokersByUsernameEquals" })
public class Broker {

    private String username;

    private String authToken;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "broker")
    private Set<CashPosition> cashPositions = new HashSet<CashPosition>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "broker")
    private Set<Customer> customers = new HashSet<Customer>();
}
