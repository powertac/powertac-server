package org.powertac.server.module.databaseservice.domain;

import org.joda.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@RooJavaBean
@RooToString
@RooEntity
public class Timeslot {

  @ManyToOne
  private Competition competition;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "timeslot")
  private Set<MeterReading> meterReadings = new HashSet<MeterReading>();

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "timeslot")
  private Set<Orderbook> orderbooks = new HashSet<Orderbook>();

  @Temporal(TemporalType.TIMESTAMP)
  @DateTimeFormat(style = "S-")
  private LocalDateTime startDateTime;

  @Temporal(TemporalType.TIMESTAMP)
  @DateTimeFormat(style = "S-")
  private LocalDateTime endDateTime;


}
