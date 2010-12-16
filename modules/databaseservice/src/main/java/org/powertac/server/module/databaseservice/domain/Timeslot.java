package org.powertac.server.module.databaseservice.domain;

import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.CascadeType;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@RooJavaBean
@RooToString
@RooEntity
public class Timeslot {

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "timeslot")
    private Set<MeterReading> meterReadings = new HashSet<MeterReading>();
}
