package org.powertac.server.module.databaseservice.domain;

import org.joda.time.LocalDateTime;
import org.powertac.common.enumerations.BuySellIndicator;
import org.springframework.roo.addon.entity.RooEntity;
import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.tostring.RooToString;

import java.math.BigDecimal;

@RooJavaBean
@RooToString
@RooEntity
public class Shout {

  private BigDecimal quantity;

  private BigDecimal limitPrice;

  private LocalDateTime DateCreated;

  private BuySellIndicator buySellIndicator;
}
