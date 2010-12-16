package org.powertac.server.module.databaseservice.domain;

import org.powertac.common.enumerations.ProductType;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@RooJavaBean
@RooToString
@RooEntity
public class Product {

  @ManyToOne
  @JoinColumn
  Competition competition;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "product")
  private Set<Orderbook> orderbooks = new HashSet<Orderbook>();

  @Enumerated
  private ProductType productType;


}
