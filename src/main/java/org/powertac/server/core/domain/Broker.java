package org.powertac.server.core.domain;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;
import org.springframework.roo.addon.entity.RooEntity;
import java.util.Set;
import org.powertac.server.core.domain.CashPosition;
import java.util.HashSet;
import javax.persistence.OneToMany;
import javax.persistence.CascadeType;

@RooJavaBean
@RooToString
@RooEntity
public class Broker {

    private String username;
    private String authToken;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "broker")
    private Set<CashPosition> cashPositions = new HashSet<CashPosition>();
}
